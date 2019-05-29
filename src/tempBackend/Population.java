package tempBackend;


public class Population {

    //we only have and work with one instance of this class because we have only one lake/river
    private Population(){}

    private static Population instance = new Population();

    public static Population getInstance(){
        return instance;
    }

    private long currentPreyPopulation = 4000;
    private long currentPredatorPopulation = 1000;

    private final double  preyGC = 5e-3;
    private final double  preyDC = 5e-6;
    private final double  predatorGC = 2.5e-6;
    private final double  predatorDC = 5e-3;

    public void updatePopulationCount(){
        long preyCP = currentPreyPopulation;
        long predCP = currentPredatorPopulation;
        currentPreyPopulation = Math.round(preyCP + preyGC*preyCP - preyDC*preyCP*predCP);
        currentPredatorPopulation = Math.round(predCP - predatorDC*predCP + predatorGC*preyCP*predCP);
    }

    public long getCurrentPreyPopulation() {
        return currentPreyPopulation;
    }

    public long getCurrentPredatorPopulation() {
        return currentPredatorPopulation;
    }

    public void setCurrentPreyPopulation(long currentPreyPopulation) {
        this.currentPreyPopulation = currentPreyPopulation;
    }

    public void setCurrentPredatorPopulation(long currentPredatorPopulation) {
        this.currentPredatorPopulation = currentPredatorPopulation;
    }

    public String toString(){
        return "Prey: "+ currentPreyPopulation+"\n"+
                "Predator: "+ currentPredatorPopulation+"\n";
    }
}
