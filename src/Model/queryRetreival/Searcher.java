package Model.queryRetreival;

import Model.Utils;
import Model.communicator.ConfigReader;
import Model.dataTypes.AllTermDocs;
import Model.dataTypes.DocumentDetails;
import Model.dataTypes.Query;
import Model.dataTypes.TermDetails;
import Model.preproccesing.Parse;
import Model.preproccesing.Stemmer;
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

/**
 * this class represent a way to retrieve the 50 most relevant documents for a given query
 */
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

        List<Map.Entry<String, Double>> retrievedDocuments = ranker.rank(queryTerms, dictionary, documentDictionary, query.getQueryID());
        List<Map.Entry<String, Double>> relevantDocuments = new ArrayList<>();

        for (int i=0; i<Math.min(50, retrievedDocuments.size()); i++){
            relevantDocuments.add(retrievedDocuments.get(i));
        }
        return relevantDocuments;
    }


}