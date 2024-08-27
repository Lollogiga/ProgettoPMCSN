package Controller;

/* Generalizzazione per Antipasto1 e Antipasto2? */

import Model.EventListEntry;

import java.util.ArrayList;
import java.util.List;

public class Antipasto1Controller {
    private final EventListManager eventListManager;
    int e; // event index

    private double nodeArea;
    private double nodeQueue;
    private double nodeService;

    private List<EventListEntry> antipasto1List;

    private Antipasto1Controller(EventListManager eventListManager) {
        this.eventListManager = EventListManager.getInstance();

        // inizializzare lo stato del sistema
        // inizializzare il clock di sistema (t)

        this.antipasto1List = new ArrayList<>();
        /* inizializzare la lista degli eventi:
        * essendo coda M/M/1 è possibile inizializzarla come t + exponential(param)
        * trovare la funzione per la distribuzione esponenziale */
    }
}
