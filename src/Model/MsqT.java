package Model;

public class MsqT {
    private double current; /* Current time */
    private double next;    /* Next (most imminent) event time  */

    public double getCurrent() {
        return current;
    }

    public void setCurrent(double current) {
        this.current = current;
    }

    public double getNext() {
        return next;
    }

    public void setNext(double next) {
        this.next = next;
    }
}
