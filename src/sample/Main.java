package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    static Stage stage;
    private static Scene mainScene;
    private static FXMLLoader loader;

    @Override
    public void start(Stage primaryStage) throws Exception{
        stage = primaryStage;
        Parent root = FXMLLoader.load(getClass().getResource("initialScene.fxml"));
        root.setStyle("-fx-background-image: url('" + "/sample/animation/456.jpg" + "'); " +
                "-fx-background-position: center center; " +
                "-fx-background-repeat: stretch;" +
                "-fx-background-size: 100% 100%");
        primaryStage.setTitle("stern fisherman");
        primaryStage.setScene(new Scene(root, 700, 490));
        primaryStage.setResizable(false);
        primaryStage.alwaysOnTopProperty();
        primaryStage.show();
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("sample.fxml"));
        this.loader = loader;
        mainScene = new Scene(loader.load(), 1000, 700);
    }

    static void changeScene() {
        stage.setScene(mainScene);
        ((Controller) loader.getController()).startSimulation();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
