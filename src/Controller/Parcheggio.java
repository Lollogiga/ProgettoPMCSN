package Controller;

import Libs.Rngs;
import Model.MsqEvent;
import Model.MsqSum;
import Model.MsqT;
import Util.Distribution;

import java.util.ArrayList;
import java.util.List;

import static Model.Constants.*;

public class Parcheggio implements Center {
    private final EventListManager eventListManager;

    long number = 0;                     /* number of jobs in the node         */
    int e;                               /* next event index                   */
    int s;                               /* server index                       */
    long index = 0;                      /* used to count processed jobs       */
    private double area = 0.0;           /* time integrated number in the node */
    double service;

    private final MsqT msqT = new MsqT();

    private final Rngs r = new Rngs();

    private final List<MsqEvent> serverList = new ArrayList<>(PARCHEGGIO_SERVER + 2);
    private final List<MsqSum> sumList = new ArrayList<>(PARCHEGGIO_SERVER + 2);
    private final Distribution distr;

    public Parcheggio() {
        eventListManager = EventListManager.getInstance();
        distr = Distribution.getInstance();

        /* Initial servers setup */
        for (s = 0; s < PARCHEGGIO_SERVER + 2; s++) {
            serverList.add(s, new MsqEvent(0, 0));
            sumList.add(s, new MsqSum());
        }

        eventListManager.setServerParcheggio(serverList);
    }

    public void simpleSimulation() {
        List<MsqEvent> eventList = eventListManager.getServerParcheggio();
        List<MsqEvent> internalEventList = eventListManager.getIntQueueParcheggio();

        /* no external arrivals, no internal arrivals and no jobs in the server */
        if (eventList.getFirst().getX() == 0 && internalEventList.isEmpty() && this.number == 0) return;

        if (!internalEventList.isEmpty()) eventList.getLast().setT(internalEventList.getFirst().getT());

        int e = MsqEvent.getNextEvent(eventList, PARCHEGGIO_SERVER + 1);
        msqT.setNext(eventList.get(e).getT());
        area += (msqT.getNext() - msqT.getCurrent()) * number;
        msqT.setCurrent(msqT.getNext());

        if (e == 0 || e == eventList.size() - 1) {   /* Check if event is an arrival */
            this.number++;
            MsqEvent event;

            if (e == 0) {   /* Check if event is an external arrival */
                event = eventList.getFirst();

                eventList.getFirst().setT(distr.getArrival(1)); /* Get new arrival from exogenous arrival */

                if (eventList.getFirst().getT() > STOP) {
                    eventList.getFirst().setX(0);
                    eventListManager.setServerParcheggio(eventList); // TODO superfluo? Fatto alla fine
                }
            } else {    /* Event is an internal arrival */
                event = internalEventList.getFirst();
                internalEventList.removeFirst();

                if (internalEventList.isEmpty()) eventList.getLast().setX(0);
            }

            if (number <= PARCHEGGIO_SERVER) {
                service = distr.getService(1);
                s = MsqEvent.findOne(eventList, PARCHEGGIO_SERVER);

                /* Set server as active */
                eventList.get(s).setT(msqT.getCurrent() +  service);
                eventList.get(s).setX(1);

                sumList.get(s).incrementService(service);
                sumList.get(s).incrementServed();
            }
        } else {        /* Process a departure */
            this.index++;
            this.number--;

            // TODO move job to Noleggio queue

            s = e;
            if (number >= PARCHEGGIO_SERVER) {        /* there is some jobs in queue, place another job in this server */
                service = distr.getService(1);
                eventList.get(s).setT(msqT.getCurrent() + service);

                sumList.get(s).incrementService(service);
                sumList.get(s).incrementServed();
            } else                                    /* no job in queue, simply remove it from server */
                eventList.get(s).setX(0);
        }

        eventListManager.setServerParcheggio(eventList);
        eventListManager.setIntQueueParcheggio(internalEventList);
    }
}


























