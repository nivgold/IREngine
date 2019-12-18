package Model.dataTypes;

// data structure that holds the data in the inverted index dictionary
public class TermDetails {
    private int corpusTF;
    private int df;
    private String postingPointer;

    public TermDetails(int df, int tf) {
        this.df = df;
        this.corpusTF = tf;
    }

    public TermDetails(int corpusTF, int df, String postingPointer) {
        this.corpusTF = corpusTF;
        this.df = df;
        this.postingPointer = postingPointer;
    }

    public void addDF(int df){
        this.df +=df;
    }

    public void addTF(int tf){
        this.corpusTF += tf;
    }

    public void setPostingPointer(String postingPointer) {
        this.postingPointer = postingPointer;
    }

    public int getCorpusTF() {
        return corpusTF;
    }

    public int getDF() {
        return df;
    }

    public String getPostingPointer() {
        return postingPointer;
    }

    @Override
    public String toString() {
        return this.corpusTF+";"+this.df+";"+this.postingPointer;
    }
}