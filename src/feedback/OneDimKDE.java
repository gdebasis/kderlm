/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package feedback;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
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

public class OneDimKDE extends RelevanceModelIId {
    HashMap<String, Kernel> kernels;
    Kernel kernelF;
    boolean toExpand;
    boolean autoParams;
    NNQueryExpander nnQexpander;
    
    public OneDimKDE(TrecDocRetriever retriever, TRECQuery trecQuery, TopDocs topDocs) throws Exception {
        super(retriever, trecQuery, topDocs);
        
        toExpand = Boolean.parseBoolean(prop.getProperty("kde.queryexpansion", "false"));        
        if (toExpand)
            nnQexpander = new NNQueryExpander(wvecs,
                    Integer.parseInt(prop.getProperty("queryexpansion.nterms")));            
        
        kernels = new HashMap<>();
        kernels.put("gaussian",
                new GaussianKernel(
                Float.parseFloat(prop.getProperty("gaussian.sigma")),
                Float.parseFloat(prop.getProperty("kde.h"))
                ));
        kernels.put("triangular", new TriangularKernel(
                Float.parseFloat(prop.getProperty("kde.h"))                
                ));
        String kernelFuncName = prop.getProperty("kde.kernel");
        if (kernelFuncName.equals("gaussian")) {
            if (Boolean.parseBoolean(prop.getProperty("kde.gaussian.autoparams", "false")))
                autoParams = true;
        }
        kernelF = kernels.get(kernelFuncName);        
    }
    
    void setAutoParams() {
        float sigma = 0f;
        float avgNorm = 0f;
        int n = qwvecs.getVecs().size();
        for (WordVec qvec : qwvecs.getVecs()) {
            if (qvec != null)
                avgNorm += qvec.getNorm();
        }
        for (WordVec qvec : qwvecs.getVecs()) {
            if (qvec != null)
                sigma += Math.pow(qvec.getNorm() - avgNorm, 2);
        }
        ((GaussianKernel)kernelF).sigma = (float)Math.sqrt(sigma);
        ((GaussianKernel)kernelF).h =
                1.06f * ((GaussianKernel)kernelF).sigma *
                (float)Math.pow(n, -0.2f);
    }
        
    float computeKernelFunction(WordVec a, WordVec b) {
        float dist = a.euclideanDist(b);
        return kernelF.fKernel(dist);
    }
    
    @Override
    public void prepareQueryVector() {        
        if (toExpand) {
            nnQexpander.expandQuery(trecQuery);
        }
        
        if (Boolean.parseBoolean(prop.getProperty("kde.compose")))
            composer.formComposedQuery();
        qwvecs = composer.getQueryWordVecs();
        
        if (autoParams)
            setAutoParams();
    }
    
    /* In one dimensional KDE, we don't care about the individual
     * documents, but rather only take into consideration the whole
     * set of pseudo-relevant documents as a whole.
     */
    @Override
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
            p_w = mixTfIdf(w);
            
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
                
                this_wt = p_q * p_w * computeKernelFunction(qwvec, w.wvec);
                f_w += this_wt;
            }
            // Take the average
            w.wt = f_w /(float)qwvecs.getVecs().size();
        }
    }
    
}

