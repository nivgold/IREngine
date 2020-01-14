package View;

import Model.Manager;
import Model.communicator.ConfigReader;
import Model.dataTypes.ResultQuery;
import Model.dataTypes.Term;
import Model.preproccesing.Parse;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.*;
import java.util.*;

/**
 * Listening to the ActionEvents that occurring in the GUI
 */
public class Controller {
    private Stage mainStage;
    private StringProperty corpusPath = new SimpleStringProperty("set path");
    private StringProperty postingPath = new SimpleStringProperty("set path");
    private StringProperty queriesPath = new SimpleStringProperty("set path");
    private Manager manager;
    private boolean isResultFileExist = false;

    @FXML
    private Button start_Button;
    @FXML
    private Button corpus_path_Button;
    @FXML
    private Button posting_path_Button;
    @FXML
    private Button reset_Button;
    @FXML
    private Button show_dictionary_Button;
    @FXML
    private Button load_dictionary_Button;
    @FXML
    private TextField corpus_path_TextField;
    @FXML
    private TextField posting_path_TextField;
    @FXML
    private CheckBox stemming_CheckBox;
    @FXML
    private CheckBox semantic_CheckBox;
    @FXML
    private TextField queries_path_TextField;
    @FXML
    private Button queries_path_run_Button;
    @FXML
    private TextField search_TextField;

    /**
     * initializing the class necessarily fields
     * @param mainStage main Application stage
     * @param manager an instance of Manager
     */
    public void initialize(Stage mainStage, Manager manager){
        this.mainStage = mainStage;
        this.manager = manager;
        corpus_path_TextField.textProperty().bind(Bindings.format("%s", this.corpusPath));
        posting_path_TextField.textProperty().bind(Bindings.format("%s", this.postingPath));
        queries_path_TextField.textProperty().bind(Bindings.format("%s", this.queriesPath));
    }

    /**
     * handles the event associated with 'corpus_path' button
     * @param actionEvent an ActionEvent associated with "corpus_path" Button
     */
    public void corpus_pathAction(ActionEvent actionEvent){
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDir = directoryChooser.showDialog(mainStage);
        if (selectedDir == null){
            postingPath.setValue("set path");
        }
        else{
            corpusPath.setValue(selectedDir.getAbsolutePath());
            // update the config file
            ConfigReader.updateCorpusPath(selectedDir.getAbsolutePath());
        }
    }

    /**
     * handles the event associated with 'posting_path' Button
     * @param actionEvent an ActionEvent associated with 'posting_path' Button
     */
    public void posting_pathAction(ActionEvent actionEvent){
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDir = directoryChooser.showDialog(mainStage);
        if (selectedDir == null){
            postingPath.setValue("set path");
        }
        else{
            postingPath.setValue(selectedDir.getAbsolutePath());
            ConfigReader.updatePostingPath(selectedDir.getAbsolutePath());
        }
    }

    /**
     * handles the event associated with 'queries_path' Button
     * @param actionEvent an ActionEvent associated with 'queries_path' Button
     */
    public void queries_pathAction(ActionEvent actionEvent){
        FileChooser fileChooser = new FileChooser();
        File selectedFile = fileChooser.showOpenDialog(mainStage);
        if (selectedFile == null){
            queriesPath.setValue("set path");
        }
        else{
            queriesPath.setValue(selectedFile.getAbsolutePath());
            ConfigReader.updateQueriesPath(selectedFile.getAbsolutePath());
        }
    }

    /**
     * handles the event associated with 'start' Button - starting the index process if allowed
     * @param actionEvent an ActionEvent associated with the 'start' Button
     */
    public void startAction(ActionEvent actionEvent){
        if (corpusPath.get().equals("set path") || postingPath.get().equals("set path")){
            // pop a warning dialog
            Alert alert = new Alert(Alert.AlertType.WARNING, "not all parameters were set");
            alert.show();
        }
        else{
            try {
                this.manager = new Manager();
                // start process
                this.manager.startProcess();
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Documents Read: " + Parse.docCounter.get() + "\nUnique Terms: " + manager.getUniqueTermsNum() + "\nTotal Time: " + manager.getTotalTime());
                alert.show();
            }catch (Exception e){
                Alert alert = new Alert(Alert.AlertType.ERROR, e.getMessage()+"\n"+ConfigReader.WORKER_NUM);
                alert.show();
            }
        }
    }

