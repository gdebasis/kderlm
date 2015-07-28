/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package retriever;

import indexing.TrecDocIndexer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import trec.TRECQuery;
import wvec.WordVec;
import wvec.WordVecs;

/**
 *
 * @author Debasis
 * Use vector compositions to form an expanded query. Most likely
 * will be useful for more effective density estimation.
 */
public class NNQueryExpander {

    WordVecs wvecs;
    
    public NNQueryExpander(TrecDocIndexer indexer) {
        try {
            wvecs = new WordVecs(indexer.getProperties());
        }
        catch (Exception ex) { ex.printStackTrace(); }
    }
    
    public NNQueryExpander(Properties prop) {
        try {
            System.out.println("Loading wordvecs in memory...");
            wvecs = new WordVecs(prop);
            System.out.println("Loaded wordvecs in memory...");
        }
        catch (Exception ex) { ex.printStackTrace(); }        
    }
    
    public NNQueryExpander(WordVecs wvecs) {
        this.wvecs = wvecs;
    }
    
    public void expandQuery(TRECQuery query) {
        // Collect the origTerms
        Query luceneQry = query.getLuceneQueryObj();
        System.out.println("Composing query: " + luceneQry.toString());
        HashSet<Term> origTerms = new HashSet<>();
        luceneQry.extractTerms(origTerms);
        
        // For checking that we are adding new expansion terms
        HashSet<String> origTermStrings = new HashSet<>();
        for (Term t : origTerms) {
            origTermStrings.add(t.text());
        }
        
        Term[] termArray = new Term[origTerms.size()];
        termArray = origTerms.toArray(termArray);
        
        // Initialize a hashmap to store the nn origTerms
        HashMap<String, WordVec> nnMap = new HashMap<>();
        
        // Iterate over the original origTerms to pairwise compose them
        for (int i = 0; i < termArray.length-1; i++) {
            String thisTerm = termArray[i].text();
            String nextTerm = termArray[i+1].text();
            WordVec thisTermVec = wvecs.getVec(thisTerm);
            WordVec nextTermVec = wvecs.getVec(nextTerm);
            if (thisTermVec == null || nextTermVec == null) {
                continue;
            }
            System.out.println("Composing words: " + thisTermVec.getWord() + " " + nextTermVec.getWord());
            WordVec composedVec = WordVec.centroid(thisTermVec, nextTermVec);
            List<WordVec> nnvecs = wvecs.getNearestNeighbors(composedVec);
            
            int nnIndex = 0;
            for (WordVec nnvec : nnvecs) {
                System.out.println("NN (" + (nnIndex++) + "): " + nnvec.getWord() + " (" + nnvec.getQuerySim() + ")");
                if (!origTermStrings.contains(nnvec.getWord()))
                    nnMap.put(nnvec.getWord(), nnvec);
            }            
        }
        
        // Now centroid the origTerms of this hashmap to the original query
        for (String nnword : nnMap.keySet()) {
            TermQuery tq = new TermQuery(
                   new Term(TrecDocIndexer.FIELD_ANALYZED_CONTENT, nnword));
            ((BooleanQuery)luceneQry).add(tq, BooleanClause.Occur.SHOULD);
        }
    }
    
    public void expandQueriesWithNN(List<TRECQuery> queries) {
        for (TRECQuery q : queries) {
            expandQuery(q);
        }
    }
}
