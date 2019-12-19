package Model.dataTypes;

/**
 * data structure that holds a specific term in a batch
 */
public class Phrase {
    private int startIndex;
    private int endIndex;
    private String value;

    public Phrase(int startIndex, int endIndex, String value) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.value = value;
    }

    /**
     * the index of the first char of the phrase in the document he is in
     * @return phrase's start index
     */
    public int getStartIndex() {
        return startIndex;
    }

    /**
     * the index of the end char of the phrase in the document he is in
     * @return phrase's end index
     */
    public int getEndIndex() {
        return endIndex;
    }

    /**
     * the value of the phrase (String)
     * @return phrase's value
     */
    public String getValue() {
        return value;
    }

    /**
     * updates the phrase's value
     * @param value new value
     */
    public void setValue(String value) {
        this.value = value;
    }
}
