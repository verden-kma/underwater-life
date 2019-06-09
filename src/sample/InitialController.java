package sample;

import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;

public class InitialController {

    @FXML
    private void launchGame(MouseEvent event) {
        Main.changeScene(0);
    }

    @FXML
    private void lunchLevel_1(){
        Main.changeScene(1);
    }

    @FXML
    private void lunchLevel_2(){
        Main.changeScene(2);
    }

    @FXML
    private void lunchLevel_3(){
        Main.changeScene(3);
    }

    @FXML
    private void lunchLevel_4(){
        Main.changeScene(4);
    }


}
