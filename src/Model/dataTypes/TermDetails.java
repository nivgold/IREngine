package Model.dataTypes;

// data structure that holds the data in the inverted index dictionary

/**
 * data structure that holds the data in the inverted index dictionary
 */
public class TermDetails {
    private int corpusTF;
    private int df;
    private String postingPointer;

    public TermDetails(int df, int tf) {
        this.df = df;
        this.corpusTF = tf;
    }

    /**
     * builds a new term with corpusTf, df and postingPointer
     * @param corpusTF number of the term's instances in the whole corpus
     * @param df number of documents containing the term
     * @param postingPointer pointer (line number) to the posting line in the posting file of the term
     */
    public TermDetails(int corpusTF, int df, String postingPointer) {
        this.corpusTF = corpusTF;
        this.df = df;
        this.postingPointer = postingPointer;
    }

    /**
     * updates the accumulated df of the term
     * @param df number of new documents containing the term
     */
    public void addDF(int df){
        this.df +=df;
    }

    /**
     * updates the accumulated tf of the term
     * @param tf number of new occurrences of the term in the corpus
     */
    public void addTF(int tf){
        this.corpusTF += tf;
    }

    /**
     * updates the term's posting pointer
     * @param postingPointer line number of the term posting value
     */
    public void setPostingPointer(String postingPointer) {
        this.postingPointer = postingPointer;
    }

    /**
     * returns the term's whole corpus TF
     * @return term's whole corpus TF
     */
    public int getCorpusTF() {
        return corpusTF;
    }

    /**
     * returns the term's df
     * @return term's df
     */
    public int getDF() {
        return df;
    }

    /**
     * returns the term's pointer in the posting file
     * @return term's pointer int the posting file
     */
    public String getPostingPointer() {
        return postingPointer;
    }

    @Override
    public String toString() {
        return this.corpusTF+";"+this.df+";"+this.postingPointer;
    }
}