    /**
     * handles the event associated with 'run' Button - starting the retrieval process from path if allowed
     * @param actionEvent an ActionEvent associated with the 'query file path run' Button
     */
    public void queries_path_runAction(ActionEvent actionEvent){
        if (queriesPath.get().equals("set path") || postingPath.get().equals("set path")){
            // pop an error dialog
            Alert alert = new Alert(Alert.AlertType.ERROR, "Not All Parameters Were Set");
            alert.show();
        }
        else if (!this.manager.hasDictionary()){
            // pop en error dialog
            Alert alert = new Alert(Alert.AlertType.ERROR, "No Dictionary Was Found");
            alert.show();
        }
        else{
            try{
                if (semantic_CheckBox.isSelected()){
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Choose Online/Offline", new ButtonType("Online"), new ButtonType("Offline"), new ButtonType("Exit"));
                    Optional<ButtonType> result = alert.showAndWait();
                    if (result.get().getText().equals("Online"))
                        ConfigReader.setOnlineSemantic(true);
                    else if (result.get().getText().equals("Offline"))
                        ConfigReader.setOnlineSemantic(false);
                    else{
                        return;
                    }
                }
                Map<String, List<Map.Entry<String, Double>>> result = this.manager.retrieveFromPath();
                //pop a new stage that showing the results
                showQueryResult(result);
            }catch (Exception e){
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR, e.getMessage());
                alert.show();
            }
        }
    }

    /**
     * handles the event associated with 'run' Button - starting the retrieval process of free text if allowed
     * @param actionEvent an ActionEvent associated with the 'search run' Button
     */
    public void search_runAction(ActionEvent actionEvent){
        if (postingPath.get().equals("set path") || search_TextField.getText()==""){
            // pop an error dialog
            Alert alert = new Alert(Alert.AlertType.ERROR, "Not All Parameters Were Set");
            alert.show();
        }
        else if (!this.manager.hasDictionary()){
            // pop en error dialog
            Alert alert = new Alert(Alert.AlertType.ERROR, "No Dictionary Was Found");
            alert.show();
        }
        else{
            try{
                if (semantic_CheckBox.isSelected()){
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Choose Online/Offline", new ButtonType("Online"), new ButtonType("Offline"), new ButtonType("Exit"));
                    Optional<ButtonType> result = alert.showAndWait();
                    if (result.get().getText().equals("Online"))
                        ConfigReader.setOnlineSemantic(true);
                    else if (result.get().getText().equals("Offline"))
                        ConfigReader.setOnlineSemantic(false);
                    else{
                        return;
                    }
                }
                Map<String, List<Map.Entry<String, Double>>> result = this.manager.retrieveFromText(search_TextField.getText());
                // pop a new stage that showing the results
                showQueryResult(result);
            }catch (Exception e){
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR, e.getMessage());
                alert.show();
            }
        }
    }

    /**
     * handles the event associated with 'stemming' CheckBox
     * @param actionEvent an ActionEvent associated with the 'stemming' CheckBox
     */
    public void stemmingAction(ActionEvent actionEvent){
        if (stemming_CheckBox.isSelected()){
            // with stemming
            ConfigReader.setStemming(true);
        }
        else{
            // without stemming
            ConfigReader.setStemming(false);
        }
    }

    /**
     * handles the event associated with 'semantic treat' CheckBox
     * @param actionEvent an ActionEven associated with the 'semantic treat' CheckBox
     */
    public void semanticAction(ActionEvent actionEvent){
        if (semantic_CheckBox.isSelected()){
            // with semantic treat
            ConfigReader.setSemantic(true);
        }
        else{
            // without semantic treat
            ConfigReader.setSemantic(false);
        }
    }

    /**
     * handles the event associated with the 'reset' Button
     * @param actionEvent an ActionEvent associated with the 'reset' Button
     */
    public void resetAction(ActionEvent actionEvent){
        if (postingPath.get().equals("set path")){
            Alert alert = new Alert(Alert.AlertType.ERROR, "Posting Path Not Specified");
            alert.show();
        }
        else {
            File postingDir = new File(ConfigReader.POSTING_DIR_PATH);
            if (postingDir.listFiles().length==0){
                // empty
                Alert alert = new Alert(Alert.AlertType.ERROR, "Directory Is Empty");
                alert.show();
            }
            else{
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Are Your Sure?");
                Optional<ButtonType> result = alert.showAndWait();
                if (result.get() == ButtonType.OK) {
                    deleteDirectory(postingDir);
                    postingDir.mkdirs();
                }
                this.manager = null;
            }

        }
    }

    /**
     * cleaning all the directory content
     * @param directory path to the directory needs to be clean
     * @return True if all the directory content was deleted and False if not
     */
    private boolean deleteDirectory(File directory){
        File[] files = directory.listFiles();
        if (files != null){
            for (File file : files)
                deleteDirectory(file);
        }
        return directory.delete();
    }

    /**
     * handles the event associated with the 'show_dictionary' Button
     * @param actionEvent an ActionEvent associated with the 'show_dictionary' Button
     */
    public void showDictionaryButton(ActionEvent actionEvent){
        if (postingPath.get().equals("set path")){
            Alert alert = new Alert(Alert.AlertType.ERROR, "No Posting Path Was Specified");
            alert.show();
        }
        else {
            File dictionary = new File(ConfigReader.INVERTED_DICTIONARY_FILE_PATH);
            if (!dictionary.exists()) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "No Dictionary Found In Posting Path");
                alert.show();
            } else {
                // show dictionary
                Stage dictionaryStage = new Stage();
                TableView tableView = new TableView();
                TableColumn<String, Term> column1 = new TableColumn<>("Term");
                column1.setCellValueFactory(new PropertyValueFactory<>("value"));
                TableColumn<String, Term> column2 = new TableColumn<>("TF");
                column2.setCellValueFactory(new PropertyValueFactory<>("corpusTF"));
                tableView.getColumns().addAll(column1, column2);
                try {
                    List<String> sortedDictionary = new ArrayList<>();
                    BufferedReader bufferedReader = new BufferedReader(new FileReader(ConfigReader.INVERTED_DICTIONARY_FILE_PATH));
                    String currentLine = "";
                    while ((currentLine = bufferedReader.readLine()) != null) {
                        sortedDictionary.add(currentLine);
                    }
                    bufferedReader.close();
                    Collections.sort(sortedDictionary, new Comparator<String>() {
                        @Override
                        public int compare(String o1, String o2) {
                            int firstDeli = o1.indexOf(';');
                            int secondDeli = o2.indexOf(';');
                            String term1 = o1.substring(0, firstDeli);
                            String term2 = o2.substring(0, secondDeli);
                            return term1.compareTo(term2);
                        }
                    });
                    for (String entry : sortedDictionary) {
                        int firstDelimiter = entry.indexOf(';');
                        int secondDelimiter = entry.indexOf(';', firstDelimiter + 1);
                        String term = entry.substring(0, firstDelimiter);
                        String corpusTF = entry.substring(firstDelimiter + 1, secondDelimiter);
                        tableView.getItems().add(new Term(term, corpusTF));
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                VBox vBox = new VBox(tableView);
                Scene scene = new Scene(vBox);
                dictionaryStage.setScene(scene);
                dictionaryStage.initModality(Modality.APPLICATION_MODAL);
                dictionaryStage.show();

            }
        }
    }

    /**
     * showing the query result stage
     * @param results Map that maps every query to its result (relevant documents)
     */
    private void showQueryResult(Map<String, List<Map.Entry<String, Double>>> results){
        Stage resultStage = new Stage();
        resultStage.setWidth(842);
        TableView tableView = new TableView();
        TableColumn<String, ResultQuery> column1 = new TableColumn<>("query ID");
        column1.setCellValueFactory(new PropertyValueFactory<>("queryID"));
        TableColumn<String, ResultQuery> column2 = new TableColumn<>("Document ID");
        column2.setCellValueFactory(new PropertyValueFactory<>("docNO"));
        TableColumn<String, ResultQuery> column3 = new TableColumn<>("Similarity");
        column3.setCellValueFactory(new PropertyValueFactory<>("similarity"));
        TableColumn<String, ResultQuery> column4 = new TableColumn<>("Dominant Entities");
        column4.setCellValueFactory(new PropertyValueFactory<>("dominantEntities"));
        column4.setVisible(false);
        tableView.getColumns().addAll(column1, column2, column3, column4);

        List<Map.Entry<String, List<Map.Entry<String, Double>>>> sorted = new ArrayList<>(results.entrySet());
        Collections.sort(sorted, new Comparator<Map.Entry<String, List<Map.Entry<String, Double>>>>() {
            @Override
            public int compare(Map.Entry<String, List<Map.Entry<String, Double>>> o1, Map.Entry<String, List<Map.Entry<String, Double>>> o2) {
                return o1.getKey().compareTo(o2.getKey());
            }
        });

        for (Map.Entry<String, List<Map.Entry<String, Double>>> entry : sorted){
            String queryID = entry.getKey();
            List<Map.Entry<String, Double>> relevantDocuments = entry.getValue();
            for (Map.Entry<String, Double> document : relevantDocuments){
                String docNO = document.getKey();
                String similarity = document.getValue()+"";
                String dominantEntities = this.manager.getDocDominantEntities(docNO);
                tableView.getItems().add(new ResultQuery(queryID, docNO, similarity, dominantEntities));
            }
        }
        Button saveToFile = new Button();
        saveToFile.setText("Save Results To Disk");
        saveToFile.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                DirectoryChooser directoryChooser = new DirectoryChooser();
                File selectedDir = directoryChooser.showDialog(resultStage);
                Set entrySet = results.entrySet();
                List<Map.Entry<String, List<Map.Entry<String, Double>>>> sorted = new ArrayList<>(entrySet);
                Collections.sort(sorted, new Comparator<Map.Entry<String, List<Map.Entry<String, Double>>>>() {
                    @Override
                    public int compare(Map.Entry<String, List<Map.Entry<String, Double>>> o1, Map.Entry<String, List<Map.Entry<String, Double>>> o2) {
                        return o1.getKey().compareTo(o2.getKey());
                    }
                });
                if (selectedDir != null) {
                    String path = selectedDir.getAbsolutePath()+"\\result.txt";
                    try {
                        BufferedWriter bufferedWriter;
                        if (isResultFileExist)
                            bufferedWriter = new BufferedWriter(new FileWriter(path, true));
                        else {
                            bufferedWriter = new BufferedWriter(new FileWriter(path));
                            isResultFileExist = true;
                        }

                        int iter = 0;
                        for (Map.Entry<String, List<Map.Entry<String, Double>>> entry : sorted){
                            String queryID = entry.getKey();
                            int rank = 1;
                            for (Map.Entry<String, Double> entry1 : entry.getValue()){
                                String docNo = entry1.getKey();
                                Double similarity = entry1.getValue();
                                bufferedWriter.write(queryID+" "+iter+" "+docNo+" "+rank+" "+similarity+" NiSaf");
                                bufferedWriter.newLine();
                                rank++;
                            }
                            iter++;
                        }
                        bufferedWriter.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        CheckBox showEntities = new CheckBox();
        showEntities.setText("Show Entities");
        showEntities.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (showEntities.isSelected())
                    column4.setVisible(true);
                else
                    column4.setVisible(false);
            }
        });
        VBox vBox = new VBox();
        vBox.getChildren().add(saveToFile);
        vBox.getChildren().add(showEntities);
        vBox.getChildren().add(tableView);
        Scene scene = new Scene(vBox);
        resultStage.setScene(scene);
        resultStage.initModality(Modality.APPLICATION_MODAL);
        resultStage.show();
    }

    /**
     * handles the event associated with the 'load_dictionary' Button
     * @param actionEvent an ActionEvent associated with the 'load_dictionary' Button
     */
    public void load_dictionaryAction(ActionEvent actionEvent){
        File dictionaryFile = new File(ConfigReader.INVERTED_DICTIONARY_FILE_PATH);
        if (!dictionaryFile.exists()){
            Alert alert;
            if (ConfigReader.STEMMING)
                alert = new Alert(Alert.AlertType.ERROR, "Can't Locate 'Stemming Dictionary'");
            else
                alert = new Alert(Alert.AlertType.ERROR, "Can't Locate 'No Stemming Dictionary'");
            alert.show();
        }
        else{
            this.manager = new Manager();
            this.manager.loadDictionary();
        }
    }
}