/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wt10g;

import static indexing.TrecDocIndexer.FIELD_ANALYZED_CONTENT;
import static indexing.TrecDocIndexer.FIELD_ID;
import org.apache.lucene.analysis.Analyzer;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.html.HtmlParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import static wt10g.WTDocument.WTDOC_FIELD_TITLE;

/**
 *
 * @author Debasis
 */
public class KhresmoiDoc extends WTDocument {

    public KhresmoiDoc(Analyzer analyzer) {
        super(analyzer);
    }
    
    @Override
    void extractText() {
        try {
            InputStream input = new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8));
            ContentHandler handler = new BodyContentHandler(MAX_CHARACTERS);
            Metadata metadata = new Metadata();
            new HtmlParser().parse(input, handler, metadata, new ParseContext());
            title = metadata.get("title");
            text = title + "\n" + handler.toString();
        }
        catch (Exception ex) {
            text = html;
        }    
    }
    
    Document constructLuceneDoc() {
        Document doc = new Document();
        
        if (this.title == null)
            this.title = "";
        
        doc.add(new Field(FIELD_ID, this.docNo, Field.Store.YES, Field.Index.NOT_ANALYZED));

        // store the title and the raw html
        doc.add(new Field(WTDOC_FIELD_TITLE, this.title,
                Field.Store.NO, Field.Index.ANALYZED));
        
        String ppContent = null;
        try {
            ppContent = preProcess(this.title + " " + this.text);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            ppContent = "";
        }
        
        // the words (also store the term vector)
        doc.add(new Field(FIELD_ANALYZED_CONTENT, ppContent,
                Field.Store.YES, Field.Index.NOT_ANALYZED, Field.TermVector.NO));
        
        return doc;        
    }
    
}
