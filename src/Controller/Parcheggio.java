package Controller;

import Model.MsqEvent;
import Model.MsqSum;
import Model.MsqT;
import Utils.Distribution;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static Model.Constants.*;

public class Parcheggio implements Center {
    private final EventListManager eventListManager;

    long number = 0;                     /* number of jobs in the node         */
    int e;                               /* next event index                   */
    int s;                               /* server index                       */
    long index = 0;                      /* used to count processed jobs       */
    double area = 0.0;           /* time integrated number in the node */
    double service;

    private final MsqT msqT = new MsqT();

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

        // First arrival event (car to park)
        double arrival = distr.getArrival(1);

        // Add this new event and setting time to arrival time
        serverList.set(0, new MsqEvent(arrival, 1));

        eventListManager.setServerParcheggio(serverList);
    }

    /* Finite horizon simulation */
    @Override
    public void simpleSimulation() {
        int e;
        MsqEvent event;
        List<MsqEvent> eventList = eventListManager.getServerParcheggio();
        List<MsqEvent> internalEventList = eventListManager.getIntQueueParcheggio();

        /* no external arrivals, no internal arrivals and no jobs in the server */
        if (eventList.getFirst().getX() == 0 && internalEventList.isEmpty() && this.number == 0) return;

        if (!internalEventList.isEmpty()) {
            eventList.getLast().setT(internalEventList.getFirst().getT());
            eventList.getLast().setX(1);
        }

        if ((e = MsqEvent.getNextEvent(eventList, PARCHEGGIO_SERVER + 1)) >= eventList.size()) return;
        msqT.setNext(eventList.get(e).getT());
        area += (msqT.getNext() - msqT.getCurrent()) * number;
        msqT.setCurrent(msqT.getNext());

        if (e == 0 || e == eventList.size() - 1) {   /* Check if event is an arrival */
            this.number++;

            if (e == 0) {   /* Check if event is an external arrival */
                eventList.getFirst().setT(msqT.getCurrent() + distr.getArrival(1)); /* Get new arrival from exogenous arrival */

                if (eventListManager.getCarsInParcheggio() + number > PARCHEGGIO_SERVER * 2) {
                    this.number--;

                    eventListManager.getSystemEventsList().get(2).setT(eventList.getFirst().getT());

                    return;
                }

                eventListManager.incrementCars();   /* New car in system */

                if (eventList.getFirst().getT() > STOP_FIN) {
                    /* Set event as "not active" cause simulation end */
                    eventList.getFirst().setX(0);
                    eventListManager.setServerParcheggio(eventList);
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

            // Update number of available cars in the center depending on where the car comes from
            if (eventListManager.incrementCarsInParcheggio() != 0) {
                this.number++;
                this.index--;

                List<MsqEvent> eventListNoleggio = eventListManager.getServerNoleggio();
                int nextEventNoleggio;

                if ((nextEventNoleggio = MsqEvent.getNextEvent(eventListNoleggio, NOLEGGIO_SERVER)) == -1) return;

                eventListManager.getSystemEventsList().get(2).setT(
                        eventListNoleggio.get(nextEventNoleggio).getT()
                );

                return; // Ho raggiunto il numero massimo di macchine nel parcheggio, devono restare in coda
                // TODO: gestire la penalità
            }

            /* Routing job to rental station */
            event = new MsqEvent(msqT.getCurrent(), 1, true);
            List<MsqEvent> intQueueNoleggio = eventListManager.getIntQueueNoleggio();
            intQueueNoleggio.add(event);
            eventListManager.setIntQueueNoleggio(intQueueNoleggio);

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
//        eventListManager.getSystemEventsList().get(2).setT(MsqEvent.getImminentEvent(eventList));

        int nextEvent = MsqEvent.getNextEvent(eventList, PARCHEGGIO_SERVER);
        if (nextEvent == -1) {
            eventListManager.getSystemEventsList().get(2).setX(0);
            return;
        }

        eventListManager.getSystemEventsList().get(2).setT(eventList.get(nextEvent).getT());
    }

    @Override
    public void infiniteSimulation() {

    }

    @Override
    public void printResult() {
        DecimalFormat f = new DecimalFormat("#0.00000000");

        System.out.println("Parcheggio\n\n");
        System.out.println("for " + index + " jobs the service node statistics are:\n\n");
        System.out.println("  avg interarrivals .. = " + eventListManager.getSystemEventsList().getFirst().getT() / index);
        System.out.println("  avg wait ........... = " + area / index);
        System.out.println("  avg # in node ...... = " + area / msqT.getCurrent());

        for(int i = 1; i <= PARCHEGGIO_SERVER; i++) {
            area -= sumList.get(i).getService();
        }
        System.out.println("  avg delay .......... = " + area / index);
        System.out.println("  avg # in queue ..... = " + area / msqT.getCurrent());
        System.out.println("\nthe server statistics are:\n\n");
        System.out.println("\tserver\tutilization\t avg service\t share\n");
        for(int i = 1; i <= PARCHEGGIO_SERVER; i++) {
            System.out.println("\t" + i + "\t\t" + f.format(sumList.get(i).getService() / msqT.getCurrent()) + "\t " + f.format(sumList.get(i).getService() / sumList.get(i).getServed()) + "\t " + f.format(((double)sumList.get(i).getServed() / index)));
        }
        System.out.println("\n");
    }
}


























