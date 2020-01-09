package Model.index;

import Model.communicator.ConfigReader;
import Model.dataTypes.TermDetails;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * responsible for creating the final updated inverted index dictionary
 */
public class InvertedIndexCreator {
    private String INVERTED_DICTIONARY_FILE_PATH = ConfigReader.INVERTED_DICTIONARY_FILE_PATH;
    private String FINAL_POSTING_FILE_PATH = ConfigReader.FINAL_POSTING_FILE_PATH;
    private Merger merger;

    public InvertedIndexCreator() {
        this.merger = new Merger();
    }

    /**
     * creating a new updated inverted index dictionary from all the worker's batches
     * @param initialDictionary un-updated inverted index dictionary
     * @return updated inverted index dictionary
     */
    public Map<String, TermDetails> create(ConcurrentHashMap<String, TermDetails> initialDictionary){

        Map<String, TermDetails> invertedIndexDictionary = new HashMap<>(initialDictionary);
        System.out.println(invertedIndexDictionary.size() +" Terms is in the inverted index dictionary initially");
        Iterator<String> iterator = invertedIndexDictionary.keySet().iterator();
        while (iterator.hasNext()){
            String term = iterator.next();
            // capital rule
            if (invertedIndexDictionary.containsKey(term.toLowerCase())){
                if (term.equals(term.toUpperCase()) && !term.toUpperCase().equals(term.toLowerCase())){
                    TermDetails removedTermDetails = invertedIndexDictionary.get(term);
                    iterator.remove();
                    invertedIndexDictionary.get(term.toLowerCase()).addDF(removedTermDetails.getDF());
                    invertedIndexDictionary.get(term.toLowerCase()).addTF(removedTermDetails.getCorpusTF());
                }
            }

            // entity rule
            if (term.endsWith(" ")){
                if (invertedIndexDictionary.get(term).getDF()<2){
                    iterator.remove();
                }
            }
        }

        System.out.println(invertedIndexDictionary.size() +" Terms is in the inverted index dictionary at the end");

        System.out.println("Starting Merger");
        this.merger.merge(invertedIndexDictionary);
        System.out.println("Merger Finished");

        saveToDisk(invertedIndexDictionary);
        return invertedIndexDictionary;
    }

    /**
     * saving the updated final dictionary to the disk
     * @param invertedIndexDictionary final inverted index dictionary
     */
    private void saveToDisk(Map<String, TermDetails> invertedIndexDictionary){
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(FINAL_POSTING_FILE_PATH));
            String updatedName = FINAL_POSTING_FILE_PATH.substring(0, FINAL_POSTING_FILE_PATH.indexOf("."));
            updatedName = updatedName+"_updated.txt";
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(updatedName));
            String currentLine="";
            int lineNumber = 0;
            while ((currentLine=bufferedReader.readLine()) != null){
                String term = currentLine.substring(0, currentLine.indexOf("="));
                TermDetails termDetails = invertedIndexDictionary.get(term);
                termDetails.setPostingPointer(lineNumber+"");
                lineNumber++;

                bufferedWriter.write(currentLine.substring(currentLine.indexOf("=")+1));
                bufferedWriter.newLine();
            }
            bufferedReader.close();
            bufferedWriter.close();
            File first = new File(FINAL_POSTING_FILE_PATH);
            File updated = new File(updatedName);
            first.delete();
            updated.renameTo(first);

            bufferedWriter = new BufferedWriter(new FileWriter(INVERTED_DICTIONARY_FILE_PATH));
            for (Map.Entry<String, TermDetails> entry : invertedIndexDictionary.entrySet()){
                String term = entry.getKey();
                TermDetails termDetails = entry.getValue();
                bufferedWriter.write(term+";"+termDetails);
                bufferedWriter.newLine();
            }
            bufferedWriter.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
