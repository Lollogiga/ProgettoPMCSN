package Model;

public class MsqSum {
    private double service; /* Time spent in service */
    private long served;    /* Number served */

    public MsqSum() {
        this.service = 0;
        this.served = 0;
    }

    public MsqSum(double service, long served) {
        this.service = service;
        this.served = served;
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
