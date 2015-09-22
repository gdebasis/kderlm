/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package feedback;

import java.util.Map;
import org.apache.lucene.search.TopDocs;
import retriever.TrecDocRetriever;
import trec.TRECQuery;

/**
 *
 * @author Debasis
 */
public class RelevanceModelConditional extends RelevanceModelIId {

    public RelevanceModelConditional(TrecDocRetriever retriever, TRECQuery trecQuery, TopDocs topDocs) throws Exception {
        super(retriever, trecQuery, topDocs);
    }
    
    @Override
    public void computeKDE() throws Exception {
        float p_w;
        float this_wt; // phi(q,w)
        
        buildTermStats();
        prepareQueryVector();
        
        int docsSeen = 0;

        // For each doc in top ranked
        for (PerDocTermVector docvec : this.retrievedDocsTermStats.docTermVecs) {            
            // For each word in this document
            for (Map.Entry<String, RetrievedDocTermInfo> e : docvec.perDocStats.entrySet()) {
                RetrievedDocTermInfo w = e.getValue();
                p_w = mixTfIdf(w, docvec);
                this_wt = p_w * docvec.sim;
                
                // Take the average
                RetrievedDocTermInfo wGlobal = retrievedDocsTermStats.getTermStats(w.wvec);
                wGlobal.wt += this_wt;      
            }
            docsSeen++;
            if (docsSeen >= numTopDocs)
                break;
        }  
    }
    
    
}
