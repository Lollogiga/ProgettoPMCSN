package Model;

import java.util.List;

public class MsqEvent {
    private double t;               /*   next event time      */
    private int x;                  /*   event status, 0 or 1 */
    private boolean fromParking;    /* true = arrival from parking station, false = arrival from charging station */

    public MsqEvent(double t, int x) {
        this.t = t;
        this.x = x;
    }

    public MsqEvent(double t, int x, boolean fromParking) {
        this.t = t;
        this.x = x;
        this.fromParking = fromParking;
    }

    /* Return the index of the next event type */
    public static int getNextEvent(List<MsqEvent> event, int servers) {
        int e;
        int i = 0;

        if (event.size() == servers && event.size() == 1) {
            return 0;
        }

        while (i < event.size() && event.get(i).x == 0)       /* find the index of the first 'active' */
            i++;                        /* element in the event list            */

        e = i;
        while (i < servers) {         /* now, check the others to find which  */
            i++;                        /* event type is most imminent          */

            if ((event.get(i).x == 1) && (event.get(i).t < event.get(e).t))
                e = i;
        }
        return (e);
    }

    /* Return the index of the available server idle longest */
    public static int findOne(List<MsqEvent> event, int servers) {
        int s;
        int i = 1;

        /* find the index of the first available */
        while (i < event.size() && event.get(i).x == 1)
            i++;                        /* (idle) server                         */

        if (i >= event.size()) return -1;

        s = i;
        while (i < servers) {         /* now, check the others to find which   */
            i++;                        /* has been idle longest                 */
            if ((event.get(i).x == 0) && (event.get(i).t < event.get(s).t))
                s = i;
        }

        return (s);
    }

    public static double getImminentEvent(List<MsqEvent> eventList) {
        double threshold = Double.MAX_VALUE;
        for (MsqEvent e : eventList) {
            if (e.x == 1 && e.t < threshold)
                threshold = e.t;
        }
        return threshold;
    }

    public double getT() {
        return t;
    }

    public void setT(double t) {
        this.t = t;
    }

    public int getX() {return x;}

    public void setX(int x) {this.x = x;}

    public boolean isFromParking() {return fromParking;}

    public void setFromParking(boolean fromParking) {this.fromParking = fromParking;}

    @Override
    public String toString() {
        return "{t=" + t + ", x=" + x + '}';
    }
}
