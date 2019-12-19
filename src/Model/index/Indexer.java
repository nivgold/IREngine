package Model.index;

import Model.dataTypes.AllTermDocs;
import Model.dataTypes.TermDetails;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

// getting the batch parsed terms and responsible for 2 things: updating the inverted index dictionary
// AND calling for BatchFlusher to write to disk
// also responsible for creating the final dictionary
public class Indexer {
    // the ID of the worker
    private String workerID;

    // the shared un-updated inverted index dictionary that each worker adds his batch to it
    public static ConcurrentHashMap<String, TermDetails> invertedIndexDictionary = new ConcurrentHashMap<>();

    // responsible for writing the batch pre-posting file
    private BatchFlusher batchFlusher;

    public Indexer(String worker){
        this.workerID = worker;
        this.batchFlusher = new BatchFlusher(workerID);
    }

    /**
     * update the shared inverted index dictionary and responsible for writing the batch pre-posting file
     * @param termDocsList batch term-sorted list of: String, AllTermDocs
     * @param batchID worker's batch number
     */
    public void updateParserBatch(ArrayList<Map.Entry<String, AllTermDocs>> termDocsList, int batchID){
        // writes the batch pre-posting file
        batchFlusher.flushBatch(termDocsList, batchID);

        // update the shared inverted index dictionary
        updateDictionary(termDocsList);
    }

    /**
     * update the shared inverted index dictionary
     * @param termDocsList batch term-sorted list of: String, AllTermDocs
     */
    private void updateDictionary(ArrayList<Map.Entry<String, AllTermDocs>> termDocsList){
        // iterating over the mapped docs for each term
        for (Map.Entry<String, AllTermDocs> entry : termDocsList){
            // getting the term
            String term = entry.getKey();
            // getting the number of docs (df) the term occurred (tf) in the batch
            int df = entry.getValue().getDF();
            int tf = entry.getValue().getTermTFInBatch();
            // update the inverted index dictionary with the term and his details
            updateTerm(term, df, tf);
        }
    }

    /**
     * update the shared inverted index dictionary with the given term and his df and tf
     * @param term updates the inverted index dictionary
     * @param df batch df of the term
     * @param tf batch tf of the term
     */
    private void updateTerm(String term, int df, int tf){
        invertedIndexDictionary.computeIfPresent(term.toUpperCase(), new BiFunction<String, TermDetails, TermDetails>() {
            @Override
            public TermDetails apply(String s, TermDetails termDetails) {
                TermDetails toAdd = termDetails;
                toAdd.addDF(df);
                toAdd.addTF(tf);
                return toAdd;
            }
        });
        invertedIndexDictionary.computeIfAbsent(term, new Function<String, TermDetails>() {
            @Override
            public TermDetails apply(String s) {
                TermDetails termDetails = new TermDetails(df, tf);
                return termDetails;
            }
        });
    }
}
