/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package indexing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.util.Version;
import wt10g.WTDocument;
import wt10g.WTDocumentParser;

/**
 *
 * @author Debasis
 */

public class WTDocIndexer extends TrecDocIndexer {
    String tarArchivePath;
    
    public WTDocIndexer(String propFile) throws Exception {
        super(propFile);
        tarArchivePath = prop.getProperty("coll");
    }

    @Override
    Analyzer constructAnalyzer() {
        Analyzer defaultAnalyzer = new WhitespaceAnalyzer(Version.LUCENE_4_9);
        Map<String, Analyzer> anmap = new HashMap<String, Analyzer>();
        Analyzer enAnalyzer = new EnglishAnalyzer(
            Version.LUCENE_4_9,
            StopFilter.makeStopSet(
                Version.LUCENE_4_9, buildStopwordList("stopfile"))); // default analyzer
        
        anmap.put(WTDocument.WTDOC_FIELD_TITLE, enAnalyzer);
        anmap.put(FIELD_ANALYZED_CONTENT, enAnalyzer);
        
        PerFieldAnalyzerWrapper pfAnalyzer = new PerFieldAnalyzerWrapper(defaultAnalyzer, anmap);
        return pfAnalyzer;
    }
    
    @Override
    void indexFile(File file) throws Exception {        
        WTDocumentParser parser = new WTDocumentParser(file, getAnalyzer());
        System.out.println("Indexing file: " + file.getName());
        parser.parse();
        List<Document> docs = parser.getDocuments();
        for (Document doc : docs) {
            writer.addDocument(doc);            
        }
    }
    
    // The entire WT10G is a single tar.gz file
    void indexTarArchive() throws Exception {
        GZIPInputStream gzipInputStream = new GZIPInputStream( new FileInputStream(new File(tarArchivePath)));
        TarArchiveInputStream tarArchiveInputStream =
                new TarArchiveInputStream(gzipInputStream);
        TarArchiveEntry tarEntry = tarArchiveInputStream.getNextTarEntry();
        while (tarEntry != null) {
            if (!tarEntry.isDirectory()) {
                System.out.println("Extracting " + tarEntry.getName());
                final OutputStream outputFileStream = new FileOutputStream(tarEntry.getName());
            }
            else {
                tarEntry = tarArchiveInputStream.getNextTarEntry();
            }
        }
    }
    
    public static void main(String[] args) {
        if (args.length == 0) {
            args = new String[1];
            System.out.println("Usage: java WTDocIndexer <prop-file>");
            args[0] = "wt10g.properties";
        }

        try {
            WTDocIndexer indexer = new WTDocIndexer(args[0]);
            indexer.processAll();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }        
}
