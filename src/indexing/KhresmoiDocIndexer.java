/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package indexing;

import java.io.File;
import java.util.List;
import org.apache.lucene.document.Document;
import wt10g.KhresmoiDocParser;
import wt10g.WTDocumentParser;

/**
 *
 * @author Debasis
 */
public class KhresmoiDocIndexer extends WTDocIndexer {

    public KhresmoiDocIndexer(String propFile) throws Exception {
        super(propFile);
    }
    
    @Override
    void indexFile(File file) throws Exception {        
        WTDocumentParser parser = new KhresmoiDocParser(file, getAnalyzer());
        System.out.println("Indexing file: " + file.getName());
        parser.parse();
        List<Document> docs = parser.getDocuments();
        for (Document doc : docs) {
            writer.addDocument(doc);            
        }
    }
    
    public static void main(String[] args) {
        if (args.length == 0) {
            args = new String[1];
            System.out.println("Usage: java KhresmoiDocIndexer <prop-file>");
            args[0] = "khresmoi.properties";
        }

        try {
            KhresmoiDocIndexer indexer = new KhresmoiDocIndexer(args[0]);
            indexer.processAll();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }        
    }
    
}
