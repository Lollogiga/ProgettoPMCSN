package Controller;

/* Event list manager realized as singleton */

import java.util.ArrayList;
import java.util.List;

public class EventListManager {
    private static EventListManager instance = null;

    /* Each list handle one server */
    private List<EventListManager> strada;
    private List<EventListManager> noleggio;
    private List<EventListManager> ricarica;
    private List<EventListManager> parcheggio;

    public static synchronized EventListManager getInstance() {
        /* If instance doesn't exist create a new one */
        if (instance == null) {
            instance = new EventListManager();
        }

        return instance;
    }

    public EventListManager() {
        this.strada = new ArrayList<>();
        this.noleggio = new ArrayList<>();
        this.ricarica = new ArrayList<>();
        this.parcheggio = new ArrayList<>();
    }
}
