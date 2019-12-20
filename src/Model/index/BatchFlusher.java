package Model.index;

import Model.communicator.ConfigReader;
import Model.dataTypes.AllTermDocs;

import java.io.*;
import java.util.*;

/**
 * responsible for writing each batch data to disk
 */
public class BatchFlusher {
    // the path for writing the each worker's batch pre-posting file
    private String workerPath;

    private BufferedWriter bufferedWriter;

    public BatchFlusher(String workerID) {
        this.workerPath = ConfigReader.BATCH_PRE_POSTING_DIR_PATH +"\\worker"+workerID;
    }

    /**
     * writes the termDocsList to pre-posting file
     * @param termDocsList batch term-sorted list of: String, AllTermDocs
     * @param batchID worker's batch number
     */
    public void flushBatch(ArrayList<Map.Entry<String, AllTermDocs>> termDocsList, int batchID){
        // opening a 'workerID_batchID.txt' file for writing
        openFile(batchID);
        // iterating over the mapped docs for each term
        for (Map.Entry<String, AllTermDocs> entry : termDocsList){
            // getting the term
            String term = entry.getKey();
            // writing the term and the posting value to the 'workerID_batchID.txt' file
            appendString(term+"="+entry.getValue());
        }
        // closing 'workerID_batchID.txt' file
        closeFile();
    }

    /**
     * appends a new line to the bufferWriter
     * @param toString line to be added to the file
     */
    private void appendString(String toString) {
        try {
            if (toString.endsWith(" "))
                toString = toString.substring(0, toString.length()-1);
            this.bufferedWriter.write(toString);
            this.bufferedWriter.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * closing the file that the bufferWriter writes to
     */
    private void closeFile() {
        try {
            this.bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * opening the file that the bufferWriter writes to
     * @param batchID number of the current batch
     */
    private void openFile(int batchID) {
        String fullPath = this.workerPath+"_batch"+(batchID+1)+".txt";
        try {
            this.bufferedWriter = new BufferedWriter(new FileWriter(fullPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
