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

    /**
     * creating the document-entity map that maps every document to its dominant entities
     */
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
    }

    /**
     * returns the docNo Dominant Entities
     * @param docNo That Represents The Documents ID
     * @return String That Represents The docNo Dominant Entities
     */
    public String getDocDominantEntities(String docNo){
        String result="";
        if (documentsEntityMap.containsKey(docNo)) {
            List<String> entities = documentsEntityMap.get(docNo);
            for (String entity : entities){
                result+= entity+" ";
            }
            result = result.substring(0, result.length()-1);
        }
        return result;
    }

    /**
     * returns all the queries relevant documents
     * @return Map that maps every query to its result (relevant documents)
     */
    public Map<String, List<Map.Entry<String, Double>>> retrieveFromPath(){
        String path = ConfigReader.QUERIES_FILE_PATH;
        HashMap<String, List<Map.Entry<String, Double>>> results = new HashMap<>();
        QueryReader queryReader = new QueryReader();
        Searcher searcher = new Searcher();
        Set<Query> queries = queryReader.extractQueriesFromPath(path);
        for (Query query : queries) {
            results.put(query.getQueryID(), searcher.search(query, dictionary, documentDictionary));
        }

        return results;
    }

    public Map<String, List<Map.Entry<String, Double>>> retrieveFromText(String text){
        Map<String, List<Map.Entry<String, Double>>> result = new HashMap<>();
        QueryReader queryReader = new QueryReader();
        Searcher searcher = new Searcher();
        Query query = queryReader.makeQuery(text);
        result.put(query.getQueryID(), searcher.search(query, dictionary, documentDictionary));
        return result;
    }

    private void loadDocumentDictionary(){
        this.documentDictionary = new HashMap<>();
        int totalDocLength = 0;
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
                        totalDocLength+=Integer.parseInt(values[3]);
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
        Parse.docCounter.set(documentDictionary.size());
        Parse.totalDocLength.set(totalDocLength);
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
        loadDocumentDictionary();
        //creating the document entity map
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
        System.out.println("Done");
        System.out.println("creating document entity map");
        createDocumentsEntityMap();
        System.out.println("Done");
    }

    /**
     * returns number of unique terms in the dictionary
     * @return number of unique terms
     */
    public int getUniqueTermsNum(){
        return this.dictionary.size();
    }

    /**
     * telling whether there is a loaded dictionary or not
     * @return true if dictionary is not null and false if it is null
     */
    public boolean hasDictioanry(){
        if (this.dictionary==null)
            return false;
        return true;
    }

    /**
     * returns the total time took to run the whole indexing process
     * @return total time
     */
    public double getTotalTime(){
        return this.total;
    }
}