import tempBackend.Population;


public class Launcher {

   // private Population instance = Population.getInstance();

    public static void main(String[] args) {
        Population instance = Population.getInstance();
        System.out.println(instance);
        long c1 = 0;
        long c2 = 0;
        for (int i = 0; i <2001; i++) {
            instance.updatePopulationCount();
            c1 = Math.max(c1, instance.getPreyPopulation());
            c2 = Math.max(c2, instance.getPredatorPopulation());
           /* System.out.println("I: " + i);
            if (instance.getPreyPopulation() > c1) {
                c1 = instance.getPreyPopulation();
                System.out.println("Prey Rise: " + instance.getPreyPopulation());
            } else {
                c1 = instance.getPreyPopulation();
                System.out.println("Prey Fall: " + instance.getPreyPopulation());
            }
            if (instance.getPredatorPopulation() > c2) {
                c2 = instance.getPredatorPopulation();
                System.out.println("Predator Rise: " + instance.getPredatorPopulation());
            } else {
                c2 = instance.getPredatorPopulation();
                System.out.println("Predator Fall: " + instance.getPredatorPopulation());
            }*/
        }
        System.out.println("PreyMax: "+c1 + "\nPredMax: " + c2);
    }
}
