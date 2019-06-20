package sample;

import javafx.application.Application;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {
    static Stage stage;
    private static Scene mainScene;
    private static FXMLLoader loader;

    @Override
    public void start(Stage primaryStage) throws Exception{
        Image icon = new Image("/sample/animation/icon.png");
        primaryStage.getIcons().add(icon);
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
        //mainScene = new Scene(loader.load(), 1000, 700);
    }

    static void changeScene(int level) {
        try {
            mainScene = new Scene(loader.load(), 1000, 700);
        } catch (IOException e) {
            e.printStackTrace();
        }

        stage.setScene(mainScene);
        if (level == 0) ((Controller) loader.getController()).startSimulation();
        else if (level == 1) ((Controller) loader.getController()).startSimulation(130, 8000,  5000, "1500");
        else if (level == 2) ((Controller) loader.getController()).startSimulation(110, 6000,  4000, "2000");
        else if (level == 3) ((Controller) loader.getController()).startSimulation(90, 4000,  3000, "2500");
        else if (level == 4) ((Controller) loader.getController()).startSimulation(75, 2000,  2000, "3000");
    }


    public static void main(String[] args) {
        final JFXPanel fxPanel = new JFXPanel();
        launch(args);
    }
}
