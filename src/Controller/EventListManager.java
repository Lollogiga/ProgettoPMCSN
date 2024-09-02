package Controller;

/* Event list manager realized as singleton */

import Model.MsqEvent;

import java.util.ArrayList;
import java.util.List;

import static Model.Constants.*;

public class EventListManager {
    private static EventListManager instance = null;

    /* Each list handle one server queue */
    private List<MsqEvent> serverStrada;
    private List<MsqEvent> serverNoleggio;
    private List<MsqEvent> serverRicarica;
    private List<MsqEvent> serverParcheggio;

    private List<MsqEvent> intQueueNoleggio;
    private List<MsqEvent> intQueueParcheggio;
    private List<MsqEvent> intQueueRicarica;

    private EventListManager() {
        this.serverStrada = new ArrayList<>();
        this.serverNoleggio = new ArrayList<>(NOLEGGIO_SERVER + 1);
        this.serverRicarica = new ArrayList<>(RICARICA_SERVER + 2);
        this.serverParcheggio = new ArrayList<>(PARCHEGGIO_SERVER + 2);

        this.intQueueParcheggio = new ArrayList<>();
        this.intQueueNoleggio = new ArrayList<>();
        this.intQueueRicarica = new ArrayList<>();
    }

    public static synchronized EventListManager getInstance() {
        /* If instance doesn't exist create a new one */
        if (instance == null) {
            instance = new EventListManager();
        }
        return instance;
    }

    public void setServerStrada(List<MsqEvent> serverStrada) {
        this.serverStrada = serverStrada;
    }

    public void setServerNoleggio(List<MsqEvent> noleggio) {
        this.serverNoleggio = noleggio;
    }

    public void setServerRicarica(List<MsqEvent> serverRicarica) {
        this.serverRicarica = serverRicarica;
    }

    public void setServerParcheggio(List<MsqEvent> parcheggio) {
        this.serverParcheggio = parcheggio;
    }

    public void setIntQueueParcheggio(List<MsqEvent> intQueueParcheggio) {
        this.intQueueParcheggio = intQueueParcheggio;
    }

    public void setIntQueueNoleggio(List<MsqEvent> intQueueNoleggio) {
        this.intQueueNoleggio = intQueueNoleggio;
    }

    public void setIntQueueRicarica(List<MsqEvent> intQueueRicarica) {
        this.intQueueRicarica = intQueueRicarica;
    }

    public List<MsqEvent> getServerStrada() {
        return serverStrada;
    }

    public List<MsqEvent> getServerNoleggio() {
        return serverNoleggio;
    }

    public List<MsqEvent> getServerRicarica() {
        return serverRicarica;
    }

    public List<MsqEvent> getServerParcheggio() {
        return serverParcheggio;
    }

    public List<MsqEvent> getIntQueueParcheggio() {
        return intQueueParcheggio;
    }

    public List<MsqEvent> getIntQueueNoleggio() {
        return intQueueNoleggio;
    }

    public List<MsqEvent> getIntQueueRicarica() {
        return intQueueRicarica;
    }
}
