package sample;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import tempBackend.Population;

import java.util.Timer;
import java.util.TimerTask;

import static java.lang.String.valueOf;
import static java.lang.Thread.sleep;

public class Controller {

    @FXML
    private Text moneyLabel;

    @FXML
    private Button startGame;

    @FXML
    private ImageView fishermanImage;

    @FXML
    private ImageView monsterImage;


    @FXML
    Label preyLabel;

    @FXML
    Label predatorLabel;


    @FXML
    Button fishingBegin;

    @FXML
    Label lootLabel;

    @FXML
    Button sellButton;

    @FXML
    Button predatorFishing;

    @FXML
    Button harpoonButton;

    @FXML
    Tooltip harpoonTooltip;

    @FXML
    ProgressBar monsterHealth;

    @FXML
    Button upgradeButton;

    @FXML
    Tooltip upgradeTooltip;

    private volatile boolean pausePU;
    private volatile boolean preyPeak;
    private volatile boolean predPeak;
    private volatile boolean autoNotification;
    private volatile boolean monsterInvokedByFishing;
    private volatile boolean gameOver;

    private SimpleStringProperty preyProperty = new SimpleStringProperty();
    private SimpleStringProperty predatorProperty = new SimpleStringProperty();
    private SimpleStringProperty money = new SimpleStringProperty("5000");
    private SimpleStringProperty lootMessage = new SimpleStringProperty("0");

    private boolean monsterFoodDigested;
    private boolean preyFishing = true;
    private boolean harpoonIsBought;
    private long additionalSleepTime;
    private PopulationUpdater pu = new PopulationUpdater();
    private FishingImpact fimp = new FishingImpact();
    private Monster monster = new Monster();
    private MonsterHunt monsterHunt = new MonsterHunt();
    private Population population = Population.getInstance();
    private final Object populationLock = new Object();
    private final Timer timer = new Timer(true);

    private final String PREY_LOOT_MESSAGE = "Small: ";
    private final String PREDATOR_LOOT_MESSAGE = "Large: ";
    private final String PREY_PROPERTY_PROMPT = "Prey: ";
    private final String PREDATOR_PROPERTY_PROMPT = "Predator: ";
    private final short PREY_PRICE = 1;
    private final short PREDATOR_PRICE = 3;
    private final int HARPOON_PRICE = 2000;
    private final int UPGRADE_PRICE = 1500;
    //minimum % of victim's population that is necessary for profitable fishing
    private final double SUCCESS_GAP = 0.51;
    private final double MONSTER_HEAL = 0.2;
    private final double HARPOON_DAMAGE = 0.3;
    private double NET_CATCH_COEF = 0.1;
    private final double MONSTER_EAT = 0.2;
    private final long PAUSE = 1000;
    private final int FISHING_STEPS = 4;
    private final int MONSTER_STEPS = 5;
    private final long MONSTER_INITIAL_DELAY = 0;//should be 15 000
    private final long PREY_MAX = population.getMaxPopulations().getV1();
    private final long PREDATOR_MAX = population.getMaxPopulations().getV2();

    private long lootCount;  //might be deleted
    private String lootPrompt = ""; //lootCount and lootPrompt


    @FXML
    public void initialize() {
        moneyLabel.textProperty().bind(money);
        lootLabel.textProperty().bind(lootMessage);
        harpoonTooltip.setText(valueOf(HARPOON_PRICE));
        upgradeTooltip.setText(valueOf(UPGRADE_PRICE));
    }

