package Model.dataTypes;

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

    public String getQueryID() {
        return queryID;
    }

    public String getDocNO() {
        return docNO;
    }

    public String getSimilarity() {
        return similarity;
    }

    public String getDominantEntities() {
        return dominantEntities;
    }
}
