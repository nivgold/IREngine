package Model.dataTypes;

/**
 * data structure that was build in order to support the tableView view in the GUI
 */
public class Term {
    private String value;
    private String corpusTF;

    public Term(String value, String corpusTF) {
        this.value = value;
        this.corpusTF = corpusTF;
    }

    /**
     * returns the term's value
     * @return term's value
     */
    public String getValue() {
        return value;
    }

    /**
     * returns the TF of the term in the whole corpus
     * @return term's corpus TF
     */
    public String getCorpusTF() {
        return corpusTF;
    }
}
