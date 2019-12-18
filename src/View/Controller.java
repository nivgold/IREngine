package View;

import Model.Manager;
import Model.communicator.ConfigReader;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;

public class Controller {
    private Stage mainStage;
    private StringProperty corpusPath = new SimpleStringProperty("set path");
    private StringProperty postingPath = new SimpleStringProperty("set path");
    private Manager manager;

    @FXML
    private Button corpus_path_Button;
    @FXML
    private Button posting_path_Button;
    @FXML
    private TextField corpus_path_TextField;
    @FXML
    private TextField posting_path_TextField;



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
}
