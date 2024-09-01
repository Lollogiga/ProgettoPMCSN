package Util;

import Libs.Rngs;

import static Model.Constants.*;

public class Distribution {
    Rngs rngs = new Rngs();
    private static Distribution instance = null;
    private double passengerArrival = 0.0;

    public static Distribution getInstance() {
        if (instance == null) {
            instance = new Distribution();
        }
        return instance;
    }

    /* Generate an Exponential random variate, use m > 0.0 */
    public double exponential(double m) {
        return (-m * Math.log(1.0 - rngs.random()));
    }

    public static double cdfBernoulli(double p, long x) {
        return ((x == 0) ? 1.0 - p : 1.0);
    }

    /** Generate the next arrival time
     * <ul>
     *  <li>param 0: user arrival</li>
     *  <li>param 1: exogenous arrival parking station</li>
     *  <li>param 2: exogenous arrival charge station</li>
     * */
    public double getArrival(int arrivalType) {
        rngs.selectStream(0);

        // TODO: capire perché si dovrebbe incrementare il numero di arrivi

        return switch (arrivalType) {
            case 0 -> /* Passenger arrival at rental station */
                    passengerArrival = exponential(1.0 / LAMBDA);
            case 1 -> /* Exogenous arrival at parking station */
                    0; // TODO
            case 2 -> /* Exogenous arrival at charge station */
                    0; // TODO
            default -> throw new IllegalArgumentException("Invalid arrival type");
        };
    }

    /** Generate the next service time
     * <ul>
     *  <li>param 0: Rental station</li>
     *  <li>param 1: Parking station</li>
     *  <li>param 2: Charging station</li>
     *  <li>param 3: Route station</li>
     */
    public double getService(int serviceType) {
        rngs.selectStream(1);

        return switch (serviceType) {
            case 0 -> /* Rental station */
                    exponential(1.0 / RENTAL_SERVICE);
            case 1 -> /* Parking station */
                    exponential(1.0 / PARKING_SERVICE);
            case 2 -> /* Charging station */
                    exponential(1.0 / CHARGING_SERVICE);
//            case 3 -> /* Route station */
//                    exponential(1.0 / ROUTE_SERVICE);
            default -> throw new IllegalArgumentException("Invalid service type");
        };
    }
}
