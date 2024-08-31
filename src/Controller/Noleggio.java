package Controller;

import Model.MsqEvent;
import Model.MsqSum;
import Model.MsqT;
import Utils.Rngs;

import java.util.ArrayList;
import java.util.List;

import static Model.Constants.*;

public class Noleggio {
    private final EventListManager eventListManager;

    long number = 0;             /* number in the node                 */
    int e;                      /* next event index                   */
    int s;                      /* server index                       */
    long index  = 0;             /* used to count processed jobs       */
    private double area   = 0.0;           /* time integrated number in the node */
    double service;

    private Rngs r = new Rngs();
    private List<MsqEvent> eventList = new ArrayList<>(NOLEGGIO_SERVER + 1);
    private List<MsqSum> sumList = new ArrayList<>(NOLEGGIO_SERVER + 1);
    private MsqT msqT = new MsqT();

    public Noleggio() {
        this.eventListManager = EventListManager.getInstance();

        for (s=0; s<NOLEGGIO_SERVER+1; s++) {
            this.eventList.add(s, new MsqEvent(0, 0));
            this.sumList.add(s, new MsqSum());
        }
        // inizializzare lo stato del sistema
        // inizializzare il clock di sistema (t)

        this.eventList.set(0, )
    }
}
