package View;

import Model.Manager;
import Model.communicator.ConfigReader;
import Model.dataTypes.Term;
import Model.preproccesing.Parse;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.*;
import java.util.*;

public class Controller {
    private Stage mainStage;
    private StringProperty corpusPath = new SimpleStringProperty("set path");
    private StringProperty postingPath = new SimpleStringProperty("set path");
    private Manager manager;

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
     * handles the event associated with the 'reset' Button
     * @param actionEvent an ActionEvent associated with the 'reset' Button
     */
    public void resetAction(ActionEvent actionEvent){
        if (postingPath.get().equals("set path")){
            Alert alert = new Alert(Alert.AlertType.WARNING, "Posting Path Not Specified");
            alert.show();
        }
        else {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Are Your Sure?");
            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK) {
                File postingDir = new File(ConfigReader.POSTING_DIR_PATH);
                deleteDirectory(postingDir);
                postingDir.mkdirs();
            }
            this.manager = null;
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
                        //if (Integer.parseInt(corpusTF) == )
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
