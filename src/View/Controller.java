package View;

import Model.Manager;
import Model.communicator.ConfigReader;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.*;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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



    public void initialize(Stage mainStage, Manager manager){
        this.mainStage = mainStage;
        this.manager = manager;
        corpus_path_TextField.textProperty().bind(Bindings.format("%s", this.corpusPath));
        posting_path_TextField.textProperty().bind(Bindings.format("%s", this.postingPath));
    }

    public void corpus_pathAction(ActionEvent actionEvent){
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDir = directoryChooser.showDialog(mainStage);
        if (selectedDir == null){
            System.out.println("not valid directory");
        }
        else{
            corpusPath.setValue(selectedDir.getAbsolutePath());
            // update the config file
            ConfigReader.updateCorpusPath(selectedDir.getAbsolutePath());
        }
    }

    public void posting_pathAction(ActionEvent actionEvent){
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDir = directoryChooser.showDialog(mainStage);
        if (selectedDir == null){
            System.out.println("not valid directory");
        }
        else{
            postingPath.setValue(selectedDir.getAbsolutePath());
            ConfigReader.updatePostingPath(selectedDir.getAbsolutePath());
        }
    }

    public void startAction(ActionEvent actionEvent){
        if (corpusPath.get().equals("set path") || postingPath.get().equals("set path")){
            // pop a warning dialog
            Alert alert = new Alert(Alert.AlertType.WARNING, "not all parameters were set");
            alert.show();
        }
        else{
            // start process
            this.manager.startProcess();
        }
    }

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
                manager.cleanRAM();
            }
        }
    }

    private boolean deleteDirectory(File directory){
        File[] files = directory.listFiles();
        if (files != null){
            for (File file : files)
                deleteDirectory(file);
        }
        return directory.delete();
    }

    public void showDictionaryButton(ActionEvent actionEvent){
        ConfigReader.loadConfiguration();
        File dictionary = new File(ConfigReader.INVERTED_DICTIONARY_FILE_PATH);
        if (!dictionary.exists()){
            Alert alert = new Alert(Alert.AlertType.ERROR, "No Dictionary Found In Posting Path");
            alert.show();
        }
        else{

            // show dictionary

            Stage dialogStage = new Stage();
            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
            StringBuilder stringBuilder = new StringBuilder();

            try{
                BufferedReader bufferedReader = new BufferedReader(new FileReader(ConfigReader.INVERTED_DICTIONARY_FILE_PATH));
                String currentLine = "";
                while ((currentLine = bufferedReader.readLine()) != null){
                    stringBuilder.append(currentLine+"\n");
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("XXX");
            TableView tableView = new TableView<>();
            TableColumn<String, String> termValue = new TableColumn<>("Term");

            TableColumn<String, String> corpusTF = new TableColumn<>("TF");
            tableView.getColumns().add(termValue);
            tableView.getColumns().add(corpusTF);


            scrollPane.setContent(tableView);
            Scene scene = new Scene(scrollPane, 200, 1000);
            dialogStage.setScene(scene);
            dialogStage.show();
        }
    }
}
