package Model.dataTypes;

import java.util.*;

// data structure used in Parser to accumulate the terms and theirs data
public class AllTermDocs {
    private int termTFInBatch;
    private HashMap<String, Integer> docsTFMap;

    public AllTermDocs(String docNO) {
        this.docsTFMap = new HashMap<>();
        this.termTFInBatch =1;
        docsTFMap.put(docNO.replace(" ", ""), 1);
    }
    public void addTermDetails(String docNo){
        docNo = docNo.replace(" ", "");
        this.termTFInBatch++;
        if (docsTFMap.containsKey(docNo))
            docsTFMap.put(docNo, docsTFMap.get(docNo) +1);
        else
            docsTFMap.put(docNo.replace(" ", ""), 1);
    }

    public ArrayList<Map.Entry<String, Integer>> getSortedDocs(){
        Set entrySet = this.docsTFMap.entrySet();
        ArrayList<Map.Entry<String, Integer>> sortedDocs = new ArrayList<>(entrySet);
        Collections.sort(sortedDocs, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                return o1.getKey().compareTo(o2.getKey());
            }
        });
        return sortedDocs;
    }

    public int getDF(){
        return docsTFMap.size();
    }

    public int getTermTFInBatch() {
        return termTFInBatch;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        ArrayList<Map.Entry<String, Integer>> sortedDocs = getSortedDocs();
        for (Map.Entry<String, Integer> entry : sortedDocs)
            stringBuilder.append(entry.getKey()+":"+entry.getValue()+";");
        return stringBuilder.toString();
    }
}
