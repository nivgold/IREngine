package Model.dataTypes;

public class Term {
    private String value;
    private String corpusTF;

    public Term(String value, String corpusTF) {
        this.value = value;
        this.corpusTF = corpusTF;
    }

    public String getValue() {
        return value;
    }

    public String getCorpusTF() {
        return corpusTF;
    }
}
