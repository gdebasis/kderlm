/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package feedback;

import indexing.TrecDocIndexer;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import retriever.TrecDocRetriever;
import trec.TRECQuery;
import wvec.WordVec;
import wvec.WordVecs;

/**
 *
 * @author Debasis
 */
public class RelevanceModelIId {
    TrecDocRetriever retriever;
    TRECQuery trecQuery;
    TopDocs topDocs;
    Properties prop;
    float mixingLambda;
    int numTopDocs;
    RetrievedDocsTermStats retrievedDocsTermStats;
    QueryWordVecs qwvecs;
    QueryVecComposer composer;
    WordVecs wvecs;  // to be shared across every feedback

    public RelevanceModelIId(TrecDocRetriever retriever, TRECQuery trecQuery, TopDocs topDocs) throws Exception {
        this.prop = retriever.getProperties();
        this.retriever = retriever;
        this.trecQuery = trecQuery;
        this.topDocs = topDocs;
        numTopDocs = Integer.parseInt(prop.getProperty("kde.numtopdocs"));
        mixingLambda = Float.parseFloat(prop.getProperty("kde.lambda"));  
            
        // singleton instance shared across all feedback objects
        if (wvecs == null)
            wvecs = new WordVecs(prop);
        
        composer = new QueryVecComposer(trecQuery, wvecs, prop);        
    }
    
    public void buildTermStats() throws Exception {
        retrievedDocsTermStats = new
                RetrievedDocsTermStats(retriever.getReader(),
                wvecs, topDocs, numTopDocs);
        retrievedDocsTermStats.buildAllStats();
    }
    
    float mixTfIdf(RetrievedDocTermInfo w) {
        return mixingLambda*w.tf/(float)retrievedDocsTermStats.sumTf +
                (1-mixingLambda)*w.df/retrievedDocsTermStats.sumDf;        
    }    
    
    float mixTfIdf(RetrievedDocTermInfo w, PerDocTermVector docvec) {
        RetrievedDocTermInfo wGlobalInfo = retrievedDocsTermStats.termStats.get(w.wvec.getWord());
        return mixingLambda*w.tf/(float)docvec.sum_tf +
                (1-mixingLambda)*wGlobalInfo.df/retrievedDocsTermStats.sumDf;        
    }
            
    public void prepareQueryVector() {        
        qwvecs = composer.getQueryWordVecs();
    }

    public void computeKDE() throws Exception {
        float p_q;
        float p_w;
        float total_p_q = 0;
        
        buildTermStats();
        prepareQueryVector();
        
        /* For each w \in V (vocab of top docs),
         * compute f(w) = \sum_{q \in qwvecs} K(w,q) */
        for (Map.Entry<String, RetrievedDocTermInfo> e : retrievedDocsTermStats.termStats.entrySet()) {
            RetrievedDocTermInfo w = e.getValue();
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
                p_q = qtermInfo.tf/(float)retrievedDocsTermStats.sumTf;
                
                total_p_q += Math.log(1+p_q);
            }
            
            w.wt = p_w * total_p_q;
        }
    }
    
    public TopDocs rerankDocs() {
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
        
        TopDocs rerankedDocs = new TopDocs(topDocs.totalHits, klDivScoreDocs, klDivScoreDocs[klDivScoreDocs.length-1].score);
        return rerankedDocs;
    }
    
    public float getQueryClarity() {
        float klDiv = 0;
        float p_w_C;
        // For each v \in V (vocab of top ranked documents)
        for (Map.Entry<String, RetrievedDocTermInfo> e : retrievedDocsTermStats.termStats.entrySet()) {
            RetrievedDocTermInfo w = e.getValue();
            p_w_C = w.df/retrievedDocsTermStats.sumDf;
            klDiv += w.wt * Math.log(w.wt/p_w_C);
        }
        return klDiv;
    }
    
    public float getQueryClarity(IndexReader reader) throws Exception {
        float klDiv = 0;
        float p_w_C;
        // For each v \in V (vocab of top ranked documents)
        for (Map.Entry<String, RetrievedDocTermInfo> e : retrievedDocsTermStats.termStats.entrySet()) {
            RetrievedDocTermInfo w = e.getValue();
            double sumCf = (double)reader.getSumTotalTermFreq(TrecDocIndexer.FIELD_ANALYZED_CONTENT);
            double cf = reader.totalTermFreq(new Term(TrecDocIndexer.FIELD_ANALYZED_CONTENT, w.wvec.getWord()));
            p_w_C = (float)(cf/sumCf);
            klDiv += w.wt * Math.log(w.wt/p_w_C);
        }
        return klDiv;
    }    
}
