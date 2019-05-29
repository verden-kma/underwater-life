package sample;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import tempBackend.Population;


import static java.lang.Thread.sleep;

public class Controller {

    @FXML
    Label preyLabel;

    @FXML
    Label predatorLabel;

    @FXML
    Button simulationButton;

    @FXML
    Button fishingBegin;

    private volatile boolean fishingBegun;

    private PopulationUpdater pu = new PopulationUpdater();
    private Population population = Population.getInstance();
    Object lock = new Object();

    @FXML
    public void startSimulation(){
        preyLabel.textProperty().bind(pu.messageProperty());
        predatorLabel.textProperty().bind(pu.titleProperty());
        pu.start();
        System.out.println("button");
        simulationButton.setDisable(true);
    }

    @FXML
    public void goFishing(){
        fishingBegun = true;
        synchronized (lock) {
            long newPreyCP = Math.round(population.getCurrentPreyPopulation()*0.9);
            population.setCurrentPreyPopulation(newPreyCP);
            fishingBegun = false;
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            lock.notifyAll();
        }
    }

    private class PopulationUpdater extends Service<Void> {
        @Override
        protected Task createTask(){
            return new Task<Void>(){
                @Override
                protected Void call(){
                        while (true) {
                            synchronized (lock) {
                                try {
                                    if (fishingBegun) lock.wait();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            updateMessage("Prey: " + population.getCurrentPreyPopulation());
                            updateTitle("Predator: " + population.getCurrentPredatorPopulation());
                            population.updatePopulationCount();
                            try {
                                sleep(50);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                    }
                }
            };

        }
        protected void succeeded(){
            preyLabel.textProperty().unbind();
            predatorLabel.textProperty().unbind();
        }
        protected void failed(){
            System.out.println("Failed");
        }


    }

}
