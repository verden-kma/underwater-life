package sample;

import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;

public class InitialController {

    @FXML
    void launchGame(MouseEvent event) {
        Main.changeScene();
    }

}
