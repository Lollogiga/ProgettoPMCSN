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

    /* Generate the next arrival time */
    public double getArrival() {
        rngs.selectStream(0);

        // TODO: capire perché si dovrebbe incrementare il numero di arrivi

        passengerArrival = exponential(1.0 / LAMBDA);
        return passengerArrival;
    }

    /* Generate the next service time */
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
