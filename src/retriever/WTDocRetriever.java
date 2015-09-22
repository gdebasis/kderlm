/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package retriever;

import java.util.List;
import trec.TRECQuery;
import trec.TRECQueryParser;
import wt10g.WTQueryParser;

/**
 *
 * @author Debasis
 */
public class WTDocRetriever extends TrecDocRetriever {

    public WTDocRetriever(String propFile) throws Exception {
        super(propFile);
    }
    
    @Override
    public List<TRECQuery> constructQueries() throws Exception {        
        String queryFile = prop.getProperty("query.file");
        //TRECQueryParser parser = new WTQueryParser(queryFile, indexer.getAnalyzer());
        TRECQueryParser parser = new TRECQueryParser(queryFile, indexer.getAnalyzer());
        parser.parse();
        return parser.getQueries();
    }
    
    public static void main(String[] args) {
        if (args.length < 1) {
            args = new String[1];
            args[0] = "wt10g.properties";
        }
        try {
            TrecDocRetriever searcher = new WTDocRetriever(args[0]);
            searcher.retrieveAll();            
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
