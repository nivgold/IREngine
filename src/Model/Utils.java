package Model;

import Model.communicator.ConfigReader;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class Utils {
    private static final String STOP_WORDS_PATH = ConfigReader.STOP_WORDS_FILE_PATH;

    /**
     * loading the 'stop_words' file from the disk
     */
    public static Set<String> loadStopWords(){
        Set<String> stopWords = new HashSet<>();
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(STOP_WORDS_PATH));
            String currentLine;
            while ((currentLine = bufferedReader.readLine()) != null) {
                stopWords.add(currentLine);
            }
            bufferedReader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stopWords;
    }
}
