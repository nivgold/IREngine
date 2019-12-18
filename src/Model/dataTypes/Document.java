package Model.dataTypes;

import java.util.HashMap;

public class Document{
    private String docNo;
    private String text;
    private int textLength;
    private HashMap<String, Integer> termsInDoc;

    public Document(String docNO, String text) {
        this.docNo = docNO;
        this.text = text;
        this.textLength = text.length();
        this.termsInDoc=new HashMap<>();
    }

    public void addTerm(String term){
        if(termsInDoc.containsKey(term)){
            termsInDoc.put(term, termsInDoc.get(term)+1);
        }
        else{
            termsInDoc.put(term, 1);
        }
    }

    public int uniqueTerms(){
        return termsInDoc.size();
    }

    public int getMaxTF(){
        int maxTF = -1;
        for (Integer integer : termsInDoc.values()){
            if (integer > maxTF)
                maxTF = integer;
        }
        return maxTF;
    }

    public String getText() {
        return text;
    }

    public int getTextLength() {
        return textLength;
    }

    public String getDocNo() {
        return docNo;
    }

    @Override
    public String toString() {
        return termsInDoc.size()+";"+getMaxTF();
    }
}
