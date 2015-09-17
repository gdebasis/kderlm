/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wt10g;

import indexing.TrecDocIndexer;
import java.util.HashSet;
import java.util.Set;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.xml.sax.SAXException;
import trec.TRECQuery;
import trec.TRECQueryParser;

/**
 *
 * @author Debasis
 */
public class WTQueryParser extends TRECQueryParser {

    public WTQueryParser(String fileName, Analyzer analyzer) throws SAXException {
        super(fileName, analyzer);
    }
    
    @Override
    public Query constructLuceneQueryObj(TRECQuery trecQuery) throws QueryNodeException {
        BooleanQuery fullQuery = new BooleanQuery();
        Query titleQuery = getQueryParser().parse(trecQuery.title, WTDocument.WTDOC_FIELD_TITLE);
        Query bodyQuery = getQueryParser().parse(trecQuery.title, TrecDocIndexer.FIELD_ANALYZED_CONTENT);
        
        Set<Term> titleTerms = new HashSet<Term>();
        titleQuery.extractTerms(titleTerms);
        for (Term t: titleTerms) {
            TermQuery tq = new TermQuery(t);
            tq.setBoost(1);
            fullQuery.add(tq, BooleanClause.Occur.SHOULD);
        }
            
        Set<Term> bodyTerms = new HashSet<Term>();
        bodyQuery.extractTerms(bodyTerms);
        for (Term t: bodyTerms) {
            TermQuery tq = new TermQuery(t);
            tq.setBoost(10);
            fullQuery.add(tq, BooleanClause.Occur.SHOULD);
        }
        
        return fullQuery;
    }    
}
