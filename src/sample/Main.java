package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
        primaryStage.setTitle("stern fisherman");
        primaryStage.setScene(new Scene(root, 475, 280));
        primaryStage.alwaysOnTopProperty();
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
