/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wt10g;

import static indexing.TrecDocIndexer.FIELD_ANALYZED_CONTENT;
import static indexing.TrecDocIndexer.FIELD_ID;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.zip.GZIPOutputStream;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.util.BytesRef;

/**
 *
 * @author Debasis
 */

// TREC Web-track document

public class WTDocument {
    String docNo;
    String title;
    String html;
    String text;
    Analyzer analyzer;
    
    public static final String WTDOC_FIELD_TITLE = "title";
    public static final String WTDOC_FIELD_HTML = "html";
    static final int MAX_CHARACTERS = -1;

    public WTDocument(Analyzer analyzer) {
        this.analyzer = analyzer;
    }
    
    void extractText() {
        text = html;
        /* It turns out that treating the entire HTML as text produces
        better results.
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
        */
    }

    BytesRef compress(String str) {
        ByteArrayOutputStream out = null;
        try {
            out = new ByteArrayOutputStream();
            GZIPOutputStream gzip = new GZIPOutputStream(out);
            gzip.write(str.getBytes());
            gzip.close();
        }
        catch (Exception ex) {
            return new BytesRef("");
        }
        return out==null? new BytesRef("") : new BytesRef(out.toByteArray());
    }
    
    String preProcess(String text) throws Exception {

        StringBuffer tokenizedContentBuff = new StringBuffer();

        TokenStream stream = analyzer.tokenStream(FIELD_ANALYZED_CONTENT, new StringReader(text));
        CharTermAttribute termAtt = stream.addAttribute(CharTermAttribute.class);
        stream.reset();

        while (stream.incrementToken()) {
            String term = termAtt.toString();
            term = term.toLowerCase();
            tokenizedContentBuff.append(term).append(" ");
        }

        stream.end();
        stream.close();
        return tokenizedContentBuff.toString();
    }
    
    Document constructLuceneDoc() {
        Document doc = new Document();
        
        if (this.title == null)
            this.title = "";
        
        doc.add(new Field(FIELD_ID, this.docNo, Field.Store.YES, Field.Index.NOT_ANALYZED));

        // store the title and the raw html
        doc.add(new Field(WTDOC_FIELD_TITLE, this.title,
                Field.Store.NO, Field.Index.ANALYZED));
        
        /*
        String ppContent = null;
        try {
            ppContent = preProcess(this.text);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            ppContent = "";
        }
        */
        
        // the words (also store the term vector)
        doc.add(new Field(FIELD_ANALYZED_CONTENT, this.title + " " + this.text,
                Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES));
        
        return doc;        
    }
    
    public String toString() {
        return docNo + "\n" + title + "\n" + text;
    }
}

