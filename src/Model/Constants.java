package Model;

public class Constants {
    /*  Number of servers in each center  */
    public static final int NOLEGGIO_SERVER = 1;
    public static final int STRADA_SERVER = ;
    public static final int PARCHEGGIO_SERVER = ;
    public static final int RICARICA_SERVER = ;

    /*  Value of Start and Stop time */
    public static final double START = 0.0;
    public static final double STOP = Double.MAX_VALUE; /* Infinite simulation */

    /* Probabilities */
    public static final double P_RICARICA = ;
    public static final double P_LOSS = 0.2;

    /* Arrival rate (passenger/sec) */
    public static final double LAMBDA = 0.0067; /*  It is like 10 arrivals every 25 minutes  */

    /* Service rate of rental station (vehicle/sec) */
    public static final double RENTAL_SERVICE = LAMBDA;

    /* Service rate of parking station */
    public static final double PARKING_SERVICE = 0.625;

    /* Service rate of charging station */
    public static final double CHARGING_SERVICE = 3240;

    /* Service rate of route */
    public static final double ROUTE_SERVICE = ;
}
