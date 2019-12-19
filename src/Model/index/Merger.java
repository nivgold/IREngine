package Model.index;

import Model.communicator.ConfigReader;
import Model.dataTypes.TermDetails;

import java.io.*;
import java.util.Map;

/**
 * responsible for creating the final posting file
 */
public class Merger {
    private int WORKER_NUM = ConfigReader.WORKER_NUM;
    private int WORKER_BATCH_NUM = ConfigReader.WORKER_BATCH_NUM;
    private String BATCH_PRE_POSTING_DIR_PATH = ConfigReader.BATCH_PRE_POSTING_DIR_PATH;
    private String FINAL_POSTING_FILE_PATH = ConfigReader.FINAL_POSTING_FILE_PATH;
    private int filesAmount = WORKER_BATCH_NUM*WORKER_NUM;
    private BufferedReader[] bufferedReaders = new BufferedReader[filesAmount];
    private String[] currentLines = new String[filesAmount];

    /**
     * performing a merge sort on fileAmount of files to a final posting file
     * @param invertedIndexDictionary final inverted index dictionary
     */
    public void merge(Map<String, TermDetails> invertedIndexDictionary){
        try {
            int k = 0;
            for (int i = 1; i <= WORKER_NUM; i++) {
                for (int j = 1; j <= WORKER_BATCH_NUM; j++) {
                    bufferedReaders[k] = new BufferedReader(new FileReader(BATCH_PRE_POSTING_DIR_PATH + "\\worker" + i + "_batch" + j+".txt"));
                    k++;
                }
            }
            // open the output file
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(FINAL_POSTING_FILE_PATH),32767);

            // read firstline at all files
            for (int i=0; i<filesAmount; i++)
                currentLines[i] = bufferedReaders[i].readLine();

            int minIndex = checkLowestTerm();

            while (minIndex != -1){
                String postingValue = currentLines[minIndex];
                String minTerm = postingValue.substring(0, postingValue.indexOf('='));
                if (!invertedIndexDictionary.containsKey(minTerm)){
                    currentLines[minIndex] = bufferedReaders[minIndex].readLine();
                    minIndex = checkLowestTerm();
                    continue;
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(postingValue.substring(postingValue.indexOf('=')+1));

                // advance the file "pointer"
                currentLines[minIndex] = bufferedReaders[minIndex].readLine();

                int secMinIndex = checkLowestTerm();
                if (secMinIndex == -1){
                    bufferedWriter.write(minTerm+"="+stringBuilder);
                    break;
                }

                String secPostingValue = currentLines[secMinIndex];
                String secMinTerm = secPostingValue.substring(0, secPostingValue.indexOf('='));
                while (secMinTerm.toLowerCase().equals(minTerm.toLowerCase())){
                    // same term

                    // add to the accumulated data
                    stringBuilder.append(secPostingValue.substring(secPostingValue.indexOf('=')+1));
                    if (! minTerm.equals(secMinTerm)){
                        minTerm = minTerm.toLowerCase();
                    }
                    // advance the file "pointer"
                    currentLines[secMinIndex] = bufferedReaders[secMinIndex].readLine();
                    secMinIndex = checkLowestTerm();
                    if (secMinIndex == -1) {
                        break;
                    }
                    secPostingValue = currentLines[secMinIndex];
                    secMinTerm = secPostingValue.substring(0, secPostingValue.indexOf('='));
                }
                // throw the accumulated data to the

                bufferedWriter.write(minTerm+"="+stringBuilder);
                bufferedWriter.newLine();

                // advance minIndex
                minIndex = secMinIndex;
            }
            bufferedWriter.close();
            // closing all bufferReaders
            for (BufferedReader bufferedReader : bufferedReaders)
                bufferedReader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * finding the "minimum" line from the all files
     * @return index of the bufferReader of the file that contains the "minimum" line
     */
    private int checkLowestTerm(){
        int minIndex = -1;
        String minTerm = "zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz";
        for (int i=0; i<filesAmount; i++){
            if (currentLines[i] != null){
                String currentPosting = currentLines[i];
                String currentTerm = currentPosting.substring(0, currentPosting.indexOf('='));
                if (currentTerm.toLowerCase().compareTo(minTerm.toLowerCase()) < 0 ){
                    minIndex = i;
                    minTerm = currentTerm;
                }
            }
        }
        return minIndex;
    }
}