/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package wvec;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 *
 * @author Debasis
 */

/**
 * A collection of WordVec instances for each unique term in
 * the collection.
 * @author Debasis
 */
public class WordVecs {

    Properties prop;
    int k;
    HashMap<String, WordVec> wordvecmap;
    HashMap<String, List<WordVec>> nearestWordVecsMap; // Store the pre-computed NNs
    
    public WordVecs(String propFile) throws Exception {
        prop = new Properties();
        prop.load(new FileReader(propFile));        
        init();
    }
    
    public WordVecs(Properties prop) throws Exception {
        this.prop = prop;
        init();
    }
    
    void init() {
        if (wordvecmap != null)
            return; // already loaded from somewhere else in the flow...
        
        k = Integer.parseInt(prop.getProperty("wordvecs.numnearest", "5"));
        String loadFrom = prop.getProperty("wordvecs.readfrom");
        if (loadFrom.equals("vec"))
            loadFromTextFile();
        else
            loadObjectFromSerFile();
    }
    
    void loadFromTextFile() {
        String wordvecFile = prop.getProperty("wordvecs.vecfile");
        wordvecmap = new HashMap();
        try (FileReader fr = new FileReader(wordvecFile);
                BufferedReader br = new BufferedReader(fr)) {
            String line;
            
            while ((line = br.readLine()) != null) {
                WordVec wv = new WordVec(line);
                wordvecmap.put(wv.word, wv);
            }
        }
        catch (Exception ex) { ex.printStackTrace(); }        
    }
    
    void loadObjectFromSerFile() {
        try {
            File serFile = new File(prop.getProperty("wordvecs.objfile"));
            FileInputStream fin = new FileInputStream(serFile);
            ObjectInputStream oin = new ObjectInputStream(fin);
            wordvecmap = (HashMap<String, WordVec>)oin.readObject();
            oin.close();
            fin.close();
        }
        catch (Exception ex) { ex.printStackTrace(); }
    }

    public void storeVectorsAsSerializedObject() throws Exception {
        File oFile = new File(prop.getProperty("wordvecs.objfile"));
        FileOutputStream fout = new FileOutputStream(oFile);
        ObjectOutputStream oos = new ObjectOutputStream(fout);
        oos.writeObject(this.wordvecmap);
        oos.close();
        fout.close();
    }
    
    public void computeAndStoreNNs() {
        nearestWordVecsMap = new HashMap<>(wordvecmap.size());
        
        for (Map.Entry<String, WordVec> entry : wordvecmap.entrySet()) {
            WordVec wv = entry.getValue();
            List<WordVec> nns = getNearestNeighbors(wv.word);
            if (nns != null) {
                nearestWordVecsMap.put(wv.word, nns);
            }
        }
    }
    
    public List<WordVec> getNearestNeighbors(String queryWord) {
        ArrayList<WordVec> distList = new ArrayList<>(wordvecmap.size());
        
        WordVec queryVec = wordvecmap.get(queryWord);
        if (queryVec == null)
            return null;
        
        for (Map.Entry<String, WordVec> entry : wordvecmap.entrySet()) {
            WordVec wv = entry.getValue();
            if (wv.word.equals(queryWord))
                continue;
            wv.querySim = queryVec.cosineSim(wv);
            distList.add(wv);
        }
        Collections.sort(distList);
        return distList.subList(0, Math.min(k, distList.size()));        
    }

    // Sequentially compute the distances of every vector from a query
    // vector and store the sims in the wvec object.
    public List<WordVec> getNearestNeighbors(WordVec queryVec) {
        ArrayList<WordVec> distList = new ArrayList<>(wordvecmap.size());
        
        for (Map.Entry<String, WordVec> entry : wordvecmap.entrySet()) {
            WordVec wv = entry.getValue();
            wv.querySim = queryVec.cosineSim(wv);
            distList.add(wv);
        }
        
        Collections.sort(distList);
        return distList.subList(0, Math.min(k, distList.size()));        
    }
    
    public WordVec getVec(String word) {
        return wordvecmap.get(word);
    }

    public float getSim(String u, String v) {
        WordVec uVec = wordvecmap.get(u);
        WordVec vVec = wordvecmap.get(v);
        return uVec.cosineSim(vVec);
    }
    
    public static void main(String[] args) {
        try {
            WordVecs qe = new WordVecs("wt10g.properties");
            qe.storeVectorsAsSerializedObject();
            /*
            List<WordVec> nwords = qe.getNearestNeighbors("test");
            for (WordVec word : nwords) {
                System.out.println(word.word + "\t" + word.querySim);
            }
            * */
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
