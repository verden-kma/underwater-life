package sample;

import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import tempBackend.Population;

import java.util.Timer;
import java.util.TimerTask;

import static java.lang.Thread.sleep;
@SuppressWarnings("Duplicates")
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

    private volatile boolean pausePU;
    private volatile boolean preyPeak;
    private volatile boolean predPeak;
    private volatile boolean autoNotification;
   // private volatile boolean fishingIsInvoker;
    private volatile boolean monsterInvokedByFishing;

    private double NET_CATCH_COEF = 0.1;
    private final double MONSTER_EAT = 0.2;
    private PopulationUpdater pu = new PopulationUpdater();
    private FishingImpact fimp = new FishingImpact();
    private Monster monster = new Monster();
    private Population population = Population.getInstance();    //?
    private final long PREY_MAX = population.getMaxPopulations().getV1();
    private final long PREDATOR_MAX = population.getMaxPopulations().getV2();
    final Object populationLock = new Object();
    private final Timer timer = new Timer();
    private boolean monsterFoodDigested;
    private final long MONSTER_SLEEP = 00000;
    private boolean preyFishing = true;
    private final long PAUSE = 1000;
    private final int FISHING_STEPS = 4;
    private final int MONSTER_STEPS = 5;

    public void initialize(){
        timer.schedule(timerTask, 0);
    }

    private TimerTask timerTask = new TimerTask(){
        @Override
        public void run(){
            while(true){
                try {
                    Thread.sleep(MONSTER_SLEEP);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                monsterFoodDigested = true;
            }
        }
    };

    @FXML
    public void startSimulation() {
        preyLabel.textProperty().bind(pu.messageProperty());
        predatorLabel.textProperty().bind(pu.titleProperty());
        pu.start();
        simulationButton.setDisable(true);
    }

    @FXML
    public void goFishing() {
        fimp.restart();
    }

    private class PopulationUpdater extends Service<Void> {
        @Override
        protected Task createTask() {
            return new Task<Void>() {
                @Override
                protected Void call() {
                    while (true) {
                        synchronized (populationLock) {
                            try {
                                if (pausePU){ populationLock.wait();
                                continue;}
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        population.updatePopulationCount();
                        long preyCP = population.getPreyPopulation();
                        long predCP = population.getPredatorPopulation();
                        updateMessage("Prey: " + preyCP);
                        updateTitle("Predator: " + predCP);
                        //pred control
                        if (population.getNextPredatorUpdate() == predCP && predCP < PREDATOR_MAX*0.25 && predCP > 272/*min population*/){
                            predCP *= 0.98;
                            population.setPredatorPopulation(predCP);
                            updateTitle("Predator: " + predCP);
                        }

                        if (population.getPreyPopulation() > PREY_MAX * 0.9) {
                            //TODO: added for testing purposes
                            if(!preyPeak) System.out.println("PreyPeak!");
                            preyPeak = true;
                            if (preyCP > PREY_MAX * 0.99) {
                                wakeMonster();
                            }
                        }else preyPeak = false;
                        if (population.getPredatorPopulation() > PREDATOR_MAX * 0.9) {
                            if(!predPeak) System.out.println("PredatorPeak!");
                            predPeak = true;
                            if (predCP > PREDATOR_MAX * 0.99) {
                                wakeMonster();
                            }
                        }else predPeak = false;

                        try {
                            sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
        }


        protected void succeeded() {
            preyLabel.textProperty().unbind();
            predatorLabel.textProperty().unbind();
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
        protected Task createTask() {
            return new Task() {
                @Override
                protected Void call() {
                    //TODO: think about this stupidity
                    try {//wait for monsterInvokedByFishing variable update
                        sleep(100);
                    } catch (InterruptedException e) {
                        System.out.println("monsterInvokedByFishing updated");
                    }
                    if (monsterInvokedByFishing) { return null;}
                    else {
                        pausePU = true;
                        synchronized (populationLock) {
                        }

                        if (preyFishing) {
                            long preyCP = population.getPreyPopulation();
                            updateMessage("Prey: " + preyCP);
                            for (double i = NET_CATCH_COEF / FISHING_STEPS; i <= NET_CATCH_COEF; i += NET_CATCH_COEF / FISHING_STEPS) {
                                try {//sleep assures that rebinding is established and monster checked its invoker
                                    sleep(PAUSE);
                                } catch (InterruptedException e) {
                                    System.out.println("Fishing InterruptedException!");
                                    cancel();
                                }
                                population.setPreyPopulation(Math.round(preyCP - preyCP * i));
                                updateMessage("Prey: " + population.getPreyPopulation());
                            }
                        } else {
                            long predCP = population.getPredatorPopulation();
                            updateMessage("Predator: " + predCP);
                            for (double i = NET_CATCH_COEF / FISHING_STEPS; i <= NET_CATCH_COEF; i += NET_CATCH_COEF / FISHING_STEPS) {
                                try {//sleep assures that rebinding is established and monster checked its invoker
                                    sleep(PAUSE);
                                } catch (InterruptedException e) {
                                    System.out.println("Fishing InterruptedException!");
                                    cancel();
                                }
                                population.setPredatorPopulation(Math.round(predCP - predCP * i));
                                updateMessage("Predator: " + population.getPredatorPopulation());
                            }
                        }
                        try {
                            sleep(PAUSE);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        pausePU = false;
                        //synchronized }
                        return null;
                    }
                }

                protected void running() {
                    if (((preyPeak && preyFishing) || (predPeak && !preyFishing)) && monsterFoodDigested){
                        monster.restart();
                        monsterInvokedByFishing = true;
                        //fishingIsInvoker = true;
                        cancel();
                        return;
                    }
                    preyLabel.textProperty().unbind();
                    preyLabel.textProperty().bind(messageProperty());
                }

                protected void succeeded() {
                    if (!monsterInvokedByFishing){
                        monsterInvokedByFishing = false;
                        return;
                    }
                    preyLabel.textProperty().unbind();
                    preyLabel.textProperty().bind(pu.messageProperty());
                    fishingIndicator.setText("awaiting...");
                    synchronized (populationLock) {
                        populationLock.notifyAll();
                    }
                }

                protected void failed() {
                    System.out.println("Exception!");
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
                                if (monsterInvokedByFishing){
                                    long preyCP = population.getPreyPopulation();
                                    updateMessage("Prey: " + preyCP);
                                    System.out.println("Testing prey fishing monster");
                                    //monsterFisherInteraction();
                                    //cannot get rid of duplication because 'updateMessage()' is protected
                                    //

                                    double stepsPassedBefore = 1.0 / (1.0 / MONSTER_STEPS + 2.0 / FISHING_STEPS);

                                    int ispb = (int) stepsPassedBefore;
                                    long takenBefore = Math.round(preyCP*MONSTER_EAT*ispb/MONSTER_STEPS + preyCP*NET_CATCH_COEF*ispb/FISHING_STEPS);
                                    for(double i = 1/ispb; i <= ispb; i += 1/ispb){
                                        try {
                                            sleep(PAUSE);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        population.setPreyPopulation(preyCP - Math.round(takenBefore*i));
                                        updateMessage("Prey: " + population.getPreyPopulation());
                                    }
                                    long sleepReminder = Math.round((1.0 - (stepsPassedBefore - ispb))*PAUSE);
                                    try {
                                        sleep(Math.round((stepsPassedBefore - ispb)*PAUSE));
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    System.out.println("Met now.");
                                    //TODO: monster eats all the fish in fisherman's nets
                                    //TODO: takenAfter -= fishermanLoot
                                    try {
                                        sleep(sleepReminder);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    long takenAfter = Math.round(preyCP*MONSTER_EAT*(MONSTER_STEPS-ispb+1)/MONSTER_STEPS);
                                    preyCP = population.getPreyPopulation();
                                    for (double i = 1.0/(MONSTER_STEPS-ispb+1); i <= 1.0 ; i += 1.0/(MONSTER_STEPS-ispb+1)){
                                        //recalculate current population
                                        population.setPreyPopulation(preyCP - Math.round(takenAfter*i));
                                        updateMessage("Prey: " + population.getPreyPopulation());
                                        try {
                                            sleep(PAUSE);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    //
                                    return null;
                                }

                                long preyCP = population.getPreyPopulation();
                                updateMessage("Prey: " + preyCP);
                                for (double i = MONSTER_EAT/MONSTER_STEPS; i <= MONSTER_EAT; i += MONSTER_EAT/MONSTER_STEPS) {
                                    try {
                                        sleep(PAUSE);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    population.setPreyPopulation(Math.round(preyCP - preyCP * i));
                                    updateMessage("Prey: " + population.getPreyPopulation());
                                }

                            } else if (predPeak) {
                                if (monsterInvokedByFishing){
                                    long predatorCP = population.getPredatorPopulation();
                                    updateMessage("Predator: " + predatorCP);
                                    System.out.println("Testing predator fishing monster");
                                    //monsterFisherInteraction();
                                    //cannot get rid of duplication because 'updateMessage()' is protected
                                    //

                                    double stepsPassedBefore = 1.0 / (1.0 / MONSTER_STEPS + 2.0 / FISHING_STEPS);

                                    int ispb = (int) stepsPassedBefore;
                                    long takenBefore = Math.round(predatorCP*MONSTER_EAT*ispb/MONSTER_STEPS + predatorCP*NET_CATCH_COEF*ispb/FISHING_STEPS);
                                    for(double i = 1/ispb; i <= ispb; i += 1/ispb){
                                        try {
                                            sleep(PAUSE);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        population.setPredatorPopulation(predatorCP - Math.round(takenBefore*i));
                                        updateMessage("Predator: " + population.getPredatorPopulation());
                                    }
                                    long sleepReminder = Math.round((1.0 - (stepsPassedBefore - ispb))*PAUSE);
                                    try {
                                        sleep(Math.round((stepsPassedBefore - ispb)*PAUSE));
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    System.out.println("Met now.");
                                    //TODO: monster eats all the fish in fisherman's nets
                                    //takenAfter -= fishermanLoot
                                    try {
                                        sleep(sleepReminder);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    long takenAfter = Math.round(predatorCP*MONSTER_EAT*(MONSTER_STEPS-ispb+1)/MONSTER_STEPS);
                                    predatorCP = population.getPredatorPopulation();
                                    for (double i = 1.0/(MONSTER_STEPS-ispb+1); i <= 1.0 ; i += 1.0/(MONSTER_STEPS-ispb+1)){
                                        //recalculate current population
                                        population.setPredatorPopulation(predatorCP - Math.round(takenAfter*i));
                                        updateMessage("Predator: " + population.getPredatorPopulation());
                                        try {
                                            sleep(PAUSE);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    //
                                }
                                long predCP = population.getPredatorPopulation();
                                updateMessage("Predator: " + predCP);
                                for (double i = MONSTER_EAT/MONSTER_STEPS; i <= MONSTER_EAT; i += MONSTER_EAT/MONSTER_STEPS) {
                                    try {
                                        sleep(PAUSE);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    population.setPredatorPopulation(Math.round(predCP - predCP * i));
                                    updateMessage("Predator: " + population.getPredatorPopulation());
                                }
                            }
                    try {//TODO: delete this sleep when the game is ready
                        sleep(2*PAUSE);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                            if (autoNotification) autoNotification = false;
                            return null;
                }

                @Override
                protected void running() {
                    pausePU = true;
                    if (preyPeak) {
                        preyLabel.textProperty().unbind();
                        preyLabel.textProperty().bind(monster.messageProperty());
                    } else if (predPeak) {
                        predatorLabel.textProperty().unbind();
                        predatorLabel.textProperty().bind(monster.messageProperty());
                    }
                }

                @Override
                protected void succeeded() {
                    if (preyPeak) {
                        preyPeak = false;
                        preyLabel.textProperty().unbind();
                        preyLabel.textProperty().bind(pu.messageProperty());
                    } else if (predPeak) {
                        predPeak = false;
                        predatorLabel.textProperty().unbind();
                        predatorLabel.textProperty().bind(pu.titleProperty());
                    }
                    monsterFoodDigested = false;
                    synchronized (populationLock) {
                        pausePU = false;
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

/*        private void monsterFisherInteraction(){
            //number of steps passed before the meeting of monster and fisherman
            //'2/FISHING_STEPS' because fisherman travel in both ways
            int stepsPassedBefore = 1/(1/MONSTER_STEPS + 2/FISHING_STEPS);
            long victims;
            String prompt;
            if (preyPeak){
                victims = population.getPreyPopulation();
                prompt = "Prey: ";
            }
            else {
                victims = population.getPredatorPopulation();
                prompt = "Predator: ";
            }
            long taken = Math.round(victims*MONSTER_EAT + victims*NET_CATCH_COEF);
            for(double i = 1/stepsPassedBefore; i <= stepsPassedBefore; i += 1/stepsPassedBefore){
                long newPopulation = victims - Math.round(taken*i);
                updateMessage()
            }
        }*/
    }

}
