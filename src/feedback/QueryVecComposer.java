/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package feedback;

import java.util.HashSet;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import trec.TRECQuery;
import wvec.WordVec;
import wvec.WordVecs;

/**
 *
 * @author Debasis
 * Expands a query representation by adding the word vectors pairwise
 */
public class QueryVecComposer {
    TRECQuery trecQuery;
    WordVecs wvecs;
    String[] termArray;
    QueryWordVecs qvecs;
    HashSet<Term> origTerms;
    
    public QueryVecComposer(TRECQuery trecQuery, WordVecs wvecs) {
        this.trecQuery = trecQuery;
        this.wvecs = wvecs;
        qvecs = new QueryWordVecs(trecQuery, wvecs);
        
        origTerms = new HashSet<>();
        Query luceneQry = trecQuery.getLuceneQueryObj();
        luceneQry.extractTerms(origTerms);
        termArray = new String[origTerms.size()];
        
        int tcount = 0;
        for (Term t: origTerms) {
            termArray[tcount] = t.text();
            qvecs.addQueryWordVec(termArray[tcount]);
            tcount++;
        }
    }

    void formComposedQuery() {        
        // Iterate over the original origTerms to pairwise compose them
        for (int i = 0; i < termArray.length-1; i++) {
            String thisTerm = termArray[i];
            String nextTerm = termArray[i+1];
            WordVec thisTermVec = wvecs.getVec(thisTerm);
            WordVec nextTermVec = wvecs.getVec(nextTerm);
            if (thisTermVec == null || nextTermVec == null) {
                continue;
            }
            System.out.println("Composing words: " + thisTermVec.getWord() + " " + nextTermVec.getWord());
            WordVec composedVec = WordVec.centroid(thisTermVec, nextTermVec);
            qvecs.addQueryWordVec(composedVec);
        }
    }
    
    QueryWordVecs getQueryWordVecs() { return qvecs; }
}