package Controller;

import Model.MsqEvent;
import Model.MsqSum;
import Model.MsqT;
import Utils.Distribution;

import java.text.DecimalFormat;
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

    /* Finite horizon simulation */
    @Override
    public void simpleSimulation() {
        List<MsqEvent> eventList = eventListManager.getServerStrada();

        /* no external arrivals, no internal arrivals and no jobs in the server */
        if (eventList.getFirst().getX() == 0 && eventList.size() == 1) return;

        if ((e = MsqEvent.getNextEvent(eventList, eventList.size() - 1)) >= eventList.size()) return;
        msqT.setNext(eventList.get(e).getT());
        area += (msqT.getNext() - msqT.getCurrent()) * number;
        msqT.setCurrent(msqT.getNext());

        if (e == 0) {
            this.number++;

            eventList.get(e).setX(0);

            service = distr.getService(3);
            s = MsqEvent.findOne(eventList, eventList.size() - 1);

            if (s == -1 || s >= eventList.size()) {
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

            /* Update centralized event list */
            List<MsqEvent> systemList = eventListManager.getSystemEventsList();

            /* Routing */
            s = e;
            double pLoss = distr.random();
            if (pLoss < P_LOSS) {
                eventListManager.decrementCars();
                // Penalty cost:
                eventList.get(s).setX(0);
                eventListManager.getSystemEventsList().get(3).setT(MsqEvent.getImminentEvent(eventList));
            } else {
                /* Job stays in this system */
                double pRicarica = distr.random();
                if (pRicarica < P_RICARICA) {
                    // Event sent to Ricarica
                    List<MsqEvent> intQueueRicarica = eventListManager.getIntQueueRicarica();
                    intQueueRicarica.add(eventList.get(s));
                    eventListManager.setIntQueueRicarica(intQueueRicarica);

                    systemList.get(1).setX(1);
                    systemList.get(1).setT(eventList.get(s).getT());
                } else {
                    // Event sent to Parcheggio
                    List<MsqEvent> intQueueParcheggio = eventListManager.getIntQueueParcheggio();
                    intQueueParcheggio.add(eventList.get(s));
                    eventListManager.setIntQueueParcheggio(intQueueParcheggio);

                    systemList.get(2).setX(1);
                    systemList.get(2).setT(eventList.get(s).getT());
                }
                eventList.get(s).setX(0);   /* Set server as idle */
            }
        }

        eventListManager.setServerStrada(eventList);
        eventListManager.getSystemEventsList().get(3).setT(MsqEvent.getImminentEvent(eventList));
    }

    @Override
    public void infiniteSimulation() {

    }

    @Override
    public void printResult() {
        DecimalFormat f = new DecimalFormat("#0.00000000");

        System.out.println("Strada\n\n");
        System.out.println("for " + index + " jobs the service node statistics are:\n\n");
        System.out.println("  avg interarrivals .. = " + eventListManager.getSystemEventsList().getFirst().getT() / index);
        System.out.println("  avg wait ........... = " + area / index);
        System.out.println("  avg # in node ...... = " + area / msqT.getCurrent());

//        for(int i = 1; i < eventListManager.getServerStrada().size(); i++) {
//            area -= sumList.get(i).getService();
//        }
//        System.out.println("  avg delay .......... = " + area / index);
//        System.out.println("  avg # in queue ..... = " + area / msqT.getCurrent());
        System.out.println("\nthe server statistics are:\n\n");
        System.out.println("\tserver\tutilization\t avg service\t share\n");
        for(int i = 1; i < eventListManager.getServerStrada().size(); i++) {
            System.out.println("\t" + i + "\t\t" + f.format(sumList.get(i).getService() / msqT.getCurrent()) + "\t " + f.format(sumList.get(i).getService() / sumList.get(i).getServed()) + "\t " + f.format(((double)sumList.get(i).getServed() / index)));
        }
        System.out.println("\n");
    }
}
