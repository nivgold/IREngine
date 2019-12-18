package Model.dataTypes;

import java.util.*;

public class AllTermDocs {
    //private HashMap<String, TermInDocDetails> mapper;
    private int totalTF;
    private HashMap<String, Integer> mapper;

    public AllTermDocs(String docNO) {
        //this.mapper = new HashMap<>();
        this.mapper = new HashMap<>();
        this.totalTF=1;
        mapper.put(docNO.replace(" ", ""), 1);
    }
    public void addTermDetails(String docNo){
        docNo = docNo.replace(" ", "");
        this.totalTF++;
        if (mapper.containsKey(docNo))
            mapper.put(docNo, mapper.get(docNo) +1);
        else
            mapper.put(docNo.replace(" ", ""), 1);
    }

    public ArrayList<Map.Entry<String, Integer>> getSortedDocs(){
        Set entrySet = this.mapper.entrySet();
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
        return mapper.size();
    }

    public int getTotalTF() {
        return totalTF;
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
