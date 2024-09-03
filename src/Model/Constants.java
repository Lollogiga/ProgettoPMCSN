package Model;

import Utils.Distribution;

public class Constants {
    /*  Number of servers in each center  */
    public static final int NOLEGGIO_SERVER = 1;
    public static final int STRADA_SERVER = Integer.MAX_VALUE; // Infinite server
    public static final int PARCHEGGIO_SERVER = 200;
    public static final int RICARICA_SERVER = 60;

    /*  Value of Start and Stop time */
    public static final double START = 0.0;
    public static final double STOP_INF = Double.MAX_VALUE; /* Infinite simulation */
    public static final double STOP_FIN = 86400; /* Finite simulation -> check every 1 day */

    /* Probabilities */
    public static final double P_RICARICA = Distribution.cdfBernoulli(0.9, 0);  // dove x = 0 (probabilità che la carica sia sotto al 10%) e p = 0.9 probabilità che una macchina abbia carica sopra al 10%
    public static final double P_LOSS = 0.2;

    /* Arrival rate (passenger/sec) */
    public static final double LAMBDA = 10;

    /* Exogenous rate */
    public static final double LAMBDA_EXOGENOUS = 10; // TODO set right number

    /* Service rate of rental station (vehicle/sec) */
    public static final double RENTAL_SERVICE = LAMBDA;

    /* Service rate of parking station */
    public static final double PARKING_SERVICE = 0.625;

    /* Service rate of charging station */
    public static final double CHARGING_SERVICE = 3240;

    /* Service rate of route */
    public static final double ROUTE_SERVICE = 1;// TODO set right number

    /* Nodes in system */
    public static final int NODES = 4;

    /* Seed to use for the simulation */
    public static final long SEED = 123456789L;   // TODO set right number

    public static final int CARS = 200; //Todo set right number (Should be 200, CHECK CARS AND PARCHEGGIO_SERVER)

    public static final int INIT_SYS_CARS = 100;

    /* Cost constants */
    public static final int CAR_COST = 5; /* Yen/hour for each car */
    public static final int PARKING_COST = 8; /* Yen/hour for each parking */
    public static final int LOSS_COST = 300; /* Yen for each loss */

    /* Profit constants */ /*Todo Variable or constant profit */
    public static final int RENTAL_TIME_PROFIT = 118; /* (100 + 0.3*60) Yen/hour when a car is on the road */
    public static final double RENTAL_KM_PROFIT = 0.99; /* Yen/km when a car is on the road */
    public static final int RENTAL_PROFIT = 80; /* Yen/hour */
}
