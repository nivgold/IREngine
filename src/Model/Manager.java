package Model;

import Model.communicator.ConfigReader;
import Model.dataTypes.Document;
import Model.dataTypes.DocumentDetails;
import Model.dataTypes.Query;
import Model.dataTypes.TermDetails;
import Model.index.Indexer;
import Model.index.InvertedIndexCreator;
import Model.preproccesing.Parse;
import Model.preproccesing.ReadFile;
import Model.queryRetreival.QueryReader;
import Model.queryRetreival.Searcher;

import javax.swing.text.html.parser.Entity;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager responsible for calling the workers and finally creating the final posting file and inverted index dictionary
 */
public class Manager {
    private Map<String, TermDetails> dictionary;
    private int CORPUS_FILE_NUM;
    private int WORKERS_NUM;
    private int DOCS_PER_WORKER;
    private int DOCS_LEFT;
    private double total;
    private Map<String, List<String>> documentsEntityMap;
    private Map<String, DocumentDetails> documentDictionary;

    public Map<String, List<String>> getDocumentsEntityMap(){
        return documentsEntityMap;
    }

    private void createDocumentsEntityMap(){
        this.documentsEntityMap = new HashMap<>();
        Set entrySet = dictionary.entrySet();
        List<Map.Entry<String, TermDetails>> sortedDictionary = new ArrayList<>(entrySet);
        Collections.sort(sortedDictionary, new Comparator<Map.Entry<String, TermDetails>>() {
            @Override
            public int compare(Map.Entry<String, TermDetails> o1, Map.Entry<String, TermDetails> o2) {
                return o1.getKey().toLowerCase().compareTo(o2.getKey().toLowerCase());
            }
        });
        BufferedReader bufferedReader = null;
        int currentLineIndex = 0;
        try{
            bufferedReader = new BufferedReader(new FileReader(ConfigReader.FINAL_POSTING_FILE_PATH));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        for (Map.Entry<String, TermDetails> entry : sortedDictionary){
            String term = entry.getKey();
            if (term.equals(term.toUpperCase()) && (term.charAt(0) >= 'A' && term.charAt(0) <= 'Z')){
                TermDetails termDetails = entry.getValue();
                int pointer = Integer.parseInt(termDetails.getPostingPointer());
                String postingValue = "";
                try{
                    postingValue = bufferedReader.readLine();
                    while (pointer != currentLineIndex){
                        currentLineIndex++;
                        postingValue = bufferedReader.readLine();
                    }
                    currentLineIndex++;
                    String[] documents = postingValue.split(";");
                    for (String document : documents){
                        String docNo = document.substring(0, document.indexOf(":"));
                        String termTF = document.substring(document.indexOf(":")+1);
                        double normalizedTF = Double.parseDouble(termTF)/documentDictionary.get(docNo).getMaxTF();
                        if (!documentsEntityMap.containsKey(docNo)){
                            List<String> entities = new ArrayList<>();
                            entities.add(term+":"+normalizedTF);
                            documentsEntityMap.put(docNo, entities);
                        }
                        else{
                            List<String> entities = documentsEntityMap.get(docNo);
                            if (entities.size()==5){
                                String min = Collections.min(entities, new Comparator<String>() {
                                    @Override
                                    public int compare(String o1, String o2) {
                                        return o1.substring(o1.indexOf(":")+1).compareTo(o2.substring(o2.indexOf(":")+1));
                                    }
                                });
                                if (normalizedTF+"".compareTo(min.substring(min.indexOf(":")+1))>=0){
                                    entities.remove(min);
                                    entities.add(term+":"+normalizedTF);
                                }
                            }
                            else{
                                entities.add(term+":"+normalizedTF);
                            }
                        }
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // save to disk
        try{
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("D:\\entities.txt"));
            for (Map.Entry<String, List<String>> entry : documentsEntityMap.entrySet()){
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("DocNo:");
                stringBuilder.append(entry.getKey());
                stringBuilder.append(" entities:");
                stringBuilder.append(entry.getValue().toString());
                bufferedWriter.write(stringBuilder.toString());
                bufferedWriter.newLine();
            }
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<String, List<String>> retrieveFromPath(String path){
        HashMap<String, List<String>> results = new HashMap<>();
        QueryReader queryReader = new QueryReader();
        Searcher searcher = new Searcher();
        Set<Query> queries = queryReader.extractQueriesFromPath(path);
        for (Query query : queries) {
            results.put(query.getQueryID(), searcher.search(query, dictionary, documentDictionary));
        }

        return results;
    }

    public List<String> retrieveFromText(String text){
        QueryReader queryReader = new QueryReader();
        Searcher searcher = new Searcher();
        Query query = queryReader.makeQuery(text);
        return searcher.search(query, dictionary, documentDictionary);
    }

    private void loadDocumentDictionary(){
        this.documentDictionary = new HashMap<>();
        int workerIDIndex=5;
        while (workerIDIndex>0){
            int batchIDIndex=36;
            while (batchIDIndex>0){
                try {
                    String filePath = ConfigReader.DOCUMENT_POSTING_PATH + "document_posting_worker" + workerIDIndex + "_batch" + batchIDIndex + ".txt";
                    System.out.println("opening file:"+filePath);
                    BufferedReader bufferedReader = new BufferedReader(new FileReader(filePath));
                    String currentLine="";
                    while ((currentLine = bufferedReader.readLine())!=null){
                        String[] values = currentLine.split(";");
                        documentDictionary.put(values[0], new DocumentDetails(values[0], Integer.parseInt(values[1]), Integer.parseInt(values[2]), Integer.parseInt(values[3])));
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                batchIDIndex--;
            }
            workerIDIndex--;
        }
    }

    /**
     * starting the indexing process
     */
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

        //loading the document dictionary
        createDocumentsEntityMap();

        // clean the up-updated concurrent HashMap
        Indexer.invertedIndexDictionary.clear();

        double end = System.nanoTime();
        this.total = (end - start)/1000000000.0;
        System.out.println("Total time: "+(total/60)+" mins");
    }

    /**
     * initializing the working environment to the indexing process
     */
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

    /**
     * loading a dictionary that is already saved in the disk
     */
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

        System.out.println("loading document dictionary");
        loadDocumentDictionary();
        System.out.println("creating document entity map");
        createDocumentsEntityMap();
    }

    /**
     * returns number of unique terms in the dictionary
     * @return number of unique terms
     */
    public int getUniqueTermsNum(){
        return this.dictionary.size();
    }

    /**
     * returns the total time took to run the whole indexing process
     * @return total time
     */
    public double getTotalTime(){
        return this.total;
    }
}