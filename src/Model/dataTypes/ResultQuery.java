package Model.dataTypes;

/**
 * data structure that was build in order to support the tableView view in the GUI for the query result
 */
public class ResultQuery {
    private String queryID;
    private String docNO;
    private String similarity;
    private String dominantEntities;

    public ResultQuery(String queryID, String docNO, String similarity, String dominantEntities) {
        this.queryID = queryID;
        this.docNO = docNO;
        this.similarity = similarity;
        this.dominantEntities = dominantEntities;
    }

    /**
     * return's the query ID
     * @return the query ID
     */
    public String getQueryID() {
        return queryID;
    }

    /**
     * return's the document ID (docNO)
     * @return the document ID (docNO)
     */
    public String getDocNO() {
        return docNO;
    }

    /**
     * return's the similarity of the document to the given query
     * @return the similarity of the document to the given query
     */
    public String getSimilarity() {
        return similarity;
    }

    /**
     * return's the document's dominant entities
     * @return String that represent the document's dominant entities
     */
    public String getDominantEntities() {
        return dominantEntities;
    }
}
