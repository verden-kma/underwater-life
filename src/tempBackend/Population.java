package tempBackend;


public class Population {

    //we only have and work with one instance of this class because we have only one lake/river
    private Population(){}

    private static Population instance = new Population();

    public static Population getInstance(){
        return instance;
    }

    private long preyPopulation = 4000;
    private long predatorPopulation = /*2000*/300;

    private final double  preyGC = 5e-3;
    private final double  preyDC = 5e-6;
    private final double  predatorGC = 2.5e-6;
    private final double  predatorDC = 5e-3;

    public void updatePopulationCount(){
        long preyCP = preyPopulation;
        long predCP = predatorPopulation;
        preyPopulation = Math.round(preyCP + preyGC*preyCP - preyDC*preyCP*predCP);
        predatorPopulation = Math.round(predCP - predatorDC*predCP + predatorGC*preyCP*predCP);
    }

    public long getPreyPopulation() {
        return preyPopulation;
    }

    public long getPredatorPopulation() {
        return predatorPopulation;
    }

    public void setPreyPopulation(long preyPopulation) {
        this.preyPopulation = preyPopulation;
    }

    public void setPredatorPopulation(long predatorPopulation) {
        this.predatorPopulation = predatorPopulation;
    }

    public long getNextPredatorUpdate(){
        long tempPrey = preyPopulation;
        long tempPredator = predatorPopulation;
        updatePopulationCount();
        long res = predatorPopulation;
        preyPopulation = tempPrey;
        predatorPopulation = tempPredator;
        return res;
    }

    public Tuple<Long, Long> getMaxPopulations(){
        long startPrey = preyPopulation;
        long startPred = predatorPopulation;
        long prey = 0;
        long pred = 0;
        for (int i = 0; i < 2000; i++){
            updatePopulationCount();
            prey = Math.max(prey, preyPopulation);
            pred = Math.max(pred, predatorPopulation);
        }
        preyPopulation = startPrey;
        predatorPopulation = startPred;
        return new Tuple<>(prey, pred);
    }

    public String toString(){
        return "Prey: "+ preyPopulation +"\n"+
                "Predator: "+ predatorPopulation +"\n";
    }
}
