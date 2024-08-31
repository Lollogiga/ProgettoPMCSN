package Controller;

/* Event list manager realized as singleton */

import Model.MsqEvent;

import java.util.ArrayList;
import java.util.List;

public class EventListManager {
    private static EventListManager instance = null;

    /* Each list handle one server */
    private List<MsqEvent> strada;
    private List<MsqEvent> noleggio;
    private List<MsqEvent> ricarica;
    private List<MsqEvent> parcheggio;

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

    public void setNoleggio(List<MsqEvent> noleggio) {
        this.noleggio = noleggio;
    }

    public void setRicarica(List<MsqEvent> ricarica) {
        this.ricarica = ricarica;
    }

    public void setParcheggio(List<MsqEvent> parcheggio) {
        this.parcheggio = parcheggio;
    }

    public void setStrada(List<MsqEvent> strada) {
        this.strada = strada;
    }
}
