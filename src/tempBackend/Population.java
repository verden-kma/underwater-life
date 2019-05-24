package tempBackend;

public class Population {

    public static Tuple getMaxPopulation(double area, double productivityProducer, int waterPercentConsumer_1, int waterPercentConsumer_2, double avWeight_1, double avWeight_2){
        double dryProducerWeight = area*productivityProducer;
        double dryCons_1_Weight = dryProducerWeight/10;
        double dryCons_2_Weight = dryProducerWeight/100;
        double dryCoefficient_1 = (100 - waterPercentConsumer_1)/100.0;
        double dryCoefficient_2 = (100 - waterPercentConsumer_2)/100.0;
        double fullBodyWeight_1 = dryCons_1_Weight/dryCoefficient_1;
        double fullBodyWeight_2 = dryCons_2_Weight/dryCoefficient_2;
        long numCons_1 = Math.round(fullBodyWeight_1/avWeight_1);
        long numCons_2 = Math.round(fullBodyWeight_2/avWeight_2);
        return new Tuple(numCons_1, numCons_2);
    }
}
