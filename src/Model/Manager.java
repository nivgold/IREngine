package Model;

import Model.communicator.ConfigReader;
import Model.dataTypes.TermDetails;
import Model.index.Indexer;
import Model.index.InvertedIndexCreator;
import Model.preproccesing.Parse;
import Model.preproccesing.ReadFile;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// calling the workers and finally creating the final posting file and inverted index dictionary
public class Manager {
    private Map<String, TermDetails> dictionary;
    private int CORPUS_FILE_NUM;
    private int WORKERS_NUM;
    private int DOCS_PER_WORKER;
    private int DOCS_LEFT;
    private double total;


    public void startProcess() {
        double start = System.nanoTime();

        // loading config
        ConfigReader.loadConfiguration();

        // initialize necessarily parameters
        initialize();
        System.out.println("Number of workers: "+WORKERS_NUM);

        Thread[] workers = new Thread[WORKERS_NUM];
        for (int i=0; i<WORKERS_NUM-1; i++)
            workers[i] = new Thread(new Worker(i+1, i*DOCS_PER_WORKER, DOCS_PER_WORKER));
        // the last worker has to get all the remaining files from the division
        workers[WORKERS_NUM-1] = new Thread(new Worker(WORKERS_NUM, (WORKERS_NUM-1)*DOCS_PER_WORKER, DOCS_PER_WORKER+DOCS_LEFT));

        // start the threads process
        for (Thread worker : workers)
            worker.start();
        try {
            for (Thread worker : workers)
                worker.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // verify that all the files where read

        // all the batch posting were created and now the final indexing can start
        InvertedIndexCreator indexCreator = new InvertedIndexCreator();
        this.dictionary = indexCreator.create(Indexer.invertedIndexDictionary);

        // clean the up-updated concurrent HashMap
        Indexer.invertedIndexDictionary.clear();

        double end = System.nanoTime();
        this.total = (end - start)/1000000000.0;
        System.out.println("Total time: "+(total/60)+" mins");
    }

    private void initialize(){
        String CORPUS_PATH = ConfigReader.CORPUS_DIR_PATH +"\\corpus";
        File mainDirectory = new File(CORPUS_PATH);
        CORPUS_FILE_NUM = mainDirectory.listFiles().length;
        WORKERS_NUM = ConfigReader.WORKER_NUM;
        DOCS_PER_WORKER = CORPUS_FILE_NUM/WORKERS_NUM;
        DOCS_LEFT = CORPUS_FILE_NUM - WORKERS_NUM*DOCS_PER_WORKER;

        // create temp dir
        File tempDir = new File(ConfigReader.BATCH_PRE_POSTING_DIR_PATH);
        tempDir.mkdirs();
        // creating
        File postingDir = new File(ConfigReader.POSTING_DIR_PATH);
        postingDir.mkdirs();

        // clean old memory
        Parse.docCounter.set(0);
        Indexer.invertedIndexDictionary = new ConcurrentHashMap<>();
    }

    public void loadDictionary(){
        try{
            this.dictionary = null;
            this.dictionary = new HashMap<>();
            BufferedReader bufferedReader = new BufferedReader(new FileReader(ConfigReader.INVERTED_DICTIONARY_FILE_PATH));
            String currentLine = "";
            while ((currentLine = bufferedReader.readLine()) != null){
                String[] dictionaryEntryData = currentLine.split(";");
                String term = dictionaryEntryData[0];
                int corpusTF = Integer.parseInt(dictionaryEntryData[1]);
                int df = Integer.parseInt(dictionaryEntryData[2]);
                String postingPointer = dictionaryEntryData[3];
                this.dictionary.put(term, new TermDetails(corpusTF, df, postingPointer));
            }
            bufferedReader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getUniqueTermsNum(){
        return this.dictionary.size();
    }

    public double getTotalTime(){
        return this.total;
    }
}
