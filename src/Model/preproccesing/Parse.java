package Model.preproccesing;

import Model.dataTypes.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

// parsing each file batch from the ReadFile to a HashMap of String, AllTermDocs
public class Parse{
    private enum DATE_FORMAT {
        Year, Month
    }
    public HashSet<Document> documents;
    private HashMap<String, AllTermDocs> termDocsMap;
    private Set<String> docs;
    private Set<String> stop_words;

    public Parse(Set<String> docs, Set<String> stop_words) {
        this.docs = docs;
        this.documents = new HashSet<>();
        this.termDocsMap = new HashMap<>();
        this.stop_words = new HashSet<>();
        this.stop_words = stop_words;
    }

    /**
     * sorting the parsed term and returning it
     * @return batch term-sorted list of: String, AllTermDocs
     */
    public ArrayList<Map.Entry<String, AllTermDocs>> getTermDocsMap() {
        Set entrySet = termDocsMap.entrySet();
        ArrayList<Map.Entry<String, AllTermDocs>> sortedTerms = new ArrayList<>(entrySet);
        Collections.sort(sortedTerms, new Comparator<Map.Entry<String, AllTermDocs>>() {
            @Override
            public int compare(Map.Entry<String, AllTermDocs> o1, Map.Entry<String, AllTermDocs> o2) {
                return o1.getKey().toLowerCase().compareTo(o2.getKey().toLowerCase());
            }
        });
        return sortedTerms;
    }

    /**
     * iterating over the docs and parse each one of them
     */
    public void parseBatch() {
        Iterator<String> iterator = this.docs.iterator();
        while (iterator.hasNext()) {
            String doc = iterator.next();
            iterator.remove();
            parseDoc(doc);
        }
    }

    /**
     * parsing the given document
     * @param doc retrieved doc content from ReadFile
     */
    private void parseDoc(String doc) {
        Document document = extractTextFromDoc(doc);
        numbers(document);
        words(document);
        wordMakaf(document);
        documents.add(document);
    }

    /**
     * finding all the numbers pattern and parsing them
     * @param document current document that being parsed
     */
    private void numbers(Document document) {
        String docText = document.getText();

        // the regex that finds the numbers in the given document
        Pattern pattern = Pattern.compile("[0-9]+((,[0-9]{3})+)?((\\.[0-9]+)|( ([0-9]+/[0-9]+)))?");
        Matcher matcher = pattern.matcher(docText);
        while (matcher.find()) {
            int startIndex = matcher.start();
            int endIndex = matcher.end();
            String match = matcher.group();
            Phrase phrase = new Phrase(startIndex, endIndex, match);

            // removing all the commas from the current number that was found
            phrase.setValue(phrase.getValue().replaceAll(",", ""));

            /*
            if a the number has a non-word linked right before him and after him
            then he is called: non-signed number (nonSingedNumber)
            Otherwise, he is called: signed number (signedNumber)
             */
            if (checkNonWord(docText.charAt(startIndex - 1)) && checkNonWord(docText.charAt(endIndex))) {
                // parsing non-signed numbers
                nonSignedNumbers(phrase, document);
            } else {
                // parsing signed numbers
                signedNumbers(phrase, document);
            }
        }
    }


    /**
     * parsing the given signed number according to the document that he is in
     * @param phrase a signed number
     * @param document containing the the given phrase
     */
    private void signedNumbers(Phrase phrase, Document document) {
        /*
        checking if it's a Signed Dollar rule:
        the phrase has 'm' or 'bn' linked right after him
        or has '$' linked right before him
         */
        if (checkSignedDollars(phrase, document)) {
            // parsing dollar signed number
            parseSignedDollars(phrase, document);
        }

        // parse "signed" missing unit numbers. for example: 52bn -> 52B
        else if (checkNextWord("m", phrase, document))
            addToHashMap(phrase.getValue() + "M", document);
        else if (checkNextWord("bn", phrase, document))
            addToHashMap(phrase.getValue() + "B", document);
        /*
        checking if it's a Signed percentage rule:
        the phrase has '%' linked right after him
         */
        else if (document.getText().charAt(phrase.getEndIndex()) == '%') {
            // parsing percentage singed number
            parsePercentage(phrase, document);
        }

        // *tryParse means to check and if true, then parse, and if false, then continue

        /*
        checking if it's a Singed Weight rule:
        the phrase has a 'kg' or 'gr' linked right after him
         */
        else if (tryParseSignedWeight(phrase, document)) ;

        /*
        checking if it's a Signed Distance rule:
        the phrase has a 'km' linked right after him
         */
        else if (tryParseSignedDistance(phrase, document)) ;
    }

