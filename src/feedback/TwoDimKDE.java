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
import wvec.WordVec;

/**
 *
 * @author Debasis
 */
public class TwoDimKDE extends OneDimKDE {

    public TwoDimKDE(TrecDocRetriever retriever, TRECQuery trecQuery, TopDocs topDocs) throws Exception {
        super(retriever, trecQuery, topDocs);
    }

    @Override
    public void computeKDE() throws Exception {
        float f_w; // KDE estimation for term w
        float p_q; // KDE weight, P(q)
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
                f_w = 0;
                p_w = mixTfIdf(w, docvec);

                for (WordVec qwvec : qwvecs.getVecs()) {
                    if (qwvec == null)
                        continue; // a very rare case where a query term is OOV

                    // Get query term frequency
                    RetrievedDocTermInfo qtermInfo = docvec.getTermStats(qwvec);
                    if (qtermInfo == null) {
                        continue;
                    }
                    if (qtermInfo.wvec.isComposed()) {
                        // Joint probability of two term compositions
                        p_q = qtermInfo.tf/(float)(docvec.sum_tf * docvec.sum_tf);
                    }
                    else
                        p_q = qtermInfo.tf/(float)docvec.sum_tf;

                    this_wt = p_w *
                                docvec.sim/retrievedDocsTermStats.sumSim *
                                computeKernelFunction(qwvec, w.wvec);
                    f_w += this_wt;
                }
                
                // Take the average
                RetrievedDocTermInfo wGlobal = retrievedDocsTermStats.getTermStats(w.wvec);
                wGlobal.wt += f_w /(float)qwvecs.getVecs().size();            
            }
            docsSeen++;
            if (docsSeen >= numTopDocs)
                break;
        }  
    }
}
