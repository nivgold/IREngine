package Model.preproccesing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

// reading the corpus files to the RAM
public class ReadFile{
    public static AtomicInteger counter = new AtomicInteger(0);
    private String path;
    private int startOfDocs;
    private int numOfDocs;
    private Set<String> result;

    public ReadFile(String path, int startOfDocs, int numOfDocs) {
        this.path = path;
        this.startOfDocs = startOfDocs;
        this.numOfDocs = numOfDocs;
        this.result = new HashSet<>();
    }

    public void read(){
        path += "\\corpus";
        File mainDirectory = new File(path);
        File[] allFiles = mainDirectory.listFiles();
        for (int i=startOfDocs; i<startOfDocs+numOfDocs; i++){
            File currentDir = allFiles[i];
            String fileName = path + "\\" + currentDir.getName() + "\\" + currentDir.getName();
            readCurrentDir(fileName);
        }
    }

    private void readCurrentDir(String fileName) {
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
                        System.out.println(stringBuilder.toString());
                        result.add(stringBuilder.toString() + " ");
                    }
                    else{
                        stringBuilder.append(line+" ");
                    }
                }
            }
            fileReader.close();
        }catch (IOException e) {
            e.printStackTrace();
        }
        counter.getAndAdd(1);
    }

    public Set<String> getResult() {
        return result;
    }
}