    /**
     * tryParse a signed weight number
     * @param phrase a signed number
     * @param document containing the given phrase
     * @return True if the given phrase is a signed weight number and false if not
     */
    private boolean tryParseSignedWeight(Phrase phrase, Document document) {
        // if its a 'number friction' form, for example: '15 6/7'
        String[] numberFriction = phrase.getValue().split(" ");
        double number = Double.valueOf(numberFriction[0]);

        // checking if the phrase has a 'kg' linked right after him
        if (checkNextWord("kg", phrase, document)) {
            if (numberFriction.length > 1)
                // if it's a 'number friction' form, the number will be parsed as is
                addToHashMap(phrase.getValue() + " KG", document);
            else
                // if it's NOT a 'number friction' from, then try to shorten the number
                addToHashMap(shortFriction(number + "") + " KG", document);
            return true;
        }

        // checking if the phrase has a 'gr' linked right after him
        if (checkNextWord("gr", phrase, document)) {
            // scaling the number to be kilograms
            number = number / 1000;
            addToHashMap(shortFriction(number + "") + " KG", document);
            return true;
        }
        return false;
    }

    /**
     * tryParse a signed distance number
     * @param phrase a signed number
     * @param document containing the given phrase
     * @return True if the given phrase is a signed distance number and false if not
     */
    private boolean tryParseSignedDistance(Phrase phrase, Document document) {
        // checking if the phrase has a 'km' linked right after him
        if (checkNextWord("km", phrase, document)) {
            addToHashMap(phrase.getValue() + " KM", document);
            return true;
        }
        return false;
    }

    /**
     * parse a signed dollar number
     * @param phrase a signed number
     * @param document containing the given phrase
     */
    private void parseSignedDollars(Phrase phrase, Document document) {
        String docText = document.getText();
        int docTextLength = document.getTextLength();
        String numberString = phrase.getValue();
        int numberEndIndex = phrase.getEndIndex();
        // if its a 'number friction' form, for example: '15 6/7'
        if (numberString.contains("/")) {//number with friction
            numberString = numberString.substring(0, numberString.indexOf(' '));
        }
        double number = Double.parseDouble(numberString);
        //checking if the phrase has a 'm' linked right after him
        if (docText.charAt(numberEndIndex) == 'm') {
            //checking if the phrase is a int number. if it does, omit the decimal point
            if (number == (int) number) {
                addToHashMap((int) number + " M Dollars", document);
            } else {
                addToHashMap(number + " M Dollars", document);
            }
        }
        //precondition to check if phrase has a 'm' linked right after him
        else if (numberEndIndex + 2 >= docTextLength)
            parseNonSignedDollars(phrase, document);
            //checking if phrase has a 'bn' linked right after him
        else if (docText.substring(numberEndIndex, numberEndIndex + 2).equalsIgnoreCase("bn")) {
            number = number * 1000;
            //checking if the phrase is a int number. if it does, omit the decimal point
            if (number == (int) number)
                addToHashMap((int) number + " M Dollars", document);
            else
                addToHashMap(number + " M Dollars", document);
        } else {
            parseNonSignedDollars(phrase, document);
        }
    }

    /**
     * checks if phrase answers any of the conditions to be a saved as dollar number
     * @param phrase a signed number
     * @param document containing the given phrase
     * @return True if the given number is a signed dollar number and false if not
     */
    private boolean checkSignedDollars(Phrase phrase, Document document) {
        String docText = document.getText();
        int endIndex = phrase.getEndIndex();
        if (docText.charAt(phrase.getStartIndex() - 1) == '$')
            return true;
        if (checkNextWord("m dollar", phrase, document)) {
            return true;
        }
        if (checkNextWord("bn dollar", phrase, document)) {
            return true;
        }
        return false;
    }

    /**
     * parsing the given unsigned-number according to the document that he is in
     * @param phrase an unsigned number
     * @param document containing the given phrase
     */
    private void nonSignedNumbers(Phrase phrase, Document document) {
        /*
        checking if it's an unsigned Dollar rule:
        the phrase has dollar/u.s. dollar/billion u.s. dollar/million u.s. dollar linked after it
         */
        if (checkNonSignedDollars(phrase, document))
            //parsing dollar unsigned number
            parseNonSignedDollars(phrase, document);
        /*
        checking if it's an unsigned Percentage rule:
        the phrase has percentage/percent/per cent linked after it
         */
        else if (checkNonSignedPercentage(phrase, document))
            //parsing percentage unsigned number
            parsePercentage(phrase, document);
        /*
        checking if it's an unsigned Weight rule:
        the phrase has kg/kilogram linked after it.
        if it does, then parse phrase as weight number
         */
        else if (tryParseNonSignedWeight(phrase, document)) ;
        /*
        checking if it's an unsigned Distance rule:
        the phrase has meter/metre/km/kilometer/kilometre linked after it.
        if it does, then parse phrase as distance number
         */
        else if (tryParseNonSignedDistance(phrase, document)) ;
        /*
        checking if it's a date rule:
        month day|day month|year month
        if it does, then parse phrase as a date
         */
        else if (tryParseDates(phrase, document)) ;
        /*
        checking if it's an "Between number and number" rule
        if it does, then parse
         */
        else if (tryParseBetweenNumberAndNumber(phrase, document)) ;
        else
            //parse phrase as missing unit number
            parseMissingUnits(phrase, document);
    }

