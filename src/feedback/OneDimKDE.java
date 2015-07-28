/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package feedback;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import retriever.NNQueryExpander;
import retriever.TrecDocRetriever;
import trec.TRECQuery;
import wvec.WordVec;
import wvec.WordVecs;

/**
 *
 * @author Debasis
 * The feedback class for computing one dimensional KDE estimate
 */

class KLDivScoreComparator implements Comparator<ScoreDoc> {

    @Override
    public int compare(ScoreDoc a, ScoreDoc b) {
        return a.score < b.score? -1 : a.score == b.score? 0 : 1;
    }    
}

public class OneDimKDE {
    TrecDocRetriever retriever;
    TRECQuery trecQuery;
    TopDocs topDocs;
    QueryVecComposer composer;
    RetrievedDocsTermStats retrievedDocsTermStats;
    QueryWordVecs qwvecs;
    HashMap<String, Kernel> kernels;
    Properties prop;
    Kernel kernelF;
    int numTopDocs;
    float mixingLambda;
    NNQueryExpander qc;
      
    static WordVecs wvecs;  // to be shared across every feedback
                            // object (one for each query)
            
    public OneDimKDE(TrecDocRetriever retriever, TRECQuery trecQuery, TopDocs topDocs) throws Exception {
        this.prop = retriever.getProperties();
        
        // singleton instance shared across all feedback objects
        if (wvecs == null)
            wvecs = new WordVecs(prop);
        
        this.retriever = retriever;
        this.trecQuery = trecQuery;
        this.topDocs = topDocs;
        composer = new QueryVecComposer(trecQuery, wvecs);
        kernels = new HashMap<>();
        kernels.put("gaussian",
                new GaussianKernel(
                Float.parseFloat(prop.getProperty("gaussian.sigma")),
                Float.parseFloat(prop.getProperty("kde.h"))
                ));
        kernels.put("triangular", new TriangularKernel(
                Float.parseFloat(prop.getProperty("kde.h"))                
                ));
        kernelF = kernels.get(prop.getProperty("kde.kernel"));        
        numTopDocs = Integer.parseInt(prop.getProperty("kde.numtopdocs"));
        mixingLambda = Float.parseFloat(prop.getProperty("kde.lambda"));
        
        qc = new NNQueryExpander(wvecs);  
    }
        
    float computeKernelFunction(WordVec a, WordVec b) {
        float dist = a.euclideanDist(b);
        return kernelF.fKernel(dist);
    }
    
    public void prepareQueryVector() {
        qc.expandQuery(trecQuery); // modifies the parameter trecQuery
        
        if (Boolean.parseBoolean(prop.getProperty("kde.compose")))
            composer.formComposedQuery();
        qwvecs = composer.getQueryWordVecs();
    }
    
    public void buildTermStats() throws Exception {
        retrievedDocsTermStats = new
                RetrievedDocsTermStats(retriever.getReader(),
                wvecs, topDocs, numTopDocs);
        retrievedDocsTermStats.buildAllStats();
    }
    
    /* In one dimensional KDE, we don't care about the individual
     * documents, but rather only take into consideration the whole
     * set of pseudo-relevant documents as a whole.
     */
    public void computeKDE() throws Exception {
        
        float f_w; // KDE estimation for term w
        float p_q; // KDE weight, P(q)
        float p_w;
        float this_wt; // phi(q,w)
        
        buildTermStats();
        prepareQueryVector();

        /* For each w \in V (vocab of top docs),
         * compute f(w) = \sum_{q \in qwvecs} K(w,q) */
        for (Map.Entry<String, RetrievedDocTermInfo> e : retrievedDocsTermStats.termStats.entrySet()) {
            RetrievedDocTermInfo w = e.getValue();
            f_w = 0;
            for (WordVec qwvec : qwvecs.getVecs()) {
                if (qwvec == null)
                    continue; // a very rare case where a query term is OOV
                
                // Get query term frequency
                RetrievedDocTermInfo qtermInfo = retrievedDocsTermStats.getTermStats(qwvec);
                if (qtermInfo == null) {
                    System.err.println("No KDE for query term: " + qwvec.getWord());
                    continue;
                }
                if (qtermInfo.wvec.isComposed()) {
                    // Joint probability of two term compositions
                    p_q = qtermInfo.tf/(float)(retrievedDocsTermStats.sumTf*retrievedDocsTermStats.sumTf);
                }
                else
                    p_q = qtermInfo.tf/(float)retrievedDocsTermStats.sumTf;
                
                p_w = mixTfIdf(w);
                this_wt = p_q * p_w * computeKernelFunction(qwvec, w.wvec);
                f_w += this_wt;
            }
            // Take the average
            w.wt = f_w /(float)qwvecs.getVecs().size();
        }
    }
    
    float mixTfIdf(RetrievedDocTermInfo w) {
        return mixingLambda*w.tf/(float)retrievedDocsTermStats.sumTf +
                (1-mixingLambda)*w.df/retrievedDocsTermStats.sumDf;        
    }
    
    public TopDocs rerankDocs() {
        TopDocs rerankedDocs = null;
        ScoreDoc[] klDivScoreDocs = new ScoreDoc[this.topDocs.scoreDocs.length];
        float klDiv;
        float p_w_D;    // P(w|D) for this doc D
        final float EPSILON = 0.0001f;
        
        // For each document
        for (int i = 0; i < topDocs.scoreDocs.length; i++) {
            klDiv = 0;
            klDivScoreDocs[i] = new ScoreDoc(topDocs.scoreDocs[i].doc, klDiv);
            PerDocTermVector docVector = this.retrievedDocsTermStats.docTermVecs.get(i);
            
            // For each v \in V (vocab of top ranked documents)
            for (Map.Entry<String, RetrievedDocTermInfo> e : retrievedDocsTermStats.termStats.entrySet()) {
                RetrievedDocTermInfo w = e.getValue();
                
                float ntf = docVector.getNormalizedTf(w.wvec.getWord());
                if (ntf == 0)
                    ntf = EPSILON;
                p_w_D = ntf;
                klDiv += w.wt * Math.log(w.wt/p_w_D);
            }
            klDivScoreDocs[i].score = klDiv;
        }
        
        // Sort the scoredocs in ascending order of the KL-Div scores
        Arrays.sort(klDivScoreDocs, new KLDivScoreComparator());
        
        rerankedDocs = new TopDocs(topDocs.totalHits, klDivScoreDocs, klDivScoreDocs[klDivScoreDocs.length-1].score);
        return rerankedDocs;
    }    
}

