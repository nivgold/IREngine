package Model.dataTypes;

/**
 * data structure that was created to hold the data of the given query
 */
public class Query {
    private String queryID;
    private String queryText;

    public Query(String queryID, String queryText) {
        this.queryID = queryID;
        this.queryText = queryText;
    }

    /**
     * return's the query ID
     * @return the query ID
     */
    public String getQueryID() {
        return queryID;
    }

    /**
     * return's the query text
     * @return the query text
     */
    public String getQueryText() {
        return queryText;
    }
}
