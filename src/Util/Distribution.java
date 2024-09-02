package Util;

import Libs.Rngs;

import static Model.Constants.*;

public class Distribution {
    Rngs rngs = new Rngs();
    private static Distribution instance = null;
    private double passengerArrival = 0.0;
    private double exogenous_park = 0.0;
    private double exogenous_charge = 0.0;

    long[] seed;                     /* current state of each stream   */
    int  stream        = 0;          /* stream index, 0 is the default */
    int STREAMS       = 256;        /* # of streams, DON'T CHANGE THIS VALUE    */
    long MODULUS      = 2147483647; /* DON'T CHANGE THIS VALUE                  */
    long MULTIPLIER   = 48271;      /* DON'T CHANGE THIS VALUE                  */
    long DEFAULT      = 123456789L; /* initial seed, use 0 < DEFAULT < MODULUS  */

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

        return switch (arrivalType) {
            case 0 -> /* Passenger arrival at rental station */
                    passengerArrival = exponential(1.0 / LAMBDA);
            case 1 -> /* Exogenous arrival at parking station */
                    exogenous_park = exponential(1.0 / LAMBDA); // TODO right
            case 2 -> /* Exogenous arrival at charge station */
                    exogenous_charge = exponential(1.0 / LAMBDA); // TODO right
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
            case 3 -> /* Route station */
                    exponential(1.0 / ROUTE_SERVICE);
            default -> throw new IllegalArgumentException("Invalid service type");
        };
    }

    /* ----------------------------------------------------------------
     * Random returns a pseudo-random real number uniformly distributed
     * between 0.0 and 1.0.
     * ----------------------------------------------------------------
     */
    public double random() {
        seed = new long[STREAMS];

        seed[0] = DEFAULT;

        long Q = MODULUS / MULTIPLIER;
        long R = MODULUS % MULTIPLIER;
        long t;

        t = MULTIPLIER * (seed[stream] % Q) - R * (seed[stream] / Q);
        if (t > 0)
            seed[stream] = t;
        else
            seed[stream] = t + MODULUS;
        return ((double) seed[stream] / MODULUS);
    }
}
