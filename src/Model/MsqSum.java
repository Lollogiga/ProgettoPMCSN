package Model;

public class MsqSum {
    private double service; /* Number served */
    private long served;    /* Number served */

    public MsqSum() {
        this.service = 0;
        this.served = 0;
    }

    public double getService() {
        return service;
    }

    public void setService(double service) {
        this.service = service;
    }

    public long getServed() {
        return served;
    }

    public void setServed(long served) {
        this.served = served;
    }

    public void incrementServed() {
        served++;
    }

    public void incrementService(double service) {
        this.service += service;
    }
}