    /**
     * tryParse an unsigned weight number
     * @param phrase an unsigned number
     * @param document containing the given phrase
     * @return True if the given phrase is an unsigned weight number, false if not
     */
    private boolean tryParseNonSignedWeight(Phrase phrase, Document document) {
        //split the number from its potential friction
        String[] numberFriction = phrase.getValue().split(" ");
        double number = Double.valueOf(numberFriction[0]);
        //checking if the phrase has kilogram or kg linked after it
        if (checkNextWord(" kilogram", phrase, document) || checkNextWord(" kg", phrase, document)) {
            if (numberFriction.length > 1) {
                addToHashMap(phrase.getValue() + " KG", document);
            } else {
                addToHashMap(shortFriction(number + "") + " KG", document);
            }
            return true;
        }

        // checking if the phrase has gram or gr linked after it
        if (checkNextWord(" gram", phrase, document) || checkNextWord(" gr", phrase, document)) {
            number = number / 1000;
            addToHashMap(shortFriction(number + "") + " KG", document);
            return true;
        }
        return false;
    }

    /**
     * tryParse an unsigned distance number
     * @param phrase an unsigned number
     * @param document containing the given phrase
     * @return True if the given phrase is an unsigned distance number, false if not
     */
    private boolean tryParseNonSignedDistance(Phrase phrase, Document document) {
        // TODO use 'shortFriction' method

        String numberString = phrase.getValue();
        //check if the phrase has ' kilometer'/' kilometre'/' km' linked after him
        if (checkNextWord(" kilometer", phrase, document) || checkNextWord(" kilometre", phrase, document) || checkNextWord(" km", phrase, document)) {
            addToHashMap(numberString + " KM", document);
            return true;
        }
        //check if the phrase has ' meter'/' metre' linked after him
        if (checkNextWord(" meter", phrase, document) || checkNextWord(" metre", phrase, document)) {
            if (numberString.contains("/")) {//number with friction
                numberString = numberString.substring(0, numberString.indexOf(' '));
            }
            double number = Double.parseDouble(numberString);
            number = number / 1000;
            //omit decimal point for numbers like 100.0 and save number in KM scales
            if (number == (int) number) {
                addToHashMap((int) number + " KM", document);
                return true;
            } else {
                addToHashMap(shortFriction(number + "") + " KM", document);
                return true;
            }
        }
        return false;
    }

