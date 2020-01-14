package Model.dataTypes;

/**
 * data structure that was created in order to hold the document details after loading the data from the document postings
 */
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

    /**
     * return's the document ID
     * @return the document ID
     */
    public String getDocNo() {
        return docNo;
    }

    /**
     * return's the the max TF of a term that is in the document
     * @return max Tf term that is contained in the document
     */
    public int getMaxTF() {
        return maxTF;
    }

    /**
     * return's the number of unique terms in the documents
     * @return the number of unique terms in the document
     */
    public int getUniqueTerms() {
        return uniqueTerms;
    }

    /**
     * return's the length of the document (by words)
     * @return the length of the document (by words)
     */
    public int getDocLength() {
        return docLength;
    }
}