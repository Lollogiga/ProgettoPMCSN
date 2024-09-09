package Utils;

import static Utils.Constants.*;

public class RentalProfit {

    private static RentalProfit instance = null;
    private int penalty;
    private double rechargeCost;
    private double carsCost;
    private double parksCost;
    private double carsProfit;
    private int externalCars;

    private RentalProfit() {}

    public static synchronized RentalProfit getInstance() {
        if (instance == null) {
            instance = new RentalProfit();
        }
        return instance;
    }

    public void incrementPenalty() {
        this.penalty += LOSS_COST;
    }

    public void resetPenalty() {
        this.penalty = 0;
    }

    // The value is calculated in yen
    public double getCost(double time) {
        parksCost = (time / 3600.0) * (RICARICA_SERVER + PARCHEGGIO_SERVER) * PARKING_COST;
        carsCost = (time / 3600.0) * INIT_PARK_CARS * CAR_COST;

        return parksCost + carsCost + penalty + rechargeCost;
    }

    public void incrementExternalCars() {
        externalCars++;
    }

    public int getExternalCars() {
        return externalCars;
    }

    public void resetExternalCars() {
        externalCars = 0;
    }

    public void setProfit(double profit) {
        carsProfit = profit;
    }
    
    public void setRechargeCost(double cost){
        this.rechargeCost = cost;
    }

    public double getProfit() {
        return this.carsProfit;
    }
}
