package Utils;

import static Model.Constants.*;

public class RentalProfit {

    private static RentalProfit instance = null;
    private int penalty;
    private double carsCost;
    private double parksCost;
    private double carsProfit;


    private RentalProfit(){
    }

    public static synchronized RentalProfit getInstance() {
        if (instance == null) {
            instance = new RentalProfit();
        }
        return instance;
    }

    public void incrementPenalty() {
        this.penalty += LOSS_COST;
    }

    // The value is calculated in yen
    public double getCost() {
        parksCost = (STOP_FIN / 3600.0) * (RICARICA_SERVER + PARCHEGGIO_SERVER) * PARKING_COST;
        carsCost = (STOP_FIN / 3600.0) * INIT_PARK_CARS * CAR_COST;
        return parksCost + carsCost + penalty;
    }

    public void setProfit(double profit) {
        carsProfit = profit;
    }

    public double getProfit() {
        return this.carsProfit;
    }
}
