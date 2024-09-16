package Utils;

import Libs.Rngs;

import static Utils.Constants.*;

public class Distribution {
    private static Distribution instance = null;
    private double passengerArrival = 0.0;
    private double exogenous_park = 0.0;
    private double exogenous_charge = 0.0;

    private Rngs rngs;

    private Distribution(Rngs rngsToSet) {
        this.rngs = rngsToSet;
    }

    public static Distribution getInstance(Rngs toSet) {
        if (instance == null) {
            instance = new Distribution(toSet);
        }
        return instance;
    }

    public static Distribution getInstance() {
        return instance;
    }

    /* Generate an Exponential random variate, use m > 0.0 */
    public double exponential(double m) {
        return (-m * Math.log(1.0 - rngs.random()));
    }

    /** Generate the next arrival time
     * <ul>
     *  <li>param 0: user arrival</li>
     *  <li>param 1: exogenous arrival parking station</li>
     *  <li>param 2: exogenous arrival charge station</li>
     * */
    public double getArrival(int arrivalType) {
        rngs.selectStream(0);
        double paramPark = LAMBDA_EXOGENOUS * (1-P_RICARICA);
        double paramCharge = LAMBDA_EXOGENOUS * P_RICARICA;

        return switch (arrivalType) {
            case 0 -> /* Passenger arrival at rental station */
                    passengerArrival = exponential(1.0 / LAMBDA);
            case 1 -> /* Exogenous arrival at parking station */
                    exogenous_park = exponential(1.0 / paramPark);
            case 2 -> /* Exogenous arrival at charge station */
                    exogenous_charge = exponential(1.0 / paramCharge);
            default -> throw new IllegalArgumentException("Invalid arrival type");
        };
    }

    /** Generate the next service time
     * <ul>
     *  <li>param 0: Noleggio</li>
     *  <li>param 1: Parcheggio</li>
     *  <li>param 2: Ricarica</li>
     *  <li>param 3: Strada</li>
     */
    public double getService(int serviceType) {
        rngs.selectStream(1);

        return switch (serviceType) {
            case 0 -> /* Rental station */
                    exponential(RENTAL_SERVICE);
            case 1 -> /* Parking station */
                    exponential(PARKING_SERVICE);
            case 2 -> /* Charging station */
                    exponential(CHARGING_SERVICE);
            case 3 -> /* Route station */
                    exponential(ROUTE_SERVICE);
            default -> throw new IllegalArgumentException("Invalid service type");
        };
    }

    public Rngs getRngs() {
        return rngs;
    }
}
