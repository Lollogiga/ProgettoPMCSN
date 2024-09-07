package Controller;

import Model.MsqEvent;
import Model.MsqSum;
import Model.MsqT;
import Utils.BatchMeans;
import Utils.Distribution;
import Utils.RentalProfit;
import Utils.SimulationResults;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static Model.Constants.*;

public class Parcheggio implements Center {
    private final EventListManager eventListManager;
    private final RentalProfit rentalProfit;

    long nJobInQueue = 0;                     /* number of jobs in the node         */
    int e;                               /* next event index                   */
    int s;                               /* server index                       */
    long index = 0;                      /* used to count processed jobs       */
    double area = 0.0;           /* time integrated number in the node */
    double service;

    int nBatch = 0;
    int jobInBatch = 0;
    double batchDuration = 0L;

    private final MsqT msqT = new MsqT();

    private final List<MsqEvent> serverList = new ArrayList<>(PARCHEGGIO_SERVER + 2);
    private final List<MsqSum> sumList = new ArrayList<>(PARCHEGGIO_SERVER + 2);

    private final SimulationResults batchParcheggio = new SimulationResults();
    private final Distribution distr;

    public Parcheggio() {
        eventListManager = EventListManager.getInstance();
        rentalProfit = RentalProfit.getInstance();

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
    public void simpleSimulation() throws Exception {
        int e;
        MsqEvent event;

        List<MsqEvent> eventList = eventListManager.getServerParcheggio();
        List<MsqEvent> internalEventList = eventListManager.getIntQueueParcheggio();

        if (eventList.getFirst().getX() == 0 && internalEventList.isEmpty() && this.nJobInQueue == 0)        /* no external arrivals, no internal arrivals and no jobs in the server */
            return;

        if (!internalEventList.isEmpty()) {     /* Set last eventList item to first available car in the queue */
            eventList.getLast().setT(internalEventList.getFirst().getT());
            eventList.getLast().setX(1);
        }

        boolean isQueueEv = false;

        if ((e = MsqEvent.getNextEvent(eventList, PARCHEGGIO_SERVER + 1)) >= eventList.size())      /* No event to process found */
            return;

        if (eventList.get(e).getT() > eventListManager.getSystemEventsList().get(2).getT()) {       /* check if nextEvent time is bigger than system time -> a server was freed */
            msqT.setNext(eventListManager.getSystemEventsList().get(2).getT());

            e = 0;

            isQueueEv = true;
        } else {
            msqT.setNext(eventList.get(e).getT());
        }

        area += (msqT.getNext() - msqT.getCurrent()) * nJobInQueue;
        msqT.setCurrent(msqT.getNext());

        if (e == 0 || e == eventList.size() - 1) {      /* Check if event is an arrival */
            if (!isQueueEv)
                this.nJobInQueue++;

            // Controllo se NON ho parcheggi disponibili:
            if (eventListManager.getCarsInParcheggio() + MsqEvent.findActiveServers(eventList, PARCHEGGIO_SERVER) + this.nJobInQueue > PARCHEGGIO_SERVER + PARCHEGGIO_MAX_QUEUE) {       /*  */
                // Controllo se ho spazio per mettere l'evento in coda, se no lo rimuovo dalla coda
                if (this.nJobInQueue > PARCHEGGIO_MAX_QUEUE) {      /* Non ho spazio per inserire il job in coda -> aggiorno il tempo e lo rimuovo*/
                    this.nJobInQueue--;        /* Rimuovo dalla coda dei job pendenti il job in eccesso */

                    // TODO controllare con lollogiga
                    rentalProfit.incrementPenalty();    /* Count penalty for lost job */
                }

                if ((e = MsqEvent.getNextEvent(eventList, PARCHEGGIO_SERVER + 1)) >= eventList.size())      /* Non ho eventi futuri da processare -> disattivo il nodo */
                    eventListManager.getSystemEventsList().get(2).setX(0);

                eventListManager.getSystemEventsList().get(2).setT(eventList.get(e).getT()); /* Imposto il tempo in system pari al prossimo evento più imminente */

                return;
            }

            if (e == 0) {   /* Check if event is an external arrival */
                if (!isQueueEv)
                    eventList.getFirst().setT(msqT.getCurrent() + distr.getArrival(1));

                eventListManager.incrementCars();   /* New car in system */

                if (eventList.getFirst().getT() > STOP_FIN)      /* Stop condition */
                    eventList.getFirst().setX(0);       /* Set event as "not active" cause simulation end */
            } else {        /* Event is an internal arrival */
                event = internalEventList.getFirst();

                internalEventList.removeFirst();        /* Processing the first internal arrival, remove it from internalEventList to not be picked again */
            }

            // Devo ora insirire l'evento all'interno di un server
            if (eventListManager.getCarsInParcheggio() + MsqEvent.findActiveServers(eventList, PARCHEGGIO_SERVER) + 1 <= PARCHEGGIO_SERVER) {      /* Check if there is a free server to use */
                service = distr.getService(1);
                s = MsqEvent.findOne(eventList, PARCHEGGIO_SERVER);

                /* Set server as active */
                eventList.get(s).setT(msqT.getCurrent() + service);
                eventList.get(s).setX(1);

                sumList.get(s).incrementService(service);
                sumList.get(s).incrementServed();
            }

            if (e == eventList.size() - 1 && internalEventList.isEmpty()) eventList.getLast().setX(0);
        } else {
            this.index++;

            if (this.nJobInQueue > 0)
                this.nJobInQueue--;

            // Update number of available cars in the center depending on where the car comes from
            if (eventListManager.incrementCarsInParcheggio() != 0)
                throw new Exception("IncrementCarsInParcheggio error. this.nJob: " + this.nJobInQueue + ", eventList.size(): " + eventList.size());

            /* Routing job to station Noleggio */
            List<MsqEvent> intQueueNoleggio = eventListManager.getIntQueueNoleggio();
            intQueueNoleggio.add(new MsqEvent(msqT.getCurrent(), 1, true));
            eventListManager.setIntQueueNoleggio(intQueueNoleggio);
            eventListManager.getSystemEventsList().getFirst().setT(msqT.getCurrent());

            if (nJobInQueue > 0 && eventListManager.getCarsInParcheggio() + MsqEvent.findActiveServers(eventList, PARCHEGGIO_SERVER) < PARCHEGGIO_SERVER) {       /* Check if there is a pending job and if there is a free server */
                service = distr.getService(1);
                s = e;      /* s is set to the index of the newly freed server */

                eventList.get(s).setT(msqT.getCurrent() + service);
                eventList.get(s).setX(1);

                sumList.get(s).incrementService(service);
                sumList.get(s).incrementServed();
            } else {
                s = e;
                eventList.get(s).setX(0);
            }
        }

        eventListManager.setServerParcheggio(eventList);
        eventListManager.setIntQueueParcheggio(internalEventList);

        int nextEvent = MsqEvent.getNextEvent(eventList, PARCHEGGIO_SERVER);
        if (nextEvent == -1) {
            eventListManager.getSystemEventsList().get(2).setX(0);
            return;
        }

//        if (this.nJobInQueue != 0 && eventListManager.getCarsInParcheggio() + MsqEvent.findActiveServers(eventList, PARCHEGGIO_SERVER) + 1 <= PARCHEGGIO_SERVER)
//            eventListManager.getSystemEventsList().get(2).setT(msqT.getCurrent());
//        else
            eventListManager.getSystemEventsList().get(2).setT(eventList.get(nextEvent).getT());
    }

    /* Infinite horizon simulation */
    @Override
    public void infiniteSimulation() {
        int e;
        MsqEvent event;
        List<MsqEvent> eventList = eventListManager.getServerParcheggio();
        List<MsqEvent> internalEventList = eventListManager.getIntQueueParcheggio();

        if (eventList.getFirst().getX() == 0 && internalEventList.isEmpty() && this.nJobInQueue == 0)
            return; /* no external arrivals, no internal arrivals and no jobs in the server */

        if (!internalEventList.isEmpty()) {
            eventList.getLast().setT(internalEventList.getFirst().getT());
            eventList.getLast().setX(1);
        }

        double msqCurr = msqT.getCurrent();

        if ((e = MsqEvent.getNextEvent(eventList, PARCHEGGIO_SERVER + 1)) >= eventList.size()) return;
        msqT.setNext(eventList.get(e).getT());
        area += (msqT.getNext() - msqT.getCurrent()) * nJobInQueue;
        msqT.setCurrent(msqT.getNext());

        if (e == 0 || e == eventList.size() - 1) {   /* Check if event is an arrival */
            this.nJobInQueue++;

            BatchMeans.incrementJobInBatch();
            jobInBatch++;

            if (e == 0) {       /* Check if event is an external arrival */
                eventList.getFirst().setT(msqT.getCurrent() + distr.getArrival(1));     /* Get new arrival from exogenous arrival */

                if (eventListManager.getCarsInParcheggio() + this.nJobInQueue > PARCHEGGIO_SERVER + PARCHEGGIO_MAX_QUEUE) {      /* New arrival but Parcheggio's queue is full */
                    this.nJobInQueue--;      /* Loss event */
                    eventListManager.decrementCars();
                    eventListManager.getSystemEventsList().get(2).setT(eventList.getFirst().getT());

                    return;
                }

                eventListManager.incrementCars();   /* New car in system */

                if (eventList.getFirst().getT() > STOP_INF) {
                    /* Set event as "not active" cause simulation end */
                    eventList.getFirst().setX(0);
                    eventListManager.setServerParcheggio(eventList);
                }
            } else {    /* Event is an internal arrival */
                event = internalEventList.getFirst();
                internalEventList.removeFirst();
            }

            if (eventListManager.getCarsInParcheggio() + nJobInQueue <= PARCHEGGIO_SERVER) {
                service = distr.getService(1);
                s = MsqEvent.findOne(eventList, PARCHEGGIO_SERVER);

                /* Set server as active */
                eventList.get(s).setT(msqT.getCurrent() + service);
                eventList.get(s).setX(1);

                sumList.get(s).incrementService(service);
                sumList.get(s).incrementServed();
            }

            if (e == eventList.size() - 1 && internalEventList.isEmpty()) eventList.getLast().setX(0);
        } else {        /* Process a departure */
            this.index++;
            this.nJobInQueue--;

            // Update number of available cars in the center depending on where the car comes from
            if (eventListManager.incrementCarsInParcheggio() != 0) {
                this.nJobInQueue++;
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

            if (jobInBatch % B == 0 && jobInBatch <= B * K) {
                batchDuration = msqT.getCurrent() - msqT.getBatchTimer();

                calculateBatchStatistics();
                nBatch++;
                msqT.setBatchTimer(msqT.getCurrent());
            }

            /* Routing job to rental station */
            event = new MsqEvent(msqT.getCurrent(), 1, true);
            List<MsqEvent> intQueueNoleggio = eventListManager.getIntQueueNoleggio();
            intQueueNoleggio.add(event);
            eventListManager.setIntQueueNoleggio(intQueueNoleggio);
            eventListManager.getSystemEventsList().getFirst().setT(msqT.getCurrent());

            s = e;
            if (nJobInQueue >= PARCHEGGIO_SERVER) {        /* there is some jobs in queue, place another job in this server */
                service = distr.getService(1);
                eventList.get(s).setT(msqT.getCurrent() + service);

                sumList.get(s).incrementService(service);
                sumList.get(s).incrementServed();
            } else                                    /* no job in queue, simply remove it from server */
                eventList.get(s).setX(0);
        }

        eventListManager.setServerParcheggio(eventList);
        eventListManager.setIntQueueParcheggio(internalEventList);

        int nextEvent = MsqEvent.getNextEvent(eventList, PARCHEGGIO_SERVER);
        if (nextEvent == -1) {
            eventListManager.getSystemEventsList().get(2).setX(0);
            return;
        }

        eventListManager.getSystemEventsList().get(2).setT(eventList.get(nextEvent).getT());
    }

    @Override
    public void calculateBatchStatistics() {
        double avgPopulationInNode = area / batchDuration;
        double responseTime = area / index;

        batchParcheggio.insertAvgPopulationInNode(avgPopulationInNode, nBatch);
        batchParcheggio.insertResponseTime(responseTime, nBatch);

        System.out.println("Parcheggio batch statistics\n\n");
        System.out.println("E[N_s]: " + avgPopulationInNode);
        System.out.println("E[T_s]: " + responseTime);

        double sum = 0;
        for (int i = 1; i <= PARCHEGGIO_SERVER; i++) {
            sum += sumList.get(i).getService();
            sumList.get(i).setService(0);
            sumList.get(i).setServed(0);
        }

        double waitingTimeInQueue = (area - sum) / index;
        double avgPopulationInQueue = (area - sum) / batchDuration;
        double utilization = sum / (batchDuration * PARCHEGGIO_SERVER);

        System.out.println("E[T_q]: " + waitingTimeInQueue);
        System.out.println("E[N_q]: " + avgPopulationInQueue);
        System.out.println("Utilization: " + utilization);

        batchParcheggio.insertWaitingTimeInQueue(waitingTimeInQueue, nBatch);
        batchParcheggio.insertAvgPopulationInQueue(avgPopulationInQueue, nBatch);
        batchParcheggio.insertUtilization(utilization, nBatch);

        /* Reset parameters */
        area = 0;
        index = 0;
    }

    @Override
    public int getNumJob() {
        return this.jobInBatch;
    }

    @Override
    public void printResult() {
        DecimalFormat f = new DecimalFormat("#0.00000000");

        double responseTime = area / index;
        double avgPopulationInNode = area / msqT.getCurrent();

        System.out.println("Parcheggio\n\n");
        System.out.println("for " + index + " jobs the service node statistics are:\n\n");
        System.out.println("  avg interarrivals .. = " + eventListManager.getSystemEventsList().getFirst().getT() / index);
        System.out.println("  avg wait ........... = " + responseTime);
        System.out.println("  avg # in node ...... = " + avgPopulationInNode);

        for (int i = 1; i <= PARCHEGGIO_SERVER; i++) {
            area -= sumList.get(i).getService();
        }

        double waitingTime = area / index;
        double avgPopulationInQueue = area / msqT.getCurrent();
        System.out.println("  avg delay .......... = " + waitingTime);
        System.out.println("  avg # in queue ..... = " + avgPopulationInQueue);
        for (int i = 1; i <= PARCHEGGIO_SERVER; i++) {
            System.out.println("\t" + i + "\t\t" + f.format(sumList.get(i).getService() / msqT.getCurrent()) + "\t " + f.format(sumList.get(i).getService() / sumList.get(i).getServed()) + "\t " + f.format(((double) sumList.get(i).getServed() / index)));
        }
        System.out.println("\n");
    }
}


























