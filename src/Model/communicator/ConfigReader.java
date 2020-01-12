package Model.communicator;

import java.io.*;
import java.util.Properties;

/**
 * responsible for communicating the objects in the program with the config file
 */
public class ConfigReader {
    private final static String CONFIG_PATH = "resources/config.properties";

    public static int WORKER_NUM;
    public static int WORKER_BATCH_NUM;

    public static String CORPUS_DIR_PATH;
    public static String POSTING_DIR_PATH;

    public static String BATCH_PRE_POSTING_DIR_PATH;
    public static String FINAL_POSTING_FILE_PATH;
    public static String INVERTED_DICTIONARY_FILE_PATH;
    public static String STOP_WORDS_FILE_PATH = "resources\\05 stop_words.txt";
    public static String DOCUMENT_POSTING_PATH;
    public static String QUERIES_FILE_PATH;
    public static boolean STEMMING = false;
    public static boolean SEMANTIC_TREAT = false;
    public static boolean ONLINE_SEMANTIC = false;

    /**
     * loading all the configurations from the config.properties to the fields of the class
     */
    public static void loadConfiguration(){
        try{
            Properties properties = new Properties();
            FileInputStream inputStream = new FileInputStream(CONFIG_PATH);
            properties.load(inputStream);

            WORKER_NUM = Integer.parseInt(properties.getProperty("WORKER_NUM"));
            WORKER_BATCH_NUM = Integer.parseInt(properties.getProperty("WORKER_BATCH_NUM"));

            inputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * updates the CORPUS_DIR_PATH field
     * @param newCorpusPath updated corpus path
     */
    public static void updateCorpusPath(String newCorpusPath){
        CORPUS_DIR_PATH = newCorpusPath;

        // stop words file path
        STOP_WORDS_FILE_PATH = CORPUS_DIR_PATH + "\\05 stop_words.txt";
    }

    /**
     * updates the POSTING_DIR_PATH field
     * @param newPostingPath updated posting path
     */
    public static void updatePostingPath(String newPostingPath){
        POSTING_DIR_PATH = newPostingPath;

        // temp directory for batch pre-posting files
        BATCH_PRE_POSTING_DIR_PATH = POSTING_DIR_PATH +"\\temp";

        updatePostingDependencies();
    }

    public static void updateQueriesPath(String newQueriesPath){
        QUERIES_FILE_PATH = newQueriesPath;
    }

    /**
     * updates the STEMMING field
     * @param stemming updated stemming option (true-with/false-without)
     */
    public static void setStemming(boolean stemming){
        STEMMING = stemming;
        updatePostingDependencies();
    }

    /**
     * updates the SEMANTIC_TREAT field
     * @param semantic updated semantic treat option (true-with/false-without)
     */
    public static void setSemantic(boolean semantic){
        SEMANTIC_TREAT = semantic;
    }

    /**
     * updates the ONLINE_SEMANTIC field
     * @param online updated online semantic treat option (true-online/false-offline)
     */
    public static void setOnlineSemantic(boolean online){
        ONLINE_SEMANTIC = online;
    }

    /**
     * updates all the dependencies paths derived from POSTING_DIR_PATH and CORPUS_DIR_PATH
     */
    private static void updatePostingDependencies(){
        if (STEMMING){
            // final posting file path
            FINAL_POSTING_FILE_PATH = POSTING_DIR_PATH + "\\stemming_posting.txt";
            // inverted dictionary file path
            INVERTED_DICTIONARY_FILE_PATH = POSTING_DIR_PATH + "\\stemming_dictionary.txt";
            // document posting path
            DOCUMENT_POSTING_PATH = POSTING_DIR_PATH + "\\stemming_";
        }
        else{
            // final posting file path
            FINAL_POSTING_FILE_PATH = POSTING_DIR_PATH + "\\no_stemming_posting.txt";
            // inverted dictionary file path
            INVERTED_DICTIONARY_FILE_PATH = POSTING_DIR_PATH + "\\no_stemming_dictionary.txt";
            // document posting path
            DOCUMENT_POSTING_PATH = POSTING_DIR_PATH + "\\no_stemming_";
        }
    }
}
