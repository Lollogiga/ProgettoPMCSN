package Controller;

import Model.MsqEvent;
import Model.MsqSum;
import Model.MsqT;
import Util.Distribution;

import java.util.ArrayList;
import java.util.List;

import static Model.Constants.*;

/* Modeled as an Infinite Server */
public class Strada implements Center {
    private final EventListManager eventListManager;

    long number = 0;                /* number in the node                 */
    int e;                          /* next event index                   */
    int s;                          /* server index                       */
    long index = 0;                 /* used to count processed jobs       */
    double service;
    double area = 0.0;              /* time integrated number in the node */

    private final MsqT msqT = new MsqT();

    private final List<MsqEvent> serverList = new ArrayList<>(1);
    private final List<MsqSum> sumList = new ArrayList<>(1);
    private final Distribution distr;

    public Strada() {
        eventListManager = EventListManager.getInstance();
        distr = Distribution.getInstance();

        /* Setup first server */
        serverList.addFirst(new MsqEvent(0, 0));
        sumList.addFirst(new MsqSum());

        eventListManager.setServerStrada(serverList);
    }

    @Override
    public void simpleSimulation() {
        List<MsqEvent> eventList = eventListManager.getServerStrada();

        /* no external arrivals, no internal arrivals and no jobs in the server */
        if (eventList.getFirst().getX() == 0 && eventList.size() == 1) return;

        e = MsqEvent.getNextEvent(eventList, eventList.size());
        msqT.setNext(eventList.get(e).getT());
        area += (msqT.getNext() - msqT.getCurrent()) * number;
        msqT.setCurrent(msqT.getNext());

        if (e == 0) {
            this.number++;

            eventList.get(e).setX(0);

            service = distr.getService(3);
            s = MsqEvent.findOne(eventList, eventList.size());

            if (s == -1) {
                /* Setup new server */
                eventList.add(new MsqEvent(msqT.getCurrent() +  service, 1));
                sumList.add(new MsqSum(service, 1));
            } else {
                /* Set existing server as active */
                eventList.get(s).setT(msqT.getCurrent() +  service);
                eventList.get(s).setX(1);

                sumList.get(s).incrementService(service);
                sumList.get(s).incrementServed();
            }
        } else {    /* Process a departure */
            this.index++;
            this.number--;

            /* Routing */
            double pLoss = distr.random();
            if (pLoss < P_LOSS)
                eventListManager.decrementCars();


            /* The job stays in this system */
            s = e;

            eventList.get(s).setX(1);
            double pRicarica = distr.random();
            if (pRicarica < P_RICARICA) {
                // Event sent to Ricarica
                List<MsqEvent> intQueueRicarica = eventListManager.getIntQueueRicarica();
                intQueueRicarica.add(eventList.get(s));
                eventListManager.setIntQueueRicarica(intQueueRicarica);
            } else {
                // Event sent to Parcheggio
                List<MsqEvent> intQueueParcheggio = eventListManager.getIntQueueParcheggio();
                intQueueParcheggio.add(eventList.get(s));
                eventListManager.setIntQueueParcheggio(intQueueParcheggio);
            }

            eventList.get(s).setX(0);   /* Set server as idle */

            eventListManager.setServerStrada(eventList);
        }
    }

    @Override
    public void printResult() {
        System.out.println("Strada\n\n");
        System.out.println("for " + index + " jobs the service node statistics are:\n\n");
        System.out.println("  avg interarrivals .. = " + eventListManager.getSystemEventsList().getFirst().getT() / index);
        System.out.println("  avg wait ........... = " + area / index);
        System.out.println("  avg # in node ...... = " + area / msqT.getCurrent());

        for(int i = 1; i < NODES; i++) {
            area -= sumList.get(i).getService();
        }
        System.out.println("  avg delay .......... = " + area / index);
        System.out.println("  avg # in queue ..... = " + area / msqT.getCurrent());
        System.out.println("\nthe server statistics are:\n\n");
        System.out.println("    server     utilization     avg service        share\n");
        for(int i = 1; i < NODES; i++) {
            System.out.println(i + "\t" + sumList.get(i).getService() / msqT.getCurrent() + "\t" + sumList.get(i).getService() / sumList.get(i).getServed() + "\t" + ((double)sumList.get(i).getServed() / index));
        }
        System.out.println("\n");
    }
}
