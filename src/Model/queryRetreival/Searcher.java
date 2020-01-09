package Model.queryRetreival;

import Model.Utils;
import Model.communicator.ConfigReader;
import Model.dataTypes.AllTermDocs;
import Model.dataTypes.DocumentDetails;
import Model.dataTypes.Query;
import Model.dataTypes.TermDetails;
import Model.preproccesing.Parse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class Searcher {
    public List<Map.Entry<String, Double>> search(Query query, Map<String, TermDetails> dictionary, Map<String, DocumentDetails> documentDictionary){
        Parse parse = new Parse(Utils.loadStopWords());
        Ranker ranker = new Ranker();
        parse.parseQuery(query);
        ArrayList<Map.Entry<String, AllTermDocs>> queryTerms = parse.getTermDocsMap();

        // semantic treat
        if (ConfigReader.SEMANTIC_TREAT){
            semanticTreat(queryTerms, query, dictionary);
        }
        List<Map.Entry<String, Double>> retrievedDocuments = ranker.rank(queryTerms, dictionary, documentDictionary);
        List<Map.Entry<String, Double>> relevantDocuments = new ArrayList<>();

        for (int i=0; i<Math.min(50, retrievedDocuments.size()); i++){
            relevantDocuments.add(retrievedDocuments.get(i));
        }
        return relevantDocuments;
    }

    private void semanticTreat(ArrayList<Map.Entry<String, AllTermDocs>> queryTerms, Query query, Map<String, TermDetails> dictionary) {
        Map<String, AllTermDocs> addedWords = new HashMap<>();
        for (Map.Entry<String, AllTermDocs> entry : queryTerms){
            String term = entry.getKey();
            try {
                term = term.replaceAll(" ", "+");
                String urlString = "https://api.datamuse.com/words?ml=" + term;
                URL url = new URL(urlString);
                HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.setRequestProperty("User-Agent", "Mozilla/5.0");
                int responseCode = httpURLConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK){
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
                    String currentLine;
                    StringBuilder stringBuilder = new StringBuilder();
                    while ((currentLine=bufferedReader.readLine())!=null){
                        stringBuilder.append(currentLine);
                    }
                    bufferedReader.close();

                    JSONArray jsonArray = new JSONArray(stringBuilder.toString());
                    for (int i=0; i<Math.min(10, jsonArray.length()); i++){
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        String word = jsonObject.getString("word");
                        if (word.contains(" "))
                            word = word.toUpperCase()+" ";
                        if (addedWords.containsKey(word)){
                            addedWords.get(word).addTermDetails(query.getQueryID());
                        }
                        else
                            addedWords.put(word, new AllTermDocs(query.getQueryID()));
                    }
                }
            } catch (IOException e) {
                System.out.println("HTTP GET message didnt work on term:"+term);
            } catch (JSONException e) {
                System.out.println("Invalid JSON from term:"+term);
            }
        }

        // adding the addedWord to the queryTerms
        for (Map.Entry<String, AllTermDocs> entry : addedWords.entrySet()){
            queryTerms.add(entry);
        }
    }

}