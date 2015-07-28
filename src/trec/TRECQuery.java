/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package trec;

import indexing.TrecDocIndexer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

/**
 *
 * @author Debasis
 */
public class TRECQuery {
    public String       id;
    public String       title;
    public String       desc;
    public String       narr;
    public Query        luceneQuery;
    StandardQueryParser queryParser;
    
    public TRECQuery(Analyzer analyzer) {
        queryParser = new StandardQueryParser(analyzer);
    }
    
    @Override
    public String toString() {
        return luceneQuery.toString();
    }

    public Query getLuceneQueryObj() { return luceneQuery; }
    
    public Query constructLuceneQueryObj() throws QueryNodeException {        
        luceneQuery = queryParser.parse(title, TrecDocIndexer.FIELD_ANALYZED_CONTENT);
        return luceneQuery;
    }
}
