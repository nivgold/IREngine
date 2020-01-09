package Model.dataTypes;

public class Query {
    private String queryID;
    private String queryText;

    public Query(String queryID, String queryText) {
        this.queryID = queryID;
        this.queryText = queryText;
    }

    public String getQueryID() {
        return queryID;
    }

    public String getQueryText() {
        return queryText;
    }
}
