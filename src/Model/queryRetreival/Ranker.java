package Model.queryRetreival;

import Model.communicator.ConfigReader;
import Model.dataTypes.AllTermDocs;
import Model.dataTypes.DocumentDetails;
import Model.dataTypes.TermDetails;
import Model.preproccesing.Parse;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Ranker {
    private final double k = 100;
    private final double b = 0.5;
    private Map<String, Double> innerProductSimilarityMap;
    private Map<String, Double> BM25SimilarityMap;

    public Ranker() {
        innerProductSimilarityMap = new HashMap<>();
        BM25SimilarityMap = new HashMap<>();
    }

    /**
     * returns a sorted list of relevant document descending order by their cosine similarity value
     * @param queryTerms parsed terms of the given query
     * @param dictionary inverted index dictionary
     * @param documentDictionary document dictionary
     * @return order list of relevant document descending order by their cosine similarity value
     */
    public List<Map.Entry<String, Double>> rank(List<Map.Entry<String, AllTermDocs>> queryTerms, Map<String, TermDetails> dictionary, Map<String, DocumentDetails> documentDictionary){
        int queryLength = queryTerms.size();
        int maxQueryTF = findMaxQueryTF(queryTerms);
        BufferedReader bufferedReader = openPositingFile();
        int currentLineIndex = 0;
        try {
            for (Map.Entry<String, AllTermDocs> entry : queryTerms) {
                String term = entry.getKey();
                TermDetails termDetails = dictionary.get(term);
                if (termDetails == null)
                    //name doesnt exist in the dictionary
                    continue;

                AllTermDocs allTermDocs = entry.getValue();
                int termQueryTF = allTermDocs.getTermTFInBatch();
                double normalizedQueryTF = ((double)termQueryTF/maxQueryTF);
                double df = termDetails.getDF();
                double idf = Math.log10(Parse.docCounter.get()/df);
                int postingPointer = Integer.parseInt(termDetails.getPostingPointer());
                String postingValue = "";
                postingValue = bufferedReader.readLine();
                while (postingPointer != currentLineIndex){
                    currentLineIndex++;
                    postingValue = bufferedReader.readLine();
                }
                currentLineIndex++;
                // update the similarity map

                updateSimilarityMap(postingValue, idf, documentDictionary, normalizedQueryTF, df);
            }
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // normalize the inner product similarity value
        normalizeInnerProductSimilarity(documentDictionary, queryLength);

        Map<String, Double> similarityMap = new HashMap<>();
        for (String docNo : BM25SimilarityMap.keySet()){
            double combinedSimilarity = 0.5*BM25SimilarityMap.get(docNo)+0.5*innerProductSimilarityMap.get(docNo);
            similarityMap.put(docNo, combinedSimilarity);
        }

        // return sorted documents by their similarity value
        Set entrySet = similarityMap.entrySet();
        List<Map.Entry<String, Double>> sortedDocuments = new ArrayList<>(entrySet);
        Collections.sort(sortedDocuments, new Comparator<Map.Entry<String, Double>>() {
            @Override
            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                return o2.getValue().compareTo(o1.getValue());
            }
        });

        return sortedDocuments;
    }

    private void normalizeInnerProductSimilarity(Map<String, DocumentDetails> documentDictionary, int queryLength) {
        for (Map.Entry<String, Double> entry : innerProductSimilarityMap.entrySet()){
            String docNo = entry.getKey();
            int maxTF = documentDictionary.get(docNo).getMaxTF();
            double currentSimilarity = innerProductSimilarityMap.get(docNo);
            innerProductSimilarityMap.put(docNo, currentSimilarity/(maxTF*queryLength));
        }
    }

    private int findMaxQueryTF(List<Map.Entry<String, AllTermDocs>> queryTerms) {
        int maxTF = -1;
        for (Map.Entry<String, AllTermDocs> entry : queryTerms){
            if (entry.getValue().getTermTFInBatch()>maxTF)
                maxTF = entry.getValue().getTermTFInBatch();
        }
        return maxTF;
    }

    private BufferedReader openPositingFile(){
        String FINAL_POSTING_FILE_PATH = ConfigReader.FINAL_POSTING_FILE_PATH;
        BufferedReader bufferedReader = null;
        try{
            bufferedReader = new BufferedReader(new FileReader(FINAL_POSTING_FILE_PATH));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return bufferedReader;
    }

    private void updateSimilarityMap(String postingValue, double idf, Map<String, DocumentDetails> documentDictionary, double normalizedQueryTF, double df){
        String[] termDocuments = postingValue.split(";");
        for (String document : termDocuments){
            String docNo = document.substring(0, document.indexOf(':'));
            int docLength = documentDictionary.get(docNo).getDocLength();
            double termDocumentTF = Double.parseDouble(document.substring(document.indexOf(':')+1));
            double documentMaxTF = documentDictionary.get(docNo).getMaxTF();

            double queryWeight = normalizedQueryTF * idf;
            double documentWeight = ((termDocumentTF / documentMaxTF) * idf);
            double innerProductValue = documentWeight*queryWeight;

            // update the inner product similarity
            if (innerProductSimilarityMap.containsKey(docNo))
                innerProductSimilarityMap.put(docNo, innerProductSimilarityMap.get(docNo) + innerProductValue);
            else
                innerProductSimilarityMap.put(docNo, innerProductValue);

            // update the BM25 similarity
            double BM25Value = queryWeight*(((k+1)*documentWeight)/(documentWeight + k*(1-b+b*(docLength/Parse.getAVDL()))))*Math.log10((Parse.docCounter.get()+1)/df);
            if (BM25SimilarityMap.containsKey(docNo)){
                BM25SimilarityMap.put(docNo, BM25SimilarityMap.get(docNo) + BM25Value);
            }
            else
                BM25SimilarityMap.put(docNo, BM25Value);
        }
    }
}