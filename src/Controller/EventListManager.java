package Controller;

/* Event list manager realized as singleton */

import java.util.ArrayList;
import java.util.List;

public class EventListManager {
    private static EventListManager instance = null;

    /* Each list handle one server */
    private List<EventListManager> antipasto1;
    private List<EventListManager> antipasto2;
    private List<EventListManager> primo1;
    private List<EventListManager> primo2;
    private List<EventListManager> secondo1;
    private List<EventListManager> secondo2;
    private List<EventListManager> contorno1;
    private List<EventListManager> contorno2;
    private List<EventListManager> cassa1;
    private List<EventListManager> cassa2;

    public static synchronized EventListManager getInstance() {
        /* If instance doesn't exist create a new one */
        if (instance == null) {
            instance = new EventListManager();
        }

        return instance;
    }

    public EventListManager() {
        this.antipasto1 = new ArrayList<>();
        this.antipasto2 = new ArrayList<>();
        this.primo1 = new ArrayList<>();
        this.primo2 = new ArrayList<>();
        this.secondo1 = new ArrayList<>();
        this.secondo2 = new ArrayList<>();
        this.contorno1 = new ArrayList<>();
        this.contorno2 = new ArrayList<>();
        this.cassa1 = new ArrayList<>();
        this.cassa2 = new ArrayList<>();
    }

    public List<EventListManager> getAntipasto1() {
        return antipasto1;
    }

    public List<EventListManager> getAntipasto2() {
        return antipasto2;
    }

    public List<EventListManager> getPrimo1() {
        return primo1;
    }

    public List<EventListManager> getPrimo2() {
        return primo2;
    }

    public List<EventListManager> getSecondo1() {
        return secondo1;
    }

    public List<EventListManager> getSecondo2() {
        return secondo2;
    }

    public List<EventListManager> getContorno1() {
        return contorno1;
    }

    public List<EventListManager> getContorno2() {
        return contorno2;
    }

    public List<EventListManager> getCassa1() {
        return cassa1;
    }

    public List<EventListManager> getCassa2() {
        return cassa2;
    }
}
