package wvec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import org.apache.lucene.util.BytesRef;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * Reads from a word2vec file and expands the
 * query with the k-NN set of terms...

 * @author dganguly
 */

public class WordVec implements Comparable<WordVec>, Serializable {
    String word;
    float[] vec;
    float norm;
    float querySim; // distance from a reference query point
    transient boolean isComposed;

    public static final String COMPOSING_DELIM = ":";
    
    public WordVec(int dimension) { vec = new float[dimension]; }
    
    public WordVec(String line) {
        String[] tokens = line.split("\\s+");
        word = tokens[0];
        vec = new float[tokens.length-1];
        for (int i = 1; i < tokens.length; i++)
            vec[i-1] = Float.parseFloat(tokens[i]);
        norm = getNorm();
        // Normalize the vectors
        for (int i = 0; i < vec.length; i++)
            vec[i] = vec[i]/norm;
    }
    
    public boolean isComposed() { return isComposed; }
    
    public float getNorm() {
        // calculate and store
        float sum = 0;
        for (int i = 0; i < vec.length; i++) {
            sum += vec[i]*vec[i];
        }
        norm = (float)Math.sqrt(sum);
        return norm;
    }
    
    static public WordVec centroid(WordVec a, WordVec b) {
        WordVec sum = new WordVec(a.vec.length);
        sum.word = a.word + COMPOSING_DELIM + b.word;
        for (int i = 0; i < a.vec.length; i++) {
            sum.vec[i] = (a.vec[i] + b.vec[i]);
        }
        sum.isComposed = true;
        return sum;
    }
    
    public float cosineSim(WordVec that) {
        float sum = 0;
        for (int i = 0; i < this.vec.length; i++) {
            sum += vec[i] * that.vec[i];
        }
        return sum;
    }
    
    public float euclideanDist(WordVec that) {
        float sum = 0;
        for (int i = 0; i < this.vec.length; i++) {
            sum += (vec[i] - that.vec[i]) * ((vec[i] - that.vec[i]));
        }
        return (float)Math.sqrt(sum);
    }

    public String getWord() { return word; }
    public float getQuerySim() { return querySim; }
    
    @Override
    public int compareTo(WordVec that) {
        return this.querySim > that.querySim? -1 : this.querySim == that.querySim? 0 : 1;
    }
    
    public byte[] getBytes() throws IOException {
        byte[] byteArray;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            ObjectOutput out;
            out = new ObjectOutputStream(bos);
            out.writeObject(this);
            byteArray = bos.toByteArray();
            out.close();
        }
        return byteArray;
    }
    
    static WordVec decodeFromByteArray(BytesRef bytes) throws Exception {
        ObjectInput in;
        Object o;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes.bytes)) {
            in = new ObjectInputStream(bis);
            o = in.readObject();
        }
        in.close();
        return (WordVec)o;
    }
}

