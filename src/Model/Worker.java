package Model;

import Model.communicator.ConfigReader;
import Model.dataTypes.Document;
import Model.index.Indexer;
import Model.preproccesing.Parse;
import Model.preproccesing.ReadFile;

import java.io.*;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

// handling the concurrent batch indexing from reading corpus files to inverted index
public class Worker implements Runnable{
    // number of batches the worker will do
    private final int BATCH_NUM = ConfigReader.WORKER_BATCH_NUM;

    // ID of the worker
    private int workerID;

    // the file that the worker starts his job with
    private int startIndex;

    // number of files the worker do in each batch
    private int batchFilesNum;

    // number of files left that the last worker needs to do
    private int filesLeft;

    // the indexer that responsible for writing the per-posting for the batch
    public Indexer indexer;

    private final String DOCUMENT_POSTING_PATH = ConfigReader.DOCUMENT_POSTING_PATH;

    public Worker(int workerID, int startIndex, int totalFiles) {
        this.workerID = workerID;
        this.indexer = new Indexer(workerID+"");
        this.startIndex = startIndex;
        this.batchFilesNum = totalFiles / BATCH_NUM;
        this.filesLeft = totalFiles - (this.batchFilesNum * BATCH_NUM);
    }

    @Override
    public void run() {
        double start = System.nanoTime();
        ReadFile readFile;
        Parse parser;
        Set<String> stopWords = loadStopWords();
        for (int i=0; i<BATCH_NUM; i++){
            if (i == BATCH_NUM-1){
                readFile = new ReadFile(ConfigReader.CORPUS_DIR_PATH, startIndex +(batchFilesNum*i), batchFilesNum+filesLeft);
            }
            else{
                readFile = new ReadFile(ConfigReader.CORPUS_DIR_PATH, startIndex +(batchFilesNum*i), batchFilesNum);
            }
            readFile.read();
            parser = new Parse(readFile.getResult(), stopWords);
            parser.parseBatch();

            // write batch of documents to disk
            saveDocsToDisk(parser.documents, i);


            this.indexer.updateParserBatch(parser.getTermDocsMap(), i);
            System.out.println("worker"+workerID+" finished batch: "+(i+1)+"/"+BATCH_NUM);
        }

        double end = System.nanoTime();
        double total = (end - start)/1000000000.0;
        System.out.println("worker"+this.workerID+" done in: "+(total/60));
    }

    /**
     * saving all the batch document and their data to disk
     * @param documents List of Documents
     * @param currentBatch current worker's batch number
     */
    private void saveDocsToDisk(HashSet<Document> documents, int currentBatch) {
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(DOCUMENT_POSTING_PATH+"document_posting_worker"+workerID+"_batch"+(currentBatch+1)+".txt"));
            Iterator<Document> iterator = documents.iterator();
            while (iterator.hasNext()) {
                Document document = iterator.next();
                iterator.remove();
                bufferedWriter.write(document.toString());
                bufferedWriter.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * loading the 'stop_words' file from the disk
     */
    private Set<String> loadStopWords(){
        Set<String> stopWords = new HashSet<>();
        try {
            FileReader fileReader = new FileReader(ConfigReader.STOP_WORDS_FILE_PATH);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String currentLine;
            while ((currentLine = bufferedReader.readLine()) != null) {
                stopWords.add(currentLine);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stopWords;
    }
}
