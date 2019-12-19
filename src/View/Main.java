package View;

import Model.Manager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.util.Optional;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Manager manager = new Manager();

        primaryStage.setTitle("Engine");
        primaryStage.setWidth(600);
        primaryStage.setHeight(450);
        FXMLLoader fxmlLoader = new FXMLLoader();
        Parent root = fxmlLoader.load(getClass().getResource("/View/View.fxml").openStream());
        Scene scene = new Scene(root, 600, 450);
        primaryStage.setScene(scene);

        Controller controller = fxmlLoader.getController();
        controller.initialize(primaryStage, manager);
        setStageCloseEvent(primaryStage, manager);

        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }

    /**
     * handles the operation of clicking on 'X'
     * @param primaryStage Application Stage
     * @param manager instance of Manager
     */
    private void setStageCloseEvent(Stage primaryStage, Manager manager) {
        primaryStage.setOnCloseRequest(event -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION,"Are you sure you want to exit?");
            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK){
                // ... user chose OK
                // Close the program properly

                // close the manager
                //model.close();
                //

            } else {
                // ... user chose CANCEL or closed the dialog
                event.consume();
            }
        });
    }
}
