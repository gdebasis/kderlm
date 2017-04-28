/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package feedback;

import java.util.HashMap;
import wvec.WordVec;

/**
 *
 * @author Debasis
 */
public class PerDocTermVector {
    int docId;
    int sum_tf;
    float sim;  // similarity with query
    HashMap<String, RetrievedDocTermInfo> perDocStats;
    
    public PerDocTermVector(int docId) {
        this.docId = docId;
        perDocStats = new HashMap<>();
        sum_tf = 0;
    }
    
    public float getNormalizedTf(String term) {
        RetrievedDocTermInfo tInfo = perDocStats.get(term);
        if (tInfo == null)
            return 0;
        return perDocStats.get(term).tf/(float)sum_tf;
    }
    
    RetrievedDocTermInfo getTermStats(WordVec wv) {
        RetrievedDocTermInfo tInfo;
        String qTerm = wv.getWord();
        if (qTerm == null)
            return null;
        
        // Check if this word is a composed vector
        if (!wv.isComposed()) {
            tInfo = this.perDocStats.get(qTerm);
            return tInfo;
        }
            
        // Split up the composed into it's constituents
        String[] qTerms = qTerm.split(WordVec.COMPOSING_DELIM);
        RetrievedDocTermInfo firstTerm = this.perDocStats.get(qTerms[0]);
        if (firstTerm == null)
            return null;
        RetrievedDocTermInfo secondTerm = this.perDocStats.get(qTerms[1]);
        if (secondTerm == null)
            return null;
        tInfo = new RetrievedDocTermInfo(wv);
        tInfo.tf = firstTerm.tf * secondTerm.tf;
        
        return tInfo;
    }    
}

