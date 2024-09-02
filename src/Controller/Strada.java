package Controller;

import Model.MsqEvent;
import Model.MsqSum;
import Util.Distribution;

import java.util.ArrayList;
import java.util.List;

import static Model.Constants.RICARICA_SERVER;

/* Modeled as an Infinite Server */

public class Strada implements Center {
    private final EventListManager eventListManager;

    private final List<MsqSum> sumList = new ArrayList<>();
    private final Distribution distr;

    public Strada() {
        eventListManager = EventListManager.getInstance();
        distr = Distribution.getInstance();
    }

    @Override
    public void simpleSimulation() {
        List<MsqEvent> eventList = eventListManager.getServerStrada();
        MsqEvent intEvent = eventListManager.getIntEventStrada();

        
    }
}
