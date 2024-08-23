package Controller;

/* Event list manager realized as singleton */

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

    private EventListManager() {

    }
}
