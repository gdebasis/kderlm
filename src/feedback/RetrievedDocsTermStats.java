/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package feedback;

import indexing.TrecDocIndexer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queries.function.docvalues.DocTermsIndexDocValues;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.BytesRef;
import wvec.WordVec;
import wvec.WordVecs;

/**
 *
 * @author Debasis
 */
class RetrievedDocTermInfo {
    WordVec wvec;
    int tf;
    long df;
    float wt;   // weight of this term, e.g. the P(w|R) value    

    public RetrievedDocTermInfo(WordVec wvec) {
        this.wvec = wvec;
    }
    
    public RetrievedDocTermInfo(WordVec wvec, int tf) {
        this.wvec = wvec;
        this.tf = tf;
    }
}

class PerDocTermVector {
    int docId;
    int sum_tf;
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
}

public class RetrievedDocsTermStats {
    TopDocs topDocs;
    IndexReader reader;
    WordVecs wvecs;
    int sumTf;
    float sumDf;
    Map<String, RetrievedDocTermInfo> termStats;
    List<PerDocTermVector> docTermVecs;
    int numTopDocs;
    
    public RetrievedDocsTermStats(IndexReader reader, WordVecs wvecs,
            TopDocs topDocs, int numTopDocs) {
        this.topDocs = topDocs;
        this.reader = reader;
        this.wvecs = wvecs;
        sumTf = 0;
        sumDf = numTopDocs;
        termStats = new HashMap<>();
        docTermVecs = new ArrayList<>();
        this.numTopDocs = numTopDocs;
    }
    
    public void buildAllStats() throws Exception {
        int rank = 0;
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            int docId = scoreDoc.doc;
            docTermVecs.add(buildStatsForSingleDoc(docId, rank));
            rank++;
        }
    }
    
    RetrievedDocTermInfo getTermStats(WordVec wv) {
        RetrievedDocTermInfo tInfo;
        String qTerm = wv.getWord();
        if (qTerm == null)
            return null;
        
        // Check if this word is a composed vector
        if (!wv.isComposed()) {
            tInfo = this.termStats.get(qTerm);
            return tInfo;
        }
            
        // Split up the composed into it's constituents
        String[] qTerms = qTerm.split(WordVec.COMPOSING_DELIM);
        RetrievedDocTermInfo firstTerm = this.termStats.get(qTerms[0]);
        if (firstTerm == null)
            return null;
        RetrievedDocTermInfo secondTerm = this.termStats.get(qTerms[1]);
        if (secondTerm == null)
            return null;
        tInfo = new RetrievedDocTermInfo(wv);
        tInfo.tf = firstTerm.tf * secondTerm.tf;
        
        return tInfo;
    }
    
    PerDocTermVector buildStatsForSingleDoc(int docId, int rank) throws Exception {
        String termText;
        BytesRef term;
        Terms tfvector;
        TermsEnum termsEnum;
        WordVec termVec;
        int tf;
        RetrievedDocTermInfo trmInfo;
        PerDocTermVector docTermVector = new PerDocTermVector(docId);
        
        tfvector = reader.getTermVector(docId, TrecDocIndexer.FIELD_ANALYZED_CONTENT);
        if (tfvector == null || tfvector.size() == 0)
            return null;
        
        // Construct the normalized tf vector
        termsEnum = tfvector.iterator(null); // access the terms for this field
        
    	while ((term = termsEnum.next()) != null) { // explore the terms for this field
            termText = term.utf8ToString();
            tf = (int)termsEnum.totalTermFreq();
            termVec = wvecs.getVec(termText);
            if (termVec == null)
                continue;
            
            // per-doc
            docTermVector.perDocStats.put(termText, new RetrievedDocTermInfo(termVec, tf));
            docTermVector.sum_tf += tf;
            
            if (rank >= numTopDocs) {
                continue;
            }
            // all collection            
            trmInfo = termStats.get(termText);
            if (trmInfo == null) {
                trmInfo = new RetrievedDocTermInfo(termVec);
                termStats.put(termText, trmInfo);
            }
            trmInfo.tf += tf;
            trmInfo.df++;
            sumTf += tf;
        }
        return docTermVector;
    }
}
