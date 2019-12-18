package Model.dataTypes;

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

    public void setStartIndex(int startIndex) {
        this.startIndex = startIndex;
    }

    public void setEndIndex(int endIndex) {
        this.endIndex = endIndex;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
