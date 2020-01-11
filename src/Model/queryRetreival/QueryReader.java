package Model.queryRetreival;

import Model.dataTypes.Query;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class QueryReader {
    private HashSet<Integer> queryIDs;
    private int availableQueryID;
    public QueryReader() {
        queryIDs = new HashSet<>();
        availableQueryID = 0;
    }

    public Set<Query> extractQueriesFromPath(String path){
        Set<Query> queries = new HashSet<>();
        try {
            FileReader fileReader = new FileReader(path);
            String queryID="";
            String queryText="";
            try(BufferedReader bufferedReader = new BufferedReader(fileReader)){
                String line;
                while ((line = bufferedReader.readLine())!=null){
                    if (line.contains("<top>")){
                        queryID="";
                        queryText="";
                    }
                    else if(line.contains("</top>")){
                        availableQueryID = Integer.parseInt(queryID);
                        queries.add(new Query(queryID, queryText+" "));
                        queryIDs.add(Integer.parseInt(queryID));
                    }
                    else{
                        if (line.startsWith("<num>")){
                            int delimiterIndex = line.indexOf(':');
                            queryID = line.substring(delimiterIndex+1);
                            queryID = queryID.replaceAll(" ","");
                        }
                        else if (line.startsWith("<title>")){
                            queryText = line.substring(7);
                        }
                        else if (line.startsWith("<desc>")){
                            while (!(line = bufferedReader.readLine()).startsWith("<narr>")) {
                                queryText += " " + line;
                            }
                        }
                    }
                }
            }
            fileReader.close();
        }catch (IOException e) {
            e.printStackTrace();
        }
        return queries;
    }

    public Query makeQuery(String text){
        while (queryIDs.contains(availableQueryID))
            availableQueryID++;
        text = " "+text+" ";
        return new Query(availableQueryID+"", text);
    }
}