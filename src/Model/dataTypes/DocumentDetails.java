package Model.dataTypes;

public class DocumentDetails {
    private String docNo;
    private int maxTF;
    private int uniqueTerms;
    private int docLength;

    public DocumentDetails(String docNo, int maxTF, int uniqueTerms, int docLength) {
        this.docNo = docNo;
        this.maxTF = maxTF;
        this.uniqueTerms = uniqueTerms;
        this.docLength = docLength;
    }

    public String getDocNo() {
        return docNo;
    }

    public int getMaxTF() {
        return maxTF;
    }

    public int getUniqueTerms() {
        return uniqueTerms;
    }

    public int getDocLength() {
        return docLength;
    }


}