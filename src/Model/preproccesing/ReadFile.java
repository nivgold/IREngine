package Model.preproccesing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class ReadFile implements Runnable{
    public static AtomicInteger counter = new AtomicInteger(0);
    private String path;
    private int startOfDocs;
    private int numOfDocs;
    private Set<String> result;
    public double total;


    public ReadFile(String path, int startOfDocs, int numOfDocs) {
        this.path = path;
        this.startOfDocs = startOfDocs;
        this.numOfDocs = numOfDocs;
        this.result = new HashSet<>();
    }

    @Override
    public void run() {
        double start = System.nanoTime();
        readFromPath(path, startOfDocs, numOfDocs, result);
        double end = System.nanoTime();
        total = (end - start)/1000000000.0;
    }

    public void readFromPath(String path, int startOfDocs, int numOfDocs, Set<String> result){
        path += "\\corpus";
        File mainDirectory = new File(path);
        File[] allFiles = mainDirectory.listFiles();
        for (int i=startOfDocs; i<startOfDocs+numOfDocs; i++){
            File currentDir = allFiles[i];
            String fileName = path + "\\" + currentDir.getName() + "\\" + currentDir.getName();
            readCurrentDir(fileName, result);
        }
    }

    private void readCurrentDir(String fileName, Set<String> result) {
        try {
            StringBuilder stringBuilder = new StringBuilder();
            FileReader fileReader = new FileReader(fileName);
            try(BufferedReader bufferedReader = new BufferedReader(fileReader)){
                String line;
                while ((line = bufferedReader.readLine())!=null){
                    if (line.contains("<DOC>")){
                        stringBuilder = new StringBuilder();
                    }
                    else if(line.contains("</DOC>")){
                        result.add(stringBuilder.toString() + " ");
                    }
                    else{
                        stringBuilder.append(line);
                    }
                }
            }
            fileReader.close();
        }catch (IOException e) {
            e.printStackTrace();
        }
        counter.getAndAdd(1);
    }

    public String getPath() {
        return path;
    }

    public int getStartOfDocs() {
        return startOfDocs;
    }

    public int getNumOfDocs() {
        return numOfDocs;
    }

    public Set<String> getResult() {
        return result;
    }
}
