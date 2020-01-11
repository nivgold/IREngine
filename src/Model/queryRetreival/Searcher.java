package Model.queryRetreival;

import Model.Utils;
import Model.communicator.ConfigReader;
import Model.dataTypes.AllTermDocs;
import Model.dataTypes.DocumentDetails;
import Model.dataTypes.Query;
import Model.dataTypes.TermDetails;
import Model.preproccesing.Parse;
import com.medallia.word2vec.Word2VecModel;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class Searcher {
    /**
     * search for the most 50 relevant documents for the given query
     * @param query represent the given query
     * @param dictionary the inverted index dictionary
     * @param documentDictionary the document dictionary
     * @return List of <Document , Similarity>
     */
    public List<Map.Entry<String, Double>> search(Query query, Map<String, TermDetails> dictionary, Map<String, DocumentDetails> documentDictionary){
        Parse parse = new Parse(Utils.loadStopWords());
        Ranker ranker = new Ranker();
        parse.parseQuery(query);
        ArrayList<Map.Entry<String, AllTermDocs>> queryTerms = parse.getTermDocsMap();

        // semantic treat
        if (ConfigReader.SEMANTIC_TREAT){
            if (ConfigReader.ONLINE_SEMANTIC)
                onlineSemanticTreat(queryTerms, query);
            else
                offlineSemanticTreat(queryTerms, query);
            Collections.sort(queryTerms, new Comparator<Map.Entry<String, AllTermDocs>>() {
                @Override
                public int compare(Map.Entry<String, AllTermDocs> o1, Map.Entry<String, AllTermDocs> o2) {
                    return o1.getKey().toLowerCase().compareTo(o2.getKey().toLowerCase());
                }
            });
        }

        List<Map.Entry<String, Double>> retrievedDocuments = ranker.rank(queryTerms, dictionary, documentDictionary);
        List<Map.Entry<String, Double>> relevantDocuments = new ArrayList<>();

        for (int i=0; i<Math.min(50, retrievedDocuments.size()); i++){
            relevantDocuments.add(retrievedDocuments.get(i));
        }
        return relevantDocuments;
    }

    /**
     * adding related semantic words to the query parsed terms with an online service
     * we used the 'data-muse' API
     * @param queryTerms represent the query parsed terms
     * @param query represent the given query
     */
    private void onlineSemanticTreat(ArrayList<Map.Entry<String, AllTermDocs>> queryTerms, Query query) {
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
                        if (word.equals(term))
                            continue;
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

    /**
     * adding related semantic words to the query parsed terms with an offline service
     * @param queryTerms represent the query parsed terms
     * @param query represent the given query
     */
    private void offlineSemanticTreat(ArrayList<Map.Entry<String, AllTermDocs>> queryTerms, Query query){
        Map<String, AllTermDocs> addedWords = new HashMap<>();
        for (Map.Entry<String, AllTermDocs> entry : queryTerms){
            String term = entry.getKey();
            try {
                Word2VecModel model = Word2VecModel.fromTextFile(new File("resources\\word2vec.c.output.model.txt"));
                com.medallia.word2vec.Searcher semanticSearcher = model.forSearch();

                int amountOfResults = 11;
                List<com.medallia.word2vec.Searcher.Match> matches = semanticSearcher.getMatches(term, amountOfResults);

                for(com.medallia.word2vec.Searcher.Match match : matches){
                    String word = match.match();
                    if (word.equals(term))
                        continue;
                    if (word.contains(" "))
                        word = word.toUpperCase()+" ";
                    if (addedWords.containsKey(word)){
                        addedWords.get(word).addTermDetails(query.getQueryID());
                    }
                    else
                        addedWords.put(word, new AllTermDocs(query.getQueryID()));
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (com.medallia.word2vec.Searcher.UnknownWordException e){
                System.out.println("Didnt find the word");
            }
        }

        // adding the addedWord to the queryTerms
        for (Map.Entry<String, AllTermDocs> entry : addedWords.entrySet()){
            queryTerms.add(entry);
        }
    }

}