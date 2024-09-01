package Model;

import java.util.List;

public class MsqEvent {
    private double t;   /*   next event time      */
    private int x;  /*   event status, 0 or 1 */

    public MsqEvent(double t, int x) {
        this.t = t;
        this.x = x;
    }

    /* Return the index of the next event type */
    public static int getNextEvent(List<MsqEvent> event, int servers) {
        int e;
        int i = 0;

        while (event.get(i).x == 0)       /* find the index of the first 'active' */
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

        while (event.get(i).x == 1)       /* find the index of the first available */
            i++;                        /* (idle) server                         */
        s = i;
        while (i < servers) {         /* now, check the others to find which   */
            i++;                        /* has been idle longest                 */
            if ((event.get(i).x == 0) && (event.get(i).t < event.get(s).t))
                s = i;
        }

        return (s);
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
