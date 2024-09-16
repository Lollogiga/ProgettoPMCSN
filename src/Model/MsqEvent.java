package Model;

import java.util.List;

public class MsqEvent {
    private double t;               /*   next event time      */
    private int x;                  /*   event status, 0 or 1 */ /* Se evento: 0 = evento non attivo // 1 = evento attivo //// Se server: 0 = non occupato // 1 = occupato*/
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
    public static int getNextEvent(List<MsqEvent> event) {
        int e;
        int i = 0;

        while (i < event.size() && event.get(i).x != 1)       /* find the index of the first 'active' */
            i++;                                                /* element in the event list            */

        if (i >= event.size()) return -1;

        e = i;
        while (i < event.size() - 1) {         /* now, check the others to find which  */
            i++;                             /* event type is most imminent          */

            if ((event.get(i).x == 1) && (event.get(i).t < event.get(e).t))
                e = i;
        }
        return (e);
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

        if (i >= event.size()) return -1;

        e = i;
        while (i < servers) {         /* now, check the others to find which  */
            i++;                        /* event type is most imminent          */

            if ((event.get(i).x == 1) && (event.get(i).t < event.get(e).t))
                e = i;
        }
        return (e);
    }

    public static int findOne(List<MsqEvent> event) {
        int s;
        int i = 2;

        /* find the index of the first available */
        while (i < event.size() && event.get(i).x != 0)
            i++;                        /* (idle) server */

        if (i >= event.size()) return -1;

        s = i;
        while (i < event.size() - 1) {         /* now, check the others to find which   */
            i++;                        /* has been idle longest                 */

            if ((event.get(i).x == 0) && (event.get(i).t < event.get(s).t))
                s = i;
        }

        return (s);
    }

    /* Return the index of the available server idle longest */
    public static int findOne(List<MsqEvent> event, int servers) {
        int s;
        int i = 1;

        /* find the index of the first available */
        while (i < event.size() && event.get(i).x == 1)
            i++;                        /* (idle) server                         */

        if (i >= servers) return -1;

        s = i;
        while (i < servers) {         /* now, check the others to find which   */
            i++;                        /* has been idle longest                 */
            if ((event.get(i).x == 0) && (event.get(i).t < event.get(s).t))
                s = i;
        }

        return (s);
    }

    public static int findActiveServers(List<MsqEvent> event) {
        int count = 0;

        int s = 2;
        while (s < event.size()) {
            if (event.get(s).getX() == 1) count++;

            s++;
        }

        return count;
    }

    public static int findActiveServers(List<MsqEvent> event, int servers) {
        int count = 0;

        int s = 1;
        while (s < servers) {
            if (event.get(s).getX() == 1) count++;

            s++;
        }

        return count;
    }

    public static int findAvailableCar(List<MsqEvent> event) {
        int s;
        int i = 2;

        while (i < event.size() && event.get(i).x != 2)
            i++;

        if (i >= event.size()) return -1;

        s = i;
        while (i < event.size() - 1) {
            i++;

            if((event.get(i).x == 2) && (event.get(i).t < event.get(s).t))
                s = i;
        }

        return (s);
    }

    public static int findNextServerToComplete(List<MsqEvent> event) {
        int s;
        int i = 2;

        while (i < event.size() && event.get(i).x != 1)
            i++;

        if (i >= event.size()) return -1;

        s = i;
        while (i < event.size() - 1) {
            i++;

            if((event.get(i).getX() == 1) && (event.get(i).t < event.get(s).t))
                s = i;
        }

        return s;
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
