package Model;

import Util.Distribution;

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
    public static final double LAMBDA = 0.0067; /*  It is like 10 arrivals every 25 minutes  */

    /* Exogenous rate */
    public static final double LAMBDA_EXOGENOUS = 0.0067; // TODO

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

    public static final long SEED = 123456789L;   // TODO set right number
}
