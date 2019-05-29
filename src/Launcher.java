import tempBackend.Population;


public class Launcher {

   // private Population instance = Population.getInstance();

    public static void main(String[] args) {
        Population instance = Population.getInstance();
        System.out.println(instance);
        long c1 = 0;
        long c2 = 0;
        for (int i = 0; i < 5051; i++) {
            instance.updatePopulationCount();
            System.out.println("I: " + i);
            if (instance.getCurrentPreyPopulation() > c1) {
                c1 = instance.getCurrentPreyPopulation();
                System.out.println("Prey Rise: " + instance.getCurrentPreyPopulation());
            } else {
                c1 = instance.getCurrentPreyPopulation();
                System.out.println("Prey Fall: " + instance.getCurrentPreyPopulation());
            }
            if (instance.getCurrentPredatorPopulation() > c2) {
                c2 = instance.getCurrentPredatorPopulation();
                System.out.println("Predator Rise: " + instance.getCurrentPredatorPopulation());
            } else {
                c2 = instance.getCurrentPredatorPopulation();
                System.out.println("Predator Fall: " + instance.getCurrentPredatorPopulation());
            }
        }
    }
}
