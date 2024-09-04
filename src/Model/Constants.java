package Model;

public class Constants {
    /*  Number of servers in each center  */
    public static final int NOLEGGIO_SERVER = 1;
    public static final int STRADA_SERVER = Integer.MAX_VALUE; // Infinite server
    public static final int PARCHEGGIO_SERVER = 200;

     public static final int RICARICA_SERVER = 60;

    /*  Value of Start and Stop time */
    public static final double START = 0.0;
    public static final double STOP_INF = Double.MAX_VALUE; /* Infinite simulation */
//    public static final double STOP_FIN = 86400; /* Finite simulation -> check every 1 day */
    public static final double STOP_FIN = 86400 * 365; /* Finite simulation -> check every 1 day */
//    public static final double STOP_FIN = Double.MAX_VALUE; /* Finite simulation -> check every 1 day */

    /* Probabilities */
    public static final double P_RICARICA = 0.1;
    public static final double P_LOSS = 0.2;

    /* Arrival rate in rental station (users/sec) */
    public static final double LAMBDA = 10 / 60.0 / 60.0;

    /* Service rate in rental station (jobs/sec) */
    public static final double MU_RENTAL = LAMBDA;

    /* Service rate (jobs/sec), 37,5 parked cars in one hour */
    public static final double MU_PARKING = 37.5 / 60.0 / 60.0;
//    public static final double MU_PARKING = 1 / 60.0 / 60.0;

    /* Charging rate (jobs/sec), one battery is fully charged in 45 minutes */
    public static final double MU_CHARGING = 1.33 / 60.0 / 60.0;
//    public static final double MU_CHARGING = 4 / 60.0 / 60.0;

    /* Service rate (job/sec), is considered to rent car and drive it for 30 min */
    public static final double MU_STRADA = 2 / 60.0 / 60.0;
//    public static final double MU_STRADA = 8 / 60.0 / 60.0;

    /* Exogenous rate */
    public static final double LAMBDA_EXOGENOUS = 12 / 60.0 / 60.0;

    /* rental station (time to process renting service) */
    public static final double RENTAL_SERVICE = 1.0 / MU_RENTAL;

    /* parking station (time to park a car) */
    public static final double PARKING_SERVICE = 1.0 / MU_PARKING;   // mu = 0.625

    /* charging station (time to recharge car in charging station) */
    public static final double CHARGING_SERVICE = 1.0 / MU_CHARGING; // Charging time is 45 minutes on average.

    /* Service rate of route */
    public static final double ROUTE_SERVICE = 1.0 / MU_STRADA;    // Rental time is 30 min

    /* Nodes in system */
    public static final int NODES = 4;

    /* Seed to use for the simulation */
    //public static final long SEED = 123456789L;   // TODO set right number
    public static final long SEED = 234567890L;

    public static final int INIT_PARK_CARS = 100; //Todo set right number (Should be 200, CHECK CARS AND PARCHEGGIO_SERVER)

    /* Cost constants */
    public static final int CAR_COST = 5; /* Yen/hour for each car */
    public static final int PARKING_COST = 8; /* Yen/hour for each parking */
    public static final int LOSS_COST = 300; /* Yen for each loss */

    /* Profit constants */ /* Todo Variable or constant profit */
    public static final int RENTAL_TIME_PROFIT = 118; /* (100 + 0.3 * 60) Yen/hour when a car is on the road */
    public static final double RENTAL_KM_PROFIT = 0.99; /* Yen/km when a car is on the road */
    public static final int RENTAL_PROFIT = 80; /* Yen/hour */

    /* Batch simulation */
    public static final int K = 64;
    //public static final int B = ;
}
