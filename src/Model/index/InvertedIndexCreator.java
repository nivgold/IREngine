package Model.index;

import Model.communicator.ConfigReader;
import Model.dataTypes.TermDetails;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InvertedIndexCreator {
    private final static String INVERTED_DICTIONARY_FILE_PATH = ConfigReader.INVERTED_DICTIONARY_FILE_PATH;
    private final static String FINAL_POSTING_FILE_PATH = ConfigReader.FINAL_POSTING_FILE_PATH;

    // TODO: look for a way to minimize the memory consumption when converting the ConcurrentHashMap to regular HashMap
    public static void create(ConcurrentHashMap<String, TermDetails> initialDictionary){
        Map<String, TermDetails> invertedIndexDictionary = new HashMap<>(initialDictionary);
        System.out.println(invertedIndexDictionary.size() +" Terms is in the inverted index dictionary initially");
        Iterator<String> iterator = invertedIndexDictionary.keySet().iterator();
        while (iterator.hasNext()){
            String term = iterator.next();
            // capital rule
            if (invertedIndexDictionary.containsKey(term.toLowerCase())){
                if (term.equals(term.toUpperCase()) && !term.toUpperCase().equals(term.toLowerCase())){
                    TermDetails removedtermDetails = invertedIndexDictionary.get(term);
                    iterator.remove();
                    invertedIndexDictionary.get(term.toLowerCase()).addDF(removedtermDetails.getDf());
                }
            }

            // entity rule
            if (term.endsWith(" ")){
                if (invertedIndexDictionary.get(term).getDf()<2){
                    iterator.remove();
                }
            }
        }

        System.out.println(invertedIndexDictionary.size() +" Terms is in the inverted index dictionary at the end");

        System.out.println("Starting Merger");
        Merger.merge(invertedIndexDictionary);
        System.out.println("Merger Finished");

        saveToDisk(invertedIndexDictionary);
    }

    private static void saveToDisk(Map<String, TermDetails> invertedIndexDictionary){
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(FINAL_POSTING_FILE_PATH));
            String currentLine="";
            int lineNumber = 0;
            while ((currentLine=bufferedReader.readLine()) != null){
                String term = currentLine.substring(0, currentLine.indexOf("="));
                invertedIndexDictionary.get(term).setPostingPointer(lineNumber);
                lineNumber++;
            }
            bufferedReader.close();
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(INVERTED_DICTIONARY_FILE_PATH));
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
