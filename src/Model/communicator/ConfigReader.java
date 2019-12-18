package Model.communicator;

import java.io.*;
import java.util.Properties;

public class ConfigReader {
    public static int WORKER_NUM;
    public static int WORKER_BATCH_NUM;
    public static String CORPUS_PATH;
    public static String STOP_WORDS_PATH;
    private final static String CONFIG_PATH = "resources/config.properties";
    public static String BATCH_POSTING_FILE_PATH;
    public static String FINAL_POSTING_FILE_PATH;
    public static String INVERTED_DICTIONARY_PATH;
    public static String FINAL_POSTING_DIRECTORY_PATH;

    public static void loadConfiguration(){
        try{
            Properties properties = new Properties();
            InputStream inputStream = ConfigReader.class.getClassLoader().getResourceAsStream(CONFIG_PATH);
            if (inputStream != null){
                properties.load(inputStream);
            }
            else{
                throw new FileNotFoundException("property file "+CONFIG_PATH+" not found");
            }

            WORKER_NUM = Integer.parseInt(properties.getProperty("WORKER_NUM"));
            WORKER_BATCH_NUM = Integer.parseInt(properties.getProperty("WORKER_BATCH_NUM"));
            CORPUS_PATH = properties.getProperty("CORPUS_PATH");
            STOP_WORDS_PATH = properties.getProperty("STOP_WORDS_PATH");
            BATCH_POSTING_FILE_PATH = properties.getProperty("BATCH_POSTING_FILE_PATH");
            FINAL_POSTING_FILE_PATH = properties.getProperty("FINAL_POSTING_FILE_PATH");
            INVERTED_DICTIONARY_PATH = properties.getProperty("INVERTED_DICTIONARY_PATH");
            FINAL_POSTING_DIRECTORY_PATH = properties.getProperty("FINAL_POSTING_DIRECTORY_PATH");

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void updateCorpusPath(String newCorpusPath){
        try{
            Properties properties = new Properties();
            FileInputStream inputStream = new FileInputStream(CONFIG_PATH);
            properties.load(inputStream);
            inputStream.close();

            FileOutputStream outputStream = new FileOutputStream(CONFIG_PATH);
            properties.setProperty("CORPUS_PATH", newCorpusPath);
            properties.store(outputStream, null);
            outputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void updatePostingPath(String newPostingPath){
        try{
            Properties properties = new Properties();
            FileInputStream inputStream = new FileInputStream(CONFIG_PATH);
            properties.load(inputStream);
            inputStream.close();

            FileOutputStream outputStream = new FileOutputStream(CONFIG_PATH);
            properties.setProperty("POSTING_PATH", newPostingPath);
            properties.store(outputStream, null);
            outputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
