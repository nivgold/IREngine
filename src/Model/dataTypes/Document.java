package Model.dataTypes;

import java.util.*;

/**
 * data structure that was created in order to save all document data from the corpus
 */
public class Document{
    private String docNo;
    private String text;
    private int textLength;
    private HashMap<String, Integer> termsInDoc;
    private int termsAmount;

    public Document(String docNO, String text) {
        this.docNo = docNO.replace(" ", "");
        this.text = text;
        this.textLength = text.length();
        this.termsInDoc=new HashMap<>();
        this.termsAmount = 0;
    }

    /**
     * updates the document tf "counter" of each term he contains
     * @param term term instance in the current document
     */
    public void addTerm(String term){
        if(termsInDoc.containsKey(term)){
            termsInDoc.put(term, termsInDoc.get(term)+1);
        }
        else{
            termsInDoc.put(term, 1);
        }
        this.termsAmount+=1;
    }

    /**
     * returns the number of unique terms of the current document
     * @return number of unique terms
     */
    public int uniqueTerms(){
        return termsInDoc.size();
    }

    /**
     * returns the highest Tf associated with term within the document
     * @return the highest tf that appeared in the document
     */
    public int getMaxTF(){
        int maxTF = -1;
        for (Integer integer : termsInDoc.values()){
            if (integer > maxTF)
                maxTF = integer;
        }
        return maxTF;
    }

    /**
     * returns the document text
     * @return the document text as String
     */
    public String getText() {
        return text;
    }

    /**
     * returns the length of the document text
     * @return document length
     */
    public int getTextLength() {
        return textLength;
    }

    /**
     * returns the document's DOCNO
     * @return document DOCNO
     */
    public String getDocNo() {
        return docNo;
    }

    @Override
    public String toString() {
        return this.docNo+";"+getMaxTF()+";"+uniqueTerms()+";"+this.termsAmount;
    }
}
