package Model;

import Model.communicator.ConfigReader;
import Model.dataTypes.Document;
import Model.index.Indexer;
import Model.preproccesing.Parse;
import Model.preproccesing.ReadFile;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;

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

    private final String DOCUMENT_POSTING_PATH = ConfigReader.FINAL_POSTING_DIRECTORY_PATH;

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
        for (int i=0; i<BATCH_NUM; i++){
            if (i == BATCH_NUM-1){
                readFile = new ReadFile(ConfigReader.CORPUS_PATH, startIndex +(batchFilesNum*i), batchFilesNum+filesLeft);
            }
            else{
                readFile = new ReadFile(ConfigReader.CORPUS_PATH, startIndex +(batchFilesNum*i), batchFilesNum);
            }
            readFile.run();
            parser = new Parse();
            parser.parseDocsBatch(readFile.getResult());

            // write batch of documents to disk
            saveDocsToDisk(parser.documents, i);


            this.indexer.updateParserBatch(parser.getTermDocsMap(), i);
            System.out.println("worker"+workerID+" finished batch: "+(i+1)+"/"+BATCH_NUM);
        }

        double end = System.nanoTime();
        double total = (end - start)/1000000000.0;
        System.out.println("worker"+this.workerID+" done in: "+(total/60));
    }

    private void saveDocsToDisk(HashSet<Document> documents, int currentBatch) {
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(DOCUMENT_POSTING_PATH+"\\document_posting_worker"+workerID+"_batch"+(currentBatch+1)+".txt"));
            Iterator<Document> iterator = documents.iterator();
            while (iterator.hasNext()) {
                Document document = iterator.next();
                bufferedWriter.write(document.getDocNo() + ";" + document.getMaxTF() + ";" + document.uniqueTerms() + ";" + document.getTextLength());
                bufferedWriter.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
