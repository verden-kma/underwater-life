package sample;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
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

    @FXML
    Label fishingIndicator;

    private volatile boolean fishingBegun;
    private double NET_CATCH_PERCENT = 0.1;
    private PopulationUpdater pu = new PopulationUpdater();
    private FishingImpact fimp = new FishingImpact();
    private Population population = Population.getInstance();
    Object lock = new Object();

    @FXML
    public void startSimulation(){
        preyLabel.textProperty().bind(Bindings.convert(pu.messageProperty()));
        predatorLabel.textProperty().bind(pu.titleProperty());
        pu.start();
        simulationButton.setDisable(true);
    }

    @FXML
    public void goFishing(){
            fimp.restart();
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
                            updateMessage("Prey: " + population.getCurrentPreyPopulation()/* +"@Predator: "+ population.getCurrentPredatorPopulation()*/);
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

    private class FishingImpact extends Service<Void>{
        @Override
        protected Task createTask(){
            return new Task(){
                @Override
                protected Void call(){
                    fishingBegun = true;
                    synchronized (lock) {
                        long preyCP = population.getCurrentPreyPopulation();
                        //replaces 'null' value of message with current population
                        updateMessage("Prey: " + preyCP);
                        long fishCaught = Math.round(preyCP*NET_CATCH_PERCENT);
                        for (double i = 0.25; i <= 1; i+= 0.25) {
                            try {//sleep assures that rebinding is established
                                sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            population.setCurrentPreyPopulation(Math.round(preyCP - fishCaught * i));
                            updateMessage("Prey: " + population.getCurrentPreyPopulation());
                        }
                        try {
                            sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        fishingBegun = false;
                    }
                 return null;
                }

                protected void running(){
                    fishingIndicator.setText("Fishing begun. 5 seconds for animation.\nDecreasing of population is split into 4 parts.");
                    preyLabel.textProperty().unbind();
                    preyLabel.textProperty().bind(messageProperty());
                }

                protected void succeeded(){
                    preyLabel.textProperty().unbind();
                    preyLabel.textProperty().bind(pu.messageProperty());
                    fishingIndicator.setText("awaiting...");
                    synchronized (lock) {
                        lock.notifyAll();
                    }
                }
                protected void failed(){
                    System.out.println("Exception!");
                }
            };
        }
    }

}
