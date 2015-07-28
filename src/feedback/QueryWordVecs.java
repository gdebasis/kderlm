/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package feedback;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import trec.TRECQuery;
import wvec.WordVec;
import wvec.WordVecs;

/**
 *
 * @author Debasis
 */
public class QueryWordVecs {
    TRECQuery query;
    List<WordVec> wvecs;
    WordVecs allwvecs;
    
    public QueryWordVecs(TRECQuery query, WordVecs allwVecs) {
        this.query = query;
        this.allwvecs = allwVecs;
        wvecs = new ArrayList<>();
    }
    
    void addQueryWordVec(String word) {
        WordVec wv = allwvecs.getVec(word);
        wvecs.add(wv);
    }
    
    void addQueryWordVec(WordVec wv) {
        wvecs.add(wv);
    }
    
    List<WordVec> getVecs() { return wvecs; }    
}
