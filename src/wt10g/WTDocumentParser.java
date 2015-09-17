/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wt10g;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.util.Version;

/**
 *
 * @author Debasis
 */
public class WTDocumentParser {
    File ifile;
    StringBuffer buff;  // Accumulation buffer for storing the current topic
    List<WTDocument> docs;
    WTDocument doc;
    Analyzer analyzer;
    static final int offset = 7; // length of "<DOCNO>"

    public WTDocumentParser(File ifile, Analyzer analyzer) {
        this.ifile = ifile;
        buff = new StringBuffer();
        this.analyzer = analyzer;
        docs = new ArrayList<WTDocument>();
    }

    public void parse() throws Exception {
        FileReader fr = new FileReader(ifile);        
        BufferedReader br = new BufferedReader(fr);
        StringBuffer htmlBuff = null;
        String line;
        int startpos, endpos;
        boolean htmlStart = false;
        
        while ((line = br.readLine()) != null) {
            startpos = line.indexOf("<DOCNO>");
            if (startpos >= 0) {
                endpos = line.indexOf("</DOCNO>");
                doc.docNo = line.substring(startpos + offset, endpos);                
            }
            else if (line.startsWith("<DOC>")) {
                doc = new WTDocument(analyzer);
            }
            else if (line.startsWith("</DOC>")) {
                doc.html = htmlBuff.toString();
                doc.extractText();
                docs.add(doc);
                htmlStart = false; // we have collected all the html for the current doc...
            }
            else if (htmlStart) {
                htmlBuff.append(line).append("\n");
            }
            else if (line.indexOf("</DOCHDR>") >= 0) {
                htmlStart = true;
                htmlBuff = new StringBuffer();
            }
        }
        br.close();
        fr.close();
    }
    
    public String toString() {
        StringBuffer buff = new StringBuffer();
        for (WTDocument d : docs) {
            buff.append(d.toString()).append("\n");
        }
        return buff.toString();
    }
    
    public List<Document> getDocuments() {
        List<Document> luceneDocs = new ArrayList<>();
        for (WTDocument wtdoc : docs) {
            luceneDocs.add(wtdoc.constructLuceneDoc());
        }
        return luceneDocs;
    }
    
    public static void main(String[] args) {
        try {
            WTDocumentParser parser = new WTDocumentParser(
                    new File("wt001"),
                    new EnglishAnalyzer(Version.LUCENE_4_9));
            parser.parse();
            System.out.println(parser);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

