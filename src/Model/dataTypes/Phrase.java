package Model.dataTypes;

// data structure that hold a specific term in a batch
public class Phrase {
    private int startIndex;
    private int endIndex;
    private String value;

    public Phrase(int startIndex, int endIndex, String value) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.value = value;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