    private TimerTask rentPayment = new TimerTask() {
        @Override
        public void run() {
            synchronized (populationLock) {
                try {
                    while (pausePU) populationLock.wait();
                    if (gameOver) cancel();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            int newValue = Integer.parseInt(money.get()) - 1;
            if (newValue < 0) {
                System.out.println("You Lose!");
                gameOver = true;
                cancel();
            } else Platform.runLater(() -> money.set(valueOf(newValue)));
        }
    };

    private TimerTask monsterDigestion = new TimerTask() {
        @Override
        public void run() {
            if(gameOver) cancel();
            try {
                sleep(additionalSleepTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            additionalSleepTime = 0;
            monsterFoodDigested = true;
        }
    };

    @FXML
    public void startSimulation() {
        preyLabel.textProperty().bind(preyProperty);
        predatorLabel.textProperty().bind(predatorProperty);
        pu.start();
        startGame.setVisible(false);
        timer.schedule(monsterDigestion, MONSTER_INITIAL_DELAY, 10000);
        timer.schedule(rentPayment, 0, 150);
    }

    @FXML
    public void goFishing() {
        preyFishing = true;
        fimp.restart();
    }

    public void fishingForPredator() {
        preyFishing = false;
        fimp.restart();
    }

    @FXML
    private void sellFish() {
        short pricePerFish = preyFishing ? PREY_PRICE : PREDATOR_PRICE;
        money.set(valueOf(Integer.parseInt(money.getValue()) + lootCount * pricePerFish));
        lootMessage.set("0");
    }


    @FXML
    public void upgradeFisherman(){
        money.set(valueOf(Integer.parseInt(money.get())-UPGRADE_PRICE));
        NET_CATCH_COEF = NET_CATCH_COEF*1.5;
        upgradeButton.setVisible(false);
        //TODO: change fisherman's skin
    }

    @FXML
    private void harpoonAction() {
        if (!harpoonIsBought) {
            Platform.runLater(() -> money.set(valueOf(Integer.parseInt(money.get()) - HARPOON_PRICE)));
            harpoonIsBought = true;
            harpoonButton.setText("Attack!");
            harpoonTooltip.setText("hunt monster");
            return;
        }
        monsterHunt.restart();
    }


    private class PopulationUpdater extends Service<Void> {
        @Override
        protected Task<Void> createTask() {
            return new Task<Void>() {
                @Override
                protected Void call() {
                    while (true) {
                        if (gameOver) break;
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
                    return null;
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
                    payForFishing();
                    if (monsterInvokedByFishing){
                        return null;
                    }
                    pausePU = true;
                    synchronized (populationLock) {/*ensures population updates are paused before proceeding*/ }
                    fishing(preyFishing);
                    pausePU = false;
                    additionalSleepTime += FISHING_STEPS * PAUSE;
                    return null;

                }

                private void payForFishing() {
                    int fee = (int) (preyFishing ? PREY_MAX * SUCCESS_GAP * PREY_PRICE * NET_CATCH_COEF : PREDATOR_MAX * SUCCESS_GAP * PREDATOR_PRICE * NET_CATCH_COEF);
                    int newValue = Integer.parseInt(money.get()) - fee;
                    Platform.runLater(() -> money.set(valueOf(newValue)));
                }

                private void fishing(boolean preyFishing) {
                    long CP = preyFishing ? population.getPreyPopulation() : population.getPredatorPopulation();
                    String propertyPrompt = preyFishing ? PREY_PROPERTY_PROMPT : PREDATOR_PROPERTY_PROMPT;
                    lootPrompt = preyFishing ? PREY_LOOT_MESSAGE : PREDATOR_LOOT_MESSAGE;
                    try {
                        sleep(PAUSE);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    for (double i = NET_CATCH_COEF / FISHING_STEPS; i <= NET_CATCH_COEF; i += NET_CATCH_COEF / FISHING_STEPS) {
                        lootCount = Math.round(CP * i);
                        population.setPreyPopulation(Math.round(CP - lootCount));
                        Platform.runLater(() -> {
                            if (preyFishing) {
                                preyProperty.set(propertyPrompt + population.getPreyPopulation());
                            } else {
                                predatorProperty.set(propertyPrompt + population.getPreyPopulation());
                            }
                            lootMessage.set(lootPrompt + lootCount);
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
                    }
                    disableButtons();
                }

                protected void succeeded() {
                    if (!monsterInvokedByFishing) {
                        pausePU = false;
                        synchronized (populationLock) {
                            populationLock.notifyAll();
                        }
                    }
                    enableButtons();
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
                    int healStep = 0;
                    double currentHealth = monsterHealth.getProgress();
                    for (double i = MONSTER_EAT / MONSTER_STEPS; i <= MONSTER_EAT; i += MONSTER_EAT / MONSTER_STEPS) {
                        monsterHealth.setProgress(currentHealth + MONSTER_HEAL / MONSTER_STEPS * ++healStep);
                        System.out.println("Progress: " + monsterHealth.getProgress());
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
                    monsterHealth.setProgress(Math.min(1.0, monsterHealth.getProgress()));
                }

                @Override
                protected void running() {
                    pausePU = true;
                    disableButtons();
                }

                @Override
                protected void succeeded() {
                    enableButtons();
                    additionalSleepTime += MONSTER_STEPS * PAUSE;
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

        private void monsterFisherInteraction(boolean preyIsVictim) {
            //number of steps passed before the meeting of monster and fisherman
            //'2/FISHING_STEPS' because fisherman travel in both ways
            long CP = preyIsVictim ? population.getPreyPopulation() : population.getPredatorPopulation();
            String propertyPrompt = preyIsVictim ? PREY_PROPERTY_PROMPT : PREDATOR_PROPERTY_PROMPT;
            lootPrompt = preyIsVictim ? PREY_LOOT_MESSAGE : PREDATOR_LOOT_MESSAGE;

            double currentHealth = monsterHealth.getProgress();
            double stepsPassedBefore = 1.0 / (1.0 / MONSTER_STEPS + 2.0 / FISHING_STEPS);
            int ispb = (int) stepsPassedBefore;
            int stepCounter = 0;
            long takenBefore = Math.round(CP * MONSTER_EAT * ispb / MONSTER_STEPS + CP * NET_CATCH_COEF * ispb / FISHING_STEPS);
            for (double i = 1.0 / ispb; i <= ispb; i += 1.0 / ispb) {
                try {
                    sleep(PAUSE);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                monsterFisherPredation(preyIsVictim, CP, takenBefore, i, ispb, propertyPrompt, ++stepCounter, currentHealth);
            }
            long sleepReminder = sleepBeforeMeeting(stepsPassedBefore, ispb);
            //monster eats all the fishes in fisherman's nets
            lootCount = 0;
            Platform.runLater(() -> lootMessage.set(lootPrompt + lootCount));
            long takenAfter = sleepAfterMeeting(CP, ispb, sleepReminder) - lootCount;
            monsterHealth.setProgress(currentHealth * 1.05);
//            recalculate current population
            long refreshedCP = preyIsVictim ? population.getPreyPopulation() : population.getPredatorPopulation();
            for (double i = 1.0 / (MONSTER_STEPS - ispb + 1); i < 1.0; i += 1.0 / (MONSTER_STEPS - ispb + 1)) {
                monsterFisherPredation(preyIsVictim, refreshedCP, takenAfter, i, 0, propertyPrompt, ++stepCounter, currentHealth);
            }
            monsterHealth.setProgress(Math.min(1.0, monsterHealth.getProgress()));
        }
    }

    private void monsterFisherPredation(boolean preyIsVictim, long CP, long taken, double i, int ispb, String propertyPrompt, int stepCount, double health) {
        if (preyIsVictim) population.setPreyPopulation(CP - Math.round(taken * i));
        else population.setPredatorPopulation(CP - Math.round(taken * i));
        lootCount = Math.round(CP * NET_CATCH_COEF * ispb / FISHING_STEPS * i);
        monsterHealth.setProgress(health + MONSTER_HEAL / MONSTER_STEPS * stepCount);
        System.out.println("MonsterFisher: " + monsterHealth.getProgress());
        Platform.runLater(() -> {
            if (preyIsVictim) preyProperty.set(propertyPrompt + population.getPreyPopulation());
            else predatorProperty.set(propertyPrompt + population.getPredatorPopulation());
            lootMessage.set(lootPrompt + lootCount);
        });
        try {
            sleep(PAUSE);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private long sleepBeforeMeeting(double stepsPassedBefore, int ispb) {
        long sleepReminder = Math.round((1.0 - (stepsPassedBefore - ispb)) * PAUSE);
        try {
            sleep(Math.round((stepsPassedBefore - ispb) * PAUSE));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Meet now.");
        return sleepReminder;
    }

    //hunting after monster is only possible when it is near to the surface (preyPeak)
    private class MonsterHunt extends Service<Void> {
        protected Task createTask() {
            return new Task() {
                protected Void call() {
                    long CP = population.getPreyPopulation();
                    double stepsPassedBefore = 1.0 / (1.0 / MONSTER_STEPS + 2.0 / FISHING_STEPS);
                    int ispb = (int) stepsPassedBefore;
                    int stepCounter = 0;
                    double currentHealth = monsterHealth.getProgress();
                    long taken = Math.round(CP * MONSTER_EAT * ispb / MONSTER_STEPS);
                    try {
                        sleep(PAUSE);
                    } catch (InterruptedException e) {
                        System.out.println("MonsterHunt canceled!");
                        return null;
                    }
                    System.out.println("after Exception");
                    for (double i = 1.0 / ispb; i <= ispb; i += 1.0 / ispb) {
                        monsterFisherPredation(true, CP, taken, i, 0, PREY_LOOT_MESSAGE, ++stepCounter, currentHealth);
                    }
                    sleepBeforeMeeting(stepsPassedBefore, ispb);
                    System.out.println("Harpoon!");
                    Platform.runLater(() -> monsterHealth.setProgress(Math.max(0, monsterHealth.getProgress()-HARPOON_DAMAGE)));
                    try {//let runLater code to execute
                        sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (monsterHealth.getProgress() == 0){
                        System.out.println("You win!");
                        gameOver = true;
                    }
                    return null;
                }
            };
        }

        public void running() {
            if (!preyPeak) {
                monsterHunt.cancel();
                return;
            }
            pausePU = true;
            disableButtons();
        }

        protected void succeeded() {
            additionalSleepTime += MONSTER_STEPS * PAUSE;
            monsterFoodDigested = false;
            pausePU = false;
            harpoonButton.setDisable(false);
            synchronized (populationLock) {
                populationLock.notifyAll();
            }
            enableButtons();
        }

        protected void cancelled(){
            System.out.println("Entered");
            pausePU = false;
            harpoonButton.setDisable(false);
            synchronized (populationLock) {
                populationLock.notifyAll();
            }
            enableButtons();
        }

        protected void failed() {
            System.out.println("Monster hunting failed!");
        }
    }

    private void disableButtons(){
        fishingBegin.setDisable(true);
        predatorFishing.setDisable(true);
        harpoonButton.setDisable(true);
        sellButton.setDisable(true);
    }

    private void enableButtons(){
        fishingBegin.setDisable(false);
        predatorFishing.setDisable(false);
        harpoonButton.setDisable(false);
        sellButton.setDisable(false);
    }

}