    /**
     * tryParse a "Between number and number" rule
     * @param phrase an unsigned number
     * @param document containing the given phrase
     * @return True if the given phrase is the first number in a "Between number and number" rule, false if not
     */
    private boolean tryParseBetweenNumberAndNumber(Phrase phrase, Document document) {
        if (checkPreviousWord("between ", phrase, document)) {
            if (checkNextWord(" and", phrase, document)) {

                String docText = document.getText();
                int docTextLength = document.getTextLength();
                int endPhraseIndex = phrase.getEndIndex();
                int endWordIndex = document.getText().indexOf(' ', endPhraseIndex + 5);

                // if has space in the next characters
                if (endWordIndex != -1) {
                    if (checkValidForwardIndex(endPhraseIndex + 4, (endPhraseIndex + 4) - 1 - endPhraseIndex, docTextLength)) {
                        String candidateNumber = docText.substring(endPhraseIndex + 5, endWordIndex);
                        try {
                            double nextNumber = Double.valueOf(candidateNumber);
                            addToHashMap(shortFriction(phrase.getValue()) + "-" + shortFriction(nextNumber + ""), document);
                            return true;
                        } catch (NumberFormatException e) {
                            // not a number
                            return false;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * tryParse dates rule
     * @param phrase an unsigned number
     * @param document containing the given phrase
     * @return True if the given phrase qualify as a date, false if not
     */
    private boolean tryParseDates(Phrase phrase, Document document) {
        String[] numberFriction = phrase.getValue().split(" ");
        // if its a "number friction" its NOT a date
        if (numberFriction.length > 1)
            return false;
        if (numberFriction[0].contains("."))
            return false;
        double number = Double.valueOf(numberFriction[0]);
        // DD-MM or MM-DD
        if (number <= 31) {
            if (tryParseFullMonthName(phrase, document, DATE_FORMAT.Month))
                return true;
            return tryParseShortMonthName(phrase, document, DATE_FORMAT.Month);
        }
        // YYYY-MM
        if (tryParseFullMonthName(phrase, document, DATE_FORMAT.Year))
            return true;
        return tryParseShortMonthName(phrase, document, DATE_FORMAT.Year);

    }

    /**
     * tryParse date rule while month is written in its shorten version
     * @param phrase an unsigned number
     * @param document containing the given phrase
     * @param date_format enum that specify whether its a day/year rule
     * @return true if the given phrase qualify as a date and month part was written in its shorten version, false if not
     */
    private boolean tryParseShortMonthName(Phrase phrase, Document document, DATE_FORMAT date_format) {
        String day = phrase.getValue();
        if (day.length() < 2)
            day = "0" + day;

        String docText = document.getText();
        int docTextLength = document.getTextLength();
        int startPhraseIndex = phrase.getStartIndex();
        int endPhraseIndex = phrase.getEndIndex();
        int startNextIndex = endPhraseIndex + 1;
        int endNextIndex = endPhraseIndex + 4;
        int startPrevIndex = startPhraseIndex - 4;
        int endPrevIndex = startPhraseIndex - 1;

        // checks if next word is short version of month name
        if (checkValidForwardIndex(endPhraseIndex, 3, docTextLength)) {
            String candidateMonth = docText.substring(startNextIndex, endNextIndex);
            // convertedMonth is a number that represent a month name
            String convertedMonth = convertShortMonth(candidateMonth);
            if (convertedMonth != null) {
                if (date_format == DATE_FORMAT.Year)
                    addToHashMap(day + "-" + convertedMonth, document);
                else
                    addToHashMap(convertedMonth + "-" + day, document);
                return true;
            }
        }
        // checks if previous word is short version of month name
        if (checkValidBackwardIndex(startPhraseIndex, 4)) {
            String candidateMonth = docText.substring(startPrevIndex, endPrevIndex);
            String convertedMonth = convertShortMonth(candidateMonth);
            // convertedMonth is a number that represent a month name
            if (convertedMonth != null) {
                if (date_format == DATE_FORMAT.Year)
                    addToHashMap(day + "-" + convertedMonth, document);
                else
                    addToHashMap(convertedMonth + "-" + day, document);
                return true;
            }
        }
        return false;
    }

    /**
     * Converts month shorten version to its numeric value
     * @param candidateMonth string with length of 3
     * @return numeric value of a month if candidateMonth is a shorten version of a month, null otherwise
     */
    private String convertShortMonth(String candidateMonth) {
        String actualMonth = null;
        if (candidateMonth.equalsIgnoreCase("jan")) {
            actualMonth = "01";
        }
        if (candidateMonth.equalsIgnoreCase("feb")) {
            actualMonth = "02";
        }
        if (candidateMonth.equalsIgnoreCase("mar")) {
            actualMonth = "03";
        }
        if (candidateMonth.equalsIgnoreCase("apr")) {
            actualMonth = "04";
        }
        if (candidateMonth.equalsIgnoreCase("may")) {
            actualMonth = "05";
        }
        if (candidateMonth.equalsIgnoreCase("jun")) {
            actualMonth = "06";
        }
        if (candidateMonth.equalsIgnoreCase("jul")) {
            actualMonth = "07";
        }
        if (candidateMonth.equalsIgnoreCase("aug")) {
            actualMonth = "08";
        }
        if (candidateMonth.equalsIgnoreCase("sep")) {
            actualMonth = "09";
        }
        if (candidateMonth.equalsIgnoreCase("oct")) {
            actualMonth = "10";
        }
        if (candidateMonth.equalsIgnoreCase("nov")) {
            actualMonth = "11";
        }
        if (candidateMonth.equalsIgnoreCase("dec")) {
            actualMonth = "12";
        }
        return actualMonth;
    }

    /**
     * tryParse date rule while month is written in its full version
     * @param phrase an unsigned number
     * @param document containing the given phrase
     * @param date_format enum that specify whether its a day/year rule
     * @return true if the given phrase qualify as a date and month part was written in its full version, false if not
     */
    private boolean tryParseFullMonthName(Phrase phrase, Document document, DATE_FORMAT date_format) {
        String day = phrase.getValue();
        if (day.length() < 2)
            day = "0" + day;

        String actualMonth = null;
        if (checkNextWord(" january", phrase, document) || checkPreviousWord("january ", phrase, document)) {
            actualMonth = "01";
        }
        if (checkNextWord(" february", phrase, document) || checkPreviousWord("february ", phrase, document)) {
            actualMonth = "02";
        }
        if (checkNextWord(" march", phrase, document) || checkPreviousWord("march ", phrase, document)) {
            actualMonth = "03";
        }
        if (checkNextWord(" april", phrase, document) || checkPreviousWord("april ", phrase, document)) {
            actualMonth = "04";
        }
        if (checkNextWord(" may", phrase, document) || checkPreviousWord("may ", phrase, document)) {
            actualMonth = "05";
        }
        if (checkNextWord(" june", phrase, document) || checkPreviousWord("june ", phrase, document)) {
            actualMonth = "06";
        }
        if (checkNextWord(" july", phrase, document) || checkPreviousWord("july ", phrase, document)) {
            actualMonth = "07";
        }
        if (checkNextWord(" august", phrase, document) || checkPreviousWord("august ", phrase, document)) {
            actualMonth = "08";
        }
        if (checkNextWord(" september", phrase, document) || checkPreviousWord("september ", phrase, document)) {
            actualMonth = "09";
        }
        if (checkNextWord(" october", phrase, document) || checkPreviousWord("october ", phrase, document)) {
            actualMonth = "10";
        }
        if (checkNextWord(" november", phrase, document) || checkPreviousWord("november ", phrase, document)) {
            actualMonth = "11";
        }
        if (checkNextWord(" december", phrase, document) || checkPreviousWord("december ", phrase, document)) {
            actualMonth = "12";
        }
        if (actualMonth == null)
            return false;
        if (date_format == DATE_FORMAT.Year)
            addToHashMap(day + "-" + actualMonth, document);
        else
            addToHashMap(actualMonth + "-" + day, document);
        return true;
    }

    /**
     * parse percentage number
     * @param phrase a number
     * @param document containing the given phrase
     */
    private void parsePercentage(Phrase phrase, Document document) {
        String[] numberFriction = phrase.getValue().split(" ");
        if (numberFriction.length > 1) {
            addToHashMap(phrase.getValue() + "%", document);
        } else {
            String toAdd = shortFriction(phrase.getValue());
            addToHashMap(toAdd + "%", document);
        }
    }

    /**
     * checks if phrase answers any of the conditions to be a saved as percentage number
     * @param phrase an unsigned number
     * @param document containing the given phrase
     * @return True if the given phrase is an unsigned percentage number, false if not
     */
    private boolean checkNonSignedPercentage(Phrase phrase, Document document) {
        if (checkNextWord(" percent", phrase, document)) {
            return true;
        }
        if (checkNextWord(" percentage", phrase, document)) {
            return true;
        }
        if (checkNextWord(" per cent", phrase, document)) {
            return true;
        }
        return false;
    }

    /**
     * checks if the given string appears in the document after the given phrase
     * @param nextWord string candidate that might appear after phrase in the document
     * @param phrase string
     * @param document containing the given phrase and nextWord
     * @return True if nextWord appears in the document after the given phrase, false if not
     */
    private boolean checkNextWord(String nextWord, Phrase phrase, Document document) {
        String docText = document.getText();
        int docTextLength = document.getTextLength();
        int nextWordLength = nextWord.length();
        int endPhraseIndex = phrase.getEndIndex();
        if (checkValidForwardIndex(endPhraseIndex, nextWordLength, docTextLength)) {
            String candidateWord = docText.substring(endPhraseIndex, endPhraseIndex + nextWordLength);
            if (candidateWord.equalsIgnoreCase(nextWord)) {
                return true;
            }
        }
        return false;
    }

    /**
     * checks if the given string appears in the document before the given phrase
     * @param prevWord string candidate that might appear before phrase in the document
     * @param phrase string
     * @param document containing the given phrase and prevWord
     * @return True if prevWord appears in the document before the given phrase, false if not
     */
    private boolean checkPreviousWord(String prevWord, Phrase phrase, Document document) {
        String docText = document.getText();
        int prevWordLength = prevWord.length();
        int startPhraseIndex = phrase.getStartIndex();
        if (checkValidBackwardIndex(startPhraseIndex, prevWordLength)) {
            String candidateWord = docText.substring(startPhraseIndex - prevWordLength, startPhraseIndex);
            if (candidateWord.equalsIgnoreCase(prevWord)) {
                return true;
            }
        }
        return false;
    }

    /**
     * parse an unsigned dollar number
     * @param phrase a unsigned number
     * @param document containing the given phrase
     */
    private void parseNonSignedDollars(Phrase phrase, Document document) {
        String docText = document.getText();
        int docTextLength = document.getTextLength();
        String numberString = phrase.getValue();
        int numberEndIndex = phrase.getEndIndex();
        if (numberString.contains("/")) {//number with friction
            numberString = numberString.substring(0, numberString.indexOf(' '));
        }
        double number = Double.parseDouble(numberString);
        if (number >= 1000000) {
            number = number / 1000000;
            if (number == (int) number)
                addToHashMap((int) number + " M Dollars", document);
            else
                addToHashMap(shortFriction(number + "") + " M Dollars", document);
        } else {
            handleBelowMillionNonSignedDollars(phrase, document, docText, docTextLength, numberEndIndex, number);
        }
    }

    /**
     * handle below million unsigned dollars numbers
     * @param phrase an unsigned number
     * @param document containing the given phrase
     * @param docText document text
     * @param docTextLength document text length
     * @param numberEndIndex phrase end index
     * @param number double representing the phrase number
     */
    private void handleBelowMillionNonSignedDollars(Phrase phrase, Document document, String docText, int docTextLength, int numberEndIndex, double number) {
        if (checkValidForwardIndex(numberEndIndex, 7, docTextLength)) {
            String candidateMillionOrBillion = docText.substring(numberEndIndex, numberEndIndex + 8);
            if (candidateMillionOrBillion.equalsIgnoreCase(" million")) {
                if (number == (int) number)
                    addToHashMap((int) number + " M Dollars", document);
                else
                    addToHashMap(number + " M Dollars", document);
            } else if (candidateMillionOrBillion.equalsIgnoreCase(" billion")) {
                number = number * 1000;
                if (number == (int) number)
                    addToHashMap((int) number + " M Dollars", document);
                else
                    addToHashMap(number + " M Dollars", document);
            } else
                addToHashMap(phrase.getValue() + " Dollars", document);
        } else
            addToHashMap(phrase.getValue() + " Dollars", document);
    }

    /**
     * checks if phrase answers any of the conditions to be a saved as dollar number
     * @param phrase an unsigned number
     * @param document containing the given phrase
     * @return True if the given phrase is an unsigned dollar number, false if not
     */
    private boolean checkNonSignedDollars(Phrase phrase, Document document) {
        if (checkNextWord(" dollar", phrase, document)) {
            return true;
        }
        if (checkNextWord(" billion u.s. dollar", phrase, document))
            return true;
        if (checkNextWord(" million u.s. dollar", phrase, document))
            return true;
        if (checkNextWord(" u.s. dollar", phrase, document))
            return true;

        return false;
    }

    /**
     * parsing the given phrase according to missing-units rule
     * @param phrase unsigned missing-unit number
     * @param document containing the given phrase
     */
    private void parseMissingUnits(Phrase phrase, Document document) {
        String[] numberFriction = phrase.getValue().split(" ");
        double number = Double.valueOf(numberFriction[0]);
        // below thousand
        if (number < 1000) {
            parseMissingUnitsBelowThousand(phrase, document);
        }
        // thousands
        else if (number >= 1000 && number < 1000000) {
            parseMissingUnitThousand(phrase, document);
        }
        // millions
        else if (number >= 1000000 && number < 1000000000) {
            parseMissingUnitMillion(phrase, document);
        }
        // billions
        else {
            parseMissingUnitBillion(phrase, document);
        }
    }

    /**
     * parsing the given phrase as a "above billion missing-units"
     * @param phrase an unsigned missing-unit number
     * @param document containing the given phrase
     */
    private void parseMissingUnitBillion(Phrase phrase, Document document) {
        String[] numberFriction = phrase.getValue().split(" ");
        double number = Double.valueOf(numberFriction[0]);
        number = number / 1000000000;
        addToHashMap(shortFriction(number + "") + "B", document);
    }

    /**
     * parsing the given phrase as a "between billion and million missing-units"
     * @param phrase an unsigned missing-unit number
     * @param document containing the given phrase
     */
    private void parseMissingUnitMillion(Phrase phrase, Document document) {
        String[] numberFriction = phrase.getValue().split(" ");
        double number = Double.valueOf(numberFriction[0]);
        number = number / 1000000;
        addToHashMap(shortFriction(number + "") + "M", document);
    }

    /**
     * parsing the given phrase as a "between million and thousand missing-units"
     * @param phrase an unsigned missing-unit number
     * @param document containing the given phrase
     */
    private void parseMissingUnitThousand(Phrase phrase, Document document) {
        String[] numberFriction = phrase.getValue().split(" ");
        double number = Double.valueOf(numberFriction[0]);
        number = number / 1000;
        addToHashMap(shortFriction(number + "") + "K", document);
    }

    /**
     * parsing the given phrase as a "below thousand missing-units"
     * will check if and number has thousand/million/billion linked to them and act accordingly
     * @param phrase an unsigned missing-unit number
     * @param document containing the given phrase
     */
    private void parseMissingUnitsBelowThousand(Phrase phrase, Document document) {
        int phraseEndIndex = phrase.getEndIndex();
        // number Thousand/Million/Billion
        if (checkValidForwardIndex(phraseEndIndex, 7, document.getTextLength())) {
            String nextWord = document.getText().substring(phraseEndIndex + 1, phraseEndIndex + 8);
            if (nextWord.equalsIgnoreCase("million")) {
                String toAdd = shortFriction(phrase.getValue());
                addToHashMap(toAdd + "M", document);
                return;
            } else if (nextWord.equalsIgnoreCase("billion")) {
                String toAdd = shortFriction(phrase.getValue());
                addToHashMap(toAdd + "B", document);
                return;
            }
            if (checkNextWord(" thousand", phrase, document)) {
                String toAdd = shortFriction(phrase.getValue());
                addToHashMap(toAdd + "K", document);
            }
        }

        // number
        String[] numberFriction = phrase.getValue().split(" ");
        String toAdd = shortFriction(phrase.getValue());
        if (numberFriction.length < 0)
            toAdd = shortFriction(toAdd);
        addToHashMap(toAdd, document);
    }

    /**
     * takes a string that represents a number and correct it - 3 digits after the decimal point
     * @param value string that represents a number
     * @return string that contains the fixed number
     */
    private String shortFriction(String value) {
        if (value.contains(".")) {
            int indexOfStartFriction = value.indexOf(".");
            // if contains "0E-5" for example - irrelevant
            if (value.indexOf("E", indexOfStartFriction) < 0) {
                // remain the last 3 digits after the dot
                if (indexOfStartFriction + 4 < value.length()) {
                    value = value.substring(0, indexOfStartFriction + 4);
                }
                //omitting 0s at the end of the number, after the decimal points
                while (value.endsWith("0")) {
                    value = (value.substring(0, value.length() - 1));
                }
                //omit the decimal point in case there are no numbers after it
                if (value.endsWith(".")) {
                    value = (value.substring(0, value.length() - 1));
                }
            }
        }
        return value;
    }

    /**
     * check if can go k characters afterwards from end index
     * @param end end index of the checked word
     * @param k number of characters that being checked
     * @param endOfText the last valid index of the document text
     * @return true if end+k dont go above index endOfText and false otherwise
     */
    private boolean checkValidForwardIndex(int end, int k, int endOfText) {
        return end + k < endOfText;
    }

    /**
     * check if can go k characters backward from start index
     * @param start start index of the checked word
     * @param k number of characters that being checked
     * @return true if start-k dont go below index 0 and false otherwise
     */
    private boolean checkValidBackwardIndex(int start, int k) {
        return start - k >= 0;
    }

    /**
     * checks if the supsect char in a non-word
     *
     * @param suspect char
     * @return True if the suspect char a non-word, false if not
     */
    private boolean checkNonWord(char suspect) {
        String nonWords = "!@#^&*()<>?'~+`;:_[]-{}=|/\",.\n\t ";
        if (nonWords.indexOf(suspect) >= 0) {
            return true;
        }
        return false;
    }

    /**
     * parse a regular word and entity rules
     * @param document current document that being parsed
     */
    private void words(Document document) {
        Pattern pattern = Pattern.compile("(?<=(\\W))[a-zA-Z]+(?=(\\W))");
        Matcher matcher = pattern.matcher(document.getText());
        while (matcher.find()) {
            Phrase currentPhrase = new Phrase(matcher.start(), matcher.end(), matcher.group());
            // parsing the single word that was matched
            parseSingleWord(currentPhrase, document);
            if (isFirstCharCapital(currentPhrase)) {
                // maybe starting an entity
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(currentPhrase.getValue());
                while (matcher.find()) {
                    Phrase nextPhrase = new Phrase(matcher.start(), matcher.end(), matcher.group());
                    // parsing the single word that was matched
                    parseSingleWord(nextPhrase, document);
                    if (isFirstCharCapital(nextPhrase)) {
                        // maybe continuing the entity
                        int endEntityIndex = currentPhrase.getStartIndex() + stringBuilder.length();
                        // the next word is right after
                        if (endEntityIndex + 1 == nextPhrase.getStartIndex()) {
                            stringBuilder.append(" " + nextPhrase.getValue());
                        }
                        // the next word is not right after
                        else {
                            // if the entity is not one term long, it is an entity
                            if (currentPhrase.getValue().length() != stringBuilder.toString().length()) {
                                Phrase entity = new Phrase(currentPhrase.getStartIndex(), currentPhrase.getStartIndex() + stringBuilder.length(), stringBuilder.toString());
                                addToHashMap(entity.getValue().toUpperCase() + " ", document);
                            }
                            // starting the entity searching with nextPhrase to be the first word of the entity
                            stringBuilder = new StringBuilder();
                            stringBuilder.append(nextPhrase.getValue());
                            currentPhrase = nextPhrase;
                        }
                    } else {
                        break;
                    }
                }
                // if the entity is not one term long, it is an entity
                if (currentPhrase.getValue().length() != stringBuilder.toString().length()) {
                    Phrase entity = new Phrase(currentPhrase.getStartIndex(), currentPhrase.getStartIndex() + stringBuilder.length(), stringBuilder.toString());
                    addToHashMap(entity.getValue().toUpperCase() + " ", document);
                }
            }
        }
    }

    /**
     * checks if the first letter of phrase is a capital letter
     *
     * @param phrase a phrase to check
     * @return true if the first letter of phrase is a capital letter, false if not
     */
    private boolean isFirstCharCapital(Phrase phrase) {
        String fisrtChar = phrase.getValue().charAt(0) + "";
        if (fisrtChar.equals(fisrtChar.toUpperCase()))
            return true;
        return false;
    }

    /**
     * parse a single word, using stemmer if needed
     *
     * @param phrase   represents a word
     * @param document containing the given phrase
     */
    private void parseSingleWord(Phrase phrase, Document document) {
        if (!stop_words.contains(phrase.getValue().toLowerCase())) {
            Stemmer stemmer = new Stemmer();
            stemmer.add(phrase.getValue().toLowerCase().toCharArray(), phrase.getValue().length());
            stemmer.stem();
            String toAdd = stemmer.toString();
            if (isFirstCharCapital(phrase))
                toAdd = toAdd.toUpperCase();
            addToHashMap(toAdd, document);
        }
    }

    /**
     * parse a wordMakaf rule(for example apples-oranges)
     *
     * @param document current document that being parsed
     */
    private void wordMakaf(Document document) {
        Pattern pattern = Pattern.compile("(([a-zA-Z]+)|([0-9]+(,[0-9]{3})*((\\.[0-9]+)?)))(-(([a-zA-Z]+)|([0-9]+(,[0-9]{3})*((\\.[0-9]+)?))))+");
        Matcher matcher = pattern.matcher(document.getText());
        String candidateExpression;
        while (matcher.find()) {
            candidateExpression = matcher.group();
            candidateExpression = candidateExpression.replaceAll(",", "");
            addToHashMap(candidateExpression.toLowerCase(), document);
        }
    }

    /**
     * extract text from given document and save the relevant information to a Document
     *
     * @param doc string representing full document
     * @return Document with relevant information
     */
    private Document extractTextFromDoc(String doc) {
        StringBuilder stringBuilder = new StringBuilder();
        // extracting text from doc
        Pattern parseTextsFromDocRegex = Pattern.compile("(?<=(<TEXT>))(.+?)(?=(</TEXT>))", Pattern.DOTALL);
        Matcher matcher = parseTextsFromDocRegex.matcher(doc);
        stringBuilder.append(" ");
        while (matcher.find()) {
            stringBuilder.append(matcher.group());
            stringBuilder.append(" ");
        }
        String docText = stringBuilder.toString();
        // extracting DOCNO from doc
        Pattern parseDOCNOFromDoc = Pattern.compile("(?<=(<DOCNO>))(.+?)(?=(</DOCNO>))");
        matcher = parseDOCNOFromDoc.matcher(doc);
        String docNO = "";
        if (matcher.find())
            docNO = matcher.group();
        docText = docText.replaceAll("\\s+", " ");
        return new Document(docNO, docText);
    }

    /**
     * updating termDocsMap.
     * If termDocsMap contains value - update his record and add document number,
     * otherwise add new record of value with document number
     *
     * @param value    string that represent a key/new record in termDocsMap
     * @param document containing the value given
     */
    private void addToHashMap(String value, Document document) {
        // if already exist with lower case
        if (termDocsMap.containsKey(value.toLowerCase())) {
            AllTermDocs allTermDocs = termDocsMap.get(value.toLowerCase());
            allTermDocs.addTermDetails(document.getDocNo());
            document.addTerm(value.toLowerCase());
        }
        // already exist with upper case
        else if (termDocsMap.containsKey(value.toUpperCase())) {
            // current term is also upper case
            if (value.equals(value.toUpperCase())) {
                AllTermDocs allTermDocs = termDocsMap.get(value);
                allTermDocs.addTermDetails(document.getDocNo());
                document.addTerm(value);
            }
            // current term is lower case
            else {
                AllTermDocs allTermDocs = termDocsMap.remove(value.toUpperCase());
                allTermDocs.addTermDetails(document.getDocNo());
                termDocsMap.put(value, allTermDocs);
                document.addTerm(value);
            }
        }
        // term not exist in the HashMap
        else {
            AllTermDocs allTermDocs = new AllTermDocs(document.getDocNo());
            termDocsMap.put(value, allTermDocs);
            document.addTerm(value);
        }
    }
}
