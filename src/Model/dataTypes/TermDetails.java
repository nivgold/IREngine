package Model.dataTypes;

public class TermDetails {
    private int corpusTF;
    private int df;
    private int postingPointer;

    public TermDetails(int df, int tf) {
        this.df = df;
        this.corpusTF = tf;
    }

    public void addDF(int df){
        this.df +=df;
    }

    public void addTF(int tf){
        this.corpusTF += tf;
    }

    public void setCorpusTF(int corpusTF) {
        this.corpusTF = corpusTF;
    }

    public void setDf(int df) {
        this.df = df;
    }

    public void setPostingPointer(int postingPointer) {
        this.postingPointer = postingPointer;
    }

    public int getCorpusTF() {
        return corpusTF;
    }

    public int getDf() {
        return df;
    }

    public int getPostingPointer() {
        return postingPointer;
    }

    @Override
    public String toString() {
        return this.corpusTF+";"+this.df+";"+this.postingPointer;
    }
}