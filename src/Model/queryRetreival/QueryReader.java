package Model.queryRetreival;

import Model.dataTypes.Query;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * this class represent a way to read a query from path and from free text
 */
public class QueryReader {
    private static HashSet<Integer> queryIDs = new HashSet<>();
    private static int availableQueryID = 0;

    /**
     * reading a queries path and returns the query in a correct form
     * @param path the path to the queries file
     * @return Set of Query
     */
    public Set<Query> extractQueriesFromPath(String path){
        Set<Query> queries = new HashSet<>();
        try {
            FileReader fileReader = new FileReader(path);
            String queryID="";
            StringBuilder queryText = new StringBuilder();
            try(BufferedReader bufferedReader = new BufferedReader(fileReader)){
                String line;
                while ((line = bufferedReader.readLine())!=null){
                    if (line.contains("<top>")){
                        queryID="";
                        queryText = new StringBuilder();
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
                            line = line.replace("Chunnel", "Channel");
                            queryText.append(line.substring(7));
                            while (!(line=bufferedReader.readLine()).equals("</top>")){
                                if (line.startsWith("<desc>") || line.startsWith("<narr>"))
                                    continue;

                                if (line.contains("non-relevant") || line.contains("not relevant"))
                                    continue;

                                line = line.replace("Chunnel", "Channel");
                                line = line.replaceAll("document|Document|relevant|Relevant|discussing", "");
                                queryText.append(" "+line);
                            }
                            availableQueryID = Integer.parseInt(queryID);
                            queries.add(new Query(queryID, queryText.toString()+" "));
                            queryIDs.add(Integer.parseInt(queryID));
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

    /**
     * converting the free text to ta Query class
     * @param text query free text
     * @return Query that represent the given query free text
     */
    public Query makeQuery(String text){
        while (queryIDs.contains(availableQueryID))
            availableQueryID++;
        queryIDs.add(availableQueryID);
        text = " "+text+" ";
        return new Query(availableQueryID+"", text);
    }
}