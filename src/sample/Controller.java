package sample;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import tempBackend.Population;

import java.util.Timer;
import java.util.TimerTask;

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

    @FXML
    Label lootLabel;

    @FXML
    Label moneyLabel;

    private volatile boolean pausePU;
    private volatile boolean preyPeak;
    private volatile boolean predPeak;
    private volatile boolean autoNotification;
    private volatile boolean monsterInvokedByFishing;
    private int smallLootCount, largeLootCount;

    private SimpleStringProperty preyProperty = new SimpleStringProperty();
    private SimpleStringProperty predatorProperty = new SimpleStringProperty();
    private SimpleStringProperty money = new SimpleStringProperty();
    private SimpleStringProperty preyLootMessage = new SimpleStringProperty("Small: " + smallLootCount);
    private SimpleStringProperty predatorLootMessage = new SimpleStringProperty("Large: " + largeLootCount);

    private double NET_CATCH_COEF = 0.1;
    private final double MONSTER_EAT = 0.2;
    private PopulationUpdater pu = new PopulationUpdater();
    private FishingImpact fimp = new FishingImpact();
    private Monster monster = new Monster();
    private Population population = Population.getInstance();
    private final long PREY_MAX = population.getMaxPopulations().getV1();
    private final long PREDATOR_MAX = population.getMaxPopulations().getV2();
    private final Object populationLock = new Object();
    private final Timer timer = new Timer(true);
    private boolean monsterFoodDigested;
    private boolean preyFishing = true;
    private final long PAUSE = 1000;
    private final int FISHING_STEPS = 4;
    private final int MONSTER_STEPS = 5;

    public void initialize() {
        timer.schedule(timerTask, 15000, 10000);
    }

    private TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            monsterFoodDigested = true;
        }
    };

    @FXML
    public void startSimulation() {
        preyLabel.textProperty().bind(preyProperty);
        predatorLabel.textProperty().bind(predatorProperty);
        pu.start();
        simulationButton.setDisable(true);
        //TODO: bind economics labels to fishingImpact
    }

    @FXML
    public void goFishing() {
        fimp.restart();
    }

    private class PopulationUpdater extends Service<Void> {
        @Override
        protected Task<Void> createTask() {
            return new Task<Void>() {
                @Override
                protected Void call() {
                    while (true) {
                        synchronized (populationLock) {
                            try {
                                if (pausePU) {
                                    populationLock.wait();
                                    continue;
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        population.updatePopulationCount();
                        long preyCP = population.getPreyPopulation();
                        long predCP = population.getPredatorPopulation();


                        //pred control
                        if (population.getNextPredatorUpdate() == predCP && predCP < PREDATOR_MAX * 0.25 && predCP > 272/*min population*/) {
                            predCP *= 0.98;
                            population.setPredatorPopulation(predCP);
                        }
                        long eventualPredP = predCP;
                        Platform.runLater(() -> {
                            preyProperty.set("Prey: " + preyCP);
                            predatorProperty.set("Predator: " + eventualPredP);
                        });


                        if (population.getPreyPopulation() > PREY_MAX * 0.9) {
                            //TODO: added for testing purposes
                            if (!preyPeak) System.out.println("PreyPeak!");
                            preyPeak = true;
                            if (preyCP > PREY_MAX * 0.99) {
                                wakeMonster();
                            }
                        } else preyPeak = false;
                        if (population.getPredatorPopulation() > PREDATOR_MAX * 0.9) {
                            if (!predPeak) System.out.println("PredatorPeak!");
                            predPeak = true;
                            if (predCP > PREDATOR_MAX * 0.99) {
                                wakeMonster();
                            }
                        } else predPeak = false;

                        try {
                            sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
        }

        protected void failed() {
            System.out.println("Population Failed");
        }

        private void wakeMonster() {
            autoNotification = true;
            pausePU = true;
            Platform.runLater(() -> monster.restart());
        }
    }

    private class FishingImpact extends Service<Void> {
        @Override
        protected Task<Void> createTask() {
            return new Task<Void>() {
                @Override
                protected Void call() {
                    try {//wait for monsterInvokedByFishing variable update
                        sleep(100);
                    } catch (InterruptedException e) {
                        System.out.println("monsterInvokedByFishing updated");
                    }
                    if (monsterInvokedByFishing) return null;
                    //TODO: check if it truly returns
                    pausePU = true;
                    synchronized (populationLock) {/*ensures population updates are paused before proceeding*/ }
                    fishing(preyFishing);

                    pausePU = false;
                    //synchronized }
                    return null;

                }

                private void fishing(boolean preyFishing) {
                    long CP = preyFishing ? population.getPreyPopulation() : population.getPredatorPopulation();
                    String propertyPrompt = preyFishing ? "Prey: " : "Predator: ";
                    String lootPrompt = preyFishing ? "Small: " : "Large: ";
                    //how many fishes are in fisherman's nets at the moment
                    long loot;
                    try {
                        sleep(PAUSE);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    for (double i = NET_CATCH_COEF / FISHING_STEPS; i <= NET_CATCH_COEF; i += NET_CATCH_COEF / FISHING_STEPS) {
                        loot = Math.round(CP * i);
                        population.setPreyPopulation(Math.round(CP - loot));
                        long effectivelyFinalLoot = loot;
                        Platform.runLater(() -> {
                            if (preyFishing) {
                                preyLootMessage.set(lootPrompt + effectivelyFinalLoot);
                                preyProperty.set(propertyPrompt + population.getPreyPopulation());
                            } else {
                                predatorLootMessage.set(lootPrompt + effectivelyFinalLoot);
                                predatorProperty.set(propertyPrompt + population.getPreyPopulation());
                            }
                        });
                        try {
                            sleep(PAUSE);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

                protected void running() {
                    if (((preyPeak && preyFishing) || (predPeak && !preyFishing)) && monsterFoodDigested) {
                        monster.restart();
                        monsterInvokedByFishing = true;
                        cancel();
                        return;
                    }
                }

                protected void succeeded() {
                    fishingIndicator.setText("awaiting...");
                    pausePU = false;
                    synchronized (populationLock) {
                        populationLock.notifyAll();
                    }
                }

                protected void failed() {
                    System.out.println("Fishing Exception!");
                }
            };
        }
    }

    private class Monster extends Service<Void> {
        @Override
        protected Task createTask() {
            return new Task() {
                @Override
                protected Void call() {
                    if (!monsterFoodDigested) return null;
                    if (preyPeak) {
                        if (monsterInvokedByFishing) {
                            System.out.println("Testing prey fishing monster");
                            monsterFisherInteraction(true);
                            return null;
                        }
                        monsterPredation(true);
                    } else if (predPeak) {
                        if (monsterInvokedByFishing) {
                            System.out.println("Testing predator fishing monster");
                            monsterFisherInteraction(false);
                            return null;
                        }
                        monsterPredation(false);
                    }
                    try {//TODO: delete this sleep when the game is ready
                        sleep(2 * PAUSE);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (autoNotification) autoNotification = false;
                    return null;
                }

                private void monsterPredation(boolean preyEating) {
                    long CP = preyEating ? population.getPreyPopulation() : population.getPredatorPopulation();
                    for (double i = MONSTER_EAT / MONSTER_STEPS; i <= MONSTER_EAT; i += MONSTER_EAT / MONSTER_STEPS) {
                        try {
                            sleep(PAUSE);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (preyEating) population.setPreyPopulation(Math.round(CP - CP * i));
                        else population.setPredatorPopulation(Math.round(CP - CP * i));
                        Platform.runLater(() -> {
                            if (preyEating) preyProperty.set("Prey: " + population.getPreyPopulation());
                            else predatorProperty.set("Predator: " + population.getPredatorPopulation());
                        });
                    }
                }

                @Override
                protected void running() {
                    pausePU = true;
                }

                @Override
                protected void succeeded() {
                    if (preyPeak) {
                        preyPeak = false;
                    } else if (predPeak) {
                        predPeak = false;
                    }
                    monsterFoodDigested = false;
                    pausePU = false;
                    synchronized (populationLock) {
                        populationLock.notifyAll();
                    }
                    monsterInvokedByFishing = false;
                }

                @Override
                protected void failed() {
                    System.out.println("Monster fail");
                }
            };
        }

        private long sleepAfterMeeting(long predatorCP, int ispb, long sleepReminder) {
            try {
                sleep(sleepReminder);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return Math.round(predatorCP * MONSTER_EAT * (MONSTER_STEPS - ispb + 1) / MONSTER_STEPS);

        }

        private long sleepBeforeMeeting(double stepsPassedBefore, int ispb) {
            long sleepReminder = Math.round((1.0 - (stepsPassedBefore - ispb)) * PAUSE);
            try {
                sleep(Math.round((stepsPassedBefore - ispb) * PAUSE));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Met now.");
            return sleepReminder;
        }

        private void monsterFisherInteraction(boolean preyIsVictim) {
            //number of steps passed before the meeting of monster and fisherman
            //'2/FISHING_STEPS' because fisherman travel in both ways
            long CP = preyIsVictim ? population.getPreyPopulation() : population.getPredatorPopulation();
            String propertyPrompt = preyIsVictim ? "Prey: " : "Predator: ";
            String lootPrompt = preyIsVictim ? "Small: " : "Large: ";

            double stepsPassedBefore = 1.0 / (1.0 / MONSTER_STEPS + 2.0 / FISHING_STEPS);
            int ispb = (int) stepsPassedBefore;
            long takenBefore = Math.round(CP * MONSTER_EAT * ispb / MONSTER_STEPS + CP * NET_CATCH_COEF * ispb / FISHING_STEPS);
            for (double i = 1.0 / ispb; i <= ispb; i += 1.0 / ispb) {
                if (preyIsVictim) population.setPreyPopulation(CP - Math.round(takenBefore * i));
                else population.setPredatorPopulation(CP - Math.round(takenBefore * i));
                long loot = Math.round(CP * NET_CATCH_COEF * ispb / FISHING_STEPS * i);
                Platform.runLater(() -> {
                    if (preyIsVictim) {
                        preyLootMessage.set(lootPrompt + loot);
                        preyProperty.set(propertyPrompt + population.getPreyPopulation());
                    } else {
                        predatorLootMessage.set(lootPrompt + loot);
                        predatorProperty.set(propertyPrompt + population.getPredatorPopulation());
                    }
                });
                try {
                    sleep(PAUSE);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            long sleepReminder = sleepBeforeMeeting(stepsPassedBefore, ispb);
            //TODO: monster eats all the fish in fisherman's nets
            Platform.runLater(() -> {
                if (preyIsVictim) preyLootMessage.set(lootPrompt + 0);
                else predatorLootMessage.set(lootPrompt + 0);
            });
            //TODO: takenAfter -= fishermanLoot
            long takenAfter = sleepAfterMeeting(CP, ispb, sleepReminder);
            long refreshedCP = preyIsVictim ? population.getPreyPopulation() : population.getPredatorPopulation();
            for (double i = 1.0 / (MONSTER_STEPS - ispb + 1); i <= 1.0; i += 1.0 / (MONSTER_STEPS - ispb + 1)) {
                //recalculate current population
                if (preyIsVictim) population.setPreyPopulation(refreshedCP - Math.round(takenAfter * i));
                else population.setPredatorPopulation(refreshedCP - Math.round(takenAfter * i));
                Platform.runLater(() -> {
                    if (preyIsVictim) preyProperty.set(propertyPrompt + population.getPreyPopulation());
                    else predatorProperty.set(propertyPrompt + population.getPredatorPopulation());
                });
                try {
                    sleep(PAUSE);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }

}
