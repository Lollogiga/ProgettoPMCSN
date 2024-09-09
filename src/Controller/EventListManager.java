package Controller;

/* Event list manager realized as singleton */

import Model.MsqEvent;

import java.util.ArrayList;
import java.util.List;

import static Utils.Constants.*;

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

    private List<MsqEvent> systemEventsList;

    private int carsInParcheggio;
    private int carsInRicarica;
    private int cars;

    private EventListManager() {
        this.serverStrada = new ArrayList<>(1);
        this.serverNoleggio = new ArrayList<>(NOLEGGIO_SERVER + 1);
        this.serverRicarica = new ArrayList<>(RICARICA_SERVER + 2);
        this.serverParcheggio = new ArrayList<>(PARCHEGGIO_SERVER + 2);

        this.intQueueParcheggio = new ArrayList<>();
        this.intQueueNoleggio = new ArrayList<>();
        this.intQueueRicarica = new ArrayList<>();

        this.systemEventsList = new ArrayList<>(NODES);

        this.carsInParcheggio = INIT_PARK_CARS;
        this.carsInRicarica = 0;
        this.cars = INIT_PARK_CARS; // Cars in our station
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

    public void setCarsInParcheggio(int carsInParcheggio) {
        this.carsInParcheggio = carsInParcheggio;
    }

    public void setCarsInRicarica(int carsInRicarica) {
        this.carsInRicarica = carsInRicarica;
    }

    public void setSystemEventsList(List<MsqEvent> systemEventsList) {
        this.systemEventsList = systemEventsList;
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

    public List<MsqEvent> getSystemEventsList() {
        return systemEventsList;
    }

    public int getCarsInParcheggio() {
        return carsInParcheggio;
    }

    public int getCarsInRicarica() {
        return carsInRicarica;
    }

    public int incrementCarsInParcheggio() {
        if (this.carsInParcheggio == PARCHEGGIO_SERVER) return 1;

        this.carsInParcheggio++;

        return 0;
    }

    public int incrementCarsInRicarica() {
        if (this.carsInRicarica == RICARICA_SERVER) return 1;

        this.carsInRicarica++;

        return 0;
    }

    public void incrementCars() {
        this.cars++;
    }

    public void decrementCars() {
        if (this.cars != 0)
            this.cars--;
    }

    public int reduceCarsInParcheggio() {
        if (this.carsInParcheggio == 0) return 1;

        this.carsInParcheggio--;

        return 0;
    }

    public int reduceCarsInRicarica() {
        if (this.carsInRicarica == 0) return 1;

        this.carsInRicarica--;

        return 0;
    }

    public void resetState() {
        this.serverStrada.clear();
        this.serverNoleggio.clear();
        this.serverRicarica.clear();
        this.serverParcheggio.clear();

        this.intQueueParcheggio.clear();
        this.intQueueNoleggio.clear();
        this.intQueueRicarica.clear();

        this.systemEventsList.clear();

        this.carsInParcheggio = INIT_PARK_CARS;
        this.carsInRicarica = 0;
        this.cars = INIT_PARK_CARS;
    }
}
