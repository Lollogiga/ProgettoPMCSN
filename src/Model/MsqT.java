package Model;

/* Clock di sistema (mantiene il tempo) */
public class MsqT {
    private double current; /* Current time */
    private double next;    /* Next (most imminent) event time  */
    private double batchTimer;   /* Batch time */

    public MsqT() {
        this.current = 0;
        this.next = 0;
    }

    public double getCurrent() {
        return current;
    }

    public double getNext() {
        return next;
    }

    public double getBatchTimer() {
        return batchTimer;
    }

    public void setCurrent(double current) {
        this.current = current;
    }

    public void setNext(double next) {
        this.next = next;
    }

    public void setBatchTimer(double batchTimer) {
        this.batchTimer = batchTimer;
    }
}
