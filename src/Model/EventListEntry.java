package Model;

import java.util.List;

public class EventListEntry {
    private double t;
    private int x;

    private EventListEntry (double t, int x) {
        this.t = t;
        this.x = x;
    }

    public static int getNextEvent(List<EventListEntry> event) {
        int e = -1; // initialization of "e"

        for (int i = 0; i < event.size(); i++) {
            if (event.get(i).x == 1) {
                e = i;
                break;
            }
        }

        if (e == -1) {
            throw new IllegalStateException("No active events found.");
        }

        return e;
    }

    public double getT() {
        return t;
    }

    public void setT(double t) {
        this.t = t;
    }

    public int getX() {return x;}

    public void setX(int x) {this.x = x;}
}
