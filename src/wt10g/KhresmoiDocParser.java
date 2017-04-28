/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wt10g;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import org.apache.lucene.analysis.Analyzer;
import wt10g.WTDocument;
import wt10g.WTDocumentParser;
/**
 *
 * @author Debasis
 */
public class KhresmoiDocParser extends WTDocumentParser {

    public KhresmoiDocParser(File ifile, Analyzer analyzer) {
        super(ifile, analyzer);
    }
    
    @Override
    public void parse() throws Exception {
        FileReader fr = new FileReader(ifile);        
        BufferedReader br = new BufferedReader(fr);
        StringBuffer htmlBuff = null;
        String line;
        int startpos;
        boolean htmlStart = false;
        
        final String docStartPattern = "#UID:";
        final int len = docStartPattern.length();
        
        while ((line = br.readLine()) != null) {
            startpos = line.indexOf(docStartPattern);
            if (startpos >= 0) {
                doc = new KhresmoiDoc(analyzer);
                doc.docNo = line.substring(startpos + len);                
            }
            else if (line.startsWith("#CONTENT")) {
                htmlBuff = new StringBuffer();
                htmlStart = true;
            }
            else if (line.startsWith("#EOR")) {
                doc.html = htmlBuff.toString();
                doc.extractText();
                docs.add(doc);
            }
            else if (htmlStart) {
                htmlBuff.append(line).append("\n");
            }
        }
        br.close();
        fr.close();        
    }    
}
