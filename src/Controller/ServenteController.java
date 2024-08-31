package Controller;

import Model.EventListEntry;

import java.util.ArrayList;
import java.util.List;

public class ServenteController {
    private final EventListManager eventListManager;

    long   number = 0;             /* number in the node                 */
    int    e;                      /* next event index                   */
    int    s;                      /* server index                       */
    long   index  = 0;             /* used to count processed jobs       */
    double service;

    private double nodeArea;
    private double nodeQueue;
    private double nodeService;

    private List<EventListEntry> antipasto1List;

    private ServenteController(EventListManager eventListManager) {
        this.eventListManager = EventListManager.getInstance();

        // inizializzare lo stato del sistema
        // inizializzare il clock di sistema (t)

        this.antipasto1List = new ArrayList<>();
        /* inizializzare la lista degli eventi:
        * essendo coda M/M/1 è possibile inizializzarla come t + exponential(param)
        * trovare la funzione per la distribuzione esponenziale */
    }
}
