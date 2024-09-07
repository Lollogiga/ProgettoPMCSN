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

public class Ricarica implements Center {
    long number = 0;                     /* number of jobs in the node         */
    int e;                               /* next event index                   */
    int s;                               /* server index                       */
    long index = 0;                      /* used to count processed jobs       */
    double area = 0.0;           /* time integrated number in the node */
    double service;

    int nBatch = 0;
    int jobInBatch = 0;
    double batchDuration = 0L;

    private final EventListManager eventListManager;
    private final RentalProfit rentalProfit;

    private final MsqT msqT = new MsqT();

    private final List<MsqEvent> serverList = new ArrayList<>(RICARICA_SERVER + 2);
    private final List<MsqSum> sumList = new ArrayList<>(RICARICA_SERVER + 2);

    private final SimulationResults batchRicarica = new SimulationResults();
    private final Distribution distr;

    public Ricarica() {
        eventListManager = EventListManager.getInstance();
        rentalProfit = RentalProfit.getInstance();
        distr = Distribution.getInstance();

        /* Initial servers setup */
        for (s = 0; s < RICARICA_SERVER + 2; s++) {
            serverList.add(s, new MsqEvent(0, 0));
            sumList.add(s, new MsqSum());
        }

        // First arrival event (car to charge)
        double arrival = distr.getArrival(2);

        // Add this new event and setting time to arrival time
        serverList.set(0, new MsqEvent(arrival, 1));

        eventListManager.setServerRicarica(serverList);
    }

    /* Finite horizon simulation */
    @Override
    public void simpleSimulation() {
        MsqEvent event;

        List<MsqEvent> eventList = eventListManager.getServerRicarica();
        List<MsqEvent> internalEventList = eventListManager.getIntQueueRicarica();

        /* no external arrivals, no internal arrivals and no jobs in the server */
        if (eventList.getFirst().getX() == 0 && internalEventList.isEmpty() && this.number == 0) return;

        if (!internalEventList.isEmpty()) {
            eventList.getLast().setT(internalEventList.getFirst().getT());
            eventList.getLast().setX(1);
        }

        double msqCurr = msqT.getCurrent();

        int e = MsqEvent.getNextEvent(serverList, RICARICA_SERVER + 1);
        msqT.setNext(eventList.get(e).getT());
        area += (msqT.getNext() - msqT.getCurrent()) * number;
        msqT.setCurrent(msqT.getNext());

        if (e == 0 || e == eventList.size() - 1) {   /* Check if event is an arrival */
            this.number++;

            if (e == 0) {   /* Check if event is an external arrival */
                eventList.getFirst().setT(msqT.getCurrent() + distr.getArrival(2)); /* Get new arrival from exogenous arrival */

                if (eventListManager.getCarsInRicarica() + this.number > RICARICA_SERVER + RICARICA_MAX_QUEUE) { /* New arrival but Ricarica's queue is full */
                    this.number--; /* Loss event */
                    eventListManager.decrementCars();
                    rentalProfit.incrementPenalty();

                    eventListManager.getSystemEventsList().get(1).setT(eventList.getFirst().getT());

                    return;
                }

                eventListManager.incrementCars();   /* New car in system */

                if (eventList.getFirst().getT() > STOP_FIN) {
                    eventList.getFirst().setX(0);
                    eventListManager.setServerRicarica(eventList);
                }

                event = eventList.getFirst();
            } else {    /* Event is an internal arrival */
                event = internalEventList.getFirst();
                internalEventList.removeFirst();
            }

            if (eventListManager.getCarsInRicarica() + number <= RICARICA_SERVER) {
                service = distr.getService(2);
                s = MsqEvent.findOne(serverList, RICARICA_SERVER);

                /* Set server as active */
                eventList.get(s).setT(msqT.getCurrent() +  service);
                eventList.get(s).setX(1);

                sumList.get(s).incrementService(service);
                sumList.get(s).incrementServed();
            }

            if (e == eventList.size() - 1 && internalEventList.isEmpty()) eventList.getLast().setX(0);
        } else {        /* Process a departure */
            this.index++;
            this.number--;

            // Update number of available cars in the center depending on where the car comes from
            if (eventListManager.incrementCarsInRicarica() != 0) {
                this.number++;
                this.index--;

                List<MsqEvent> eventListNoleggio = eventListManager.getServerNoleggio();
                int nextEventNoleggio = MsqEvent.getNextEvent(eventListNoleggio, NOLEGGIO_SERVER);

                eventListManager.getSystemEventsList().get(1).setT(
                        eventListNoleggio.get(nextEventNoleggio).getT()
                );

                return; // Ho raggiunto il numero massimo di macchine in ricarica, devono restare in coda
            }

            /* Routing job to rental station */
            event = new MsqEvent(msqT.getCurrent(), eventList.get(e).getX(), false);
            List<MsqEvent> intQueueNoleggio = eventListManager.getIntQueueNoleggio();
            intQueueNoleggio.add(event);
            eventListManager.setIntQueueNoleggio(intQueueNoleggio);
            eventListManager.getSystemEventsList().getFirst().setT(msqT.getCurrent());

            s = e;
            if (number >= RICARICA_SERVER) {        /* there is some jobs in queue, place another job in this server */
                service = distr.getService(2);
                eventList.get(s).setT(msqT.getCurrent() + service);

                sumList.get(s).incrementService(service);
                sumList.get(s).incrementServed();
            } else                                    /* no job in queue, simply remove it from server */
                eventList.get(s).setX(0);
        }

        eventListManager.setServerRicarica(eventList);
        eventListManager.setIntQueueRicarica(internalEventList);

        int nextEvent = MsqEvent.getNextEvent(eventList, RICARICA_SERVER);
        if (nextEvent == -1) {
            eventListManager.getSystemEventsList().get(1).setX(0);
            return;
        }

        eventListManager.getSystemEventsList().get(1).setT(eventList.get(nextEvent).getT());
    }

    /* Infinite horizon simulation */
    @Override
    public void infiniteSimulation() {
        MsqEvent event;

        List<MsqEvent> eventList = eventListManager.getServerRicarica();
        List<MsqEvent> internalEventList = eventListManager.getIntQueueRicarica();

        /* no external arrivals, no internal arrivals and no jobs in the server */
        if (eventList.getFirst().getX() == 0 && internalEventList.isEmpty() && this.number == 0) return;

        if (!internalEventList.isEmpty()) {
            eventList.getLast().setT(internalEventList.getFirst().getT());
            eventList.getLast().setX(1);
        }

        double msqCurr = msqT.getCurrent();

        int e = MsqEvent.getNextEvent(serverList, RICARICA_SERVER + 1);
        msqT.setNext(eventList.get(e).getT());
        area += (msqT.getNext() - msqT.getCurrent()) * number;
        msqT.setCurrent(msqT.getNext());

        if (e == 0 || e == eventList.size() - 1) {   /* Check if event is an arrival */
            this.number++;

            BatchMeans.incrementJobInBatch();
            jobInBatch++;

            if (e == 0) {   /* Check if event is an external arrival */
                eventList.getFirst().setT(msqT.getCurrent() + distr.getArrival(2)); /* Get new arrival from exogenous arrival */

                if (eventListManager.getCarsInRicarica() + this.number > RICARICA_SERVER + RICARICA_MAX_QUEUE) { /* New arrival but Ricarica's queue is full */
                    this.number--; /* Loss event */
                    eventListManager.decrementCars();
                    eventListManager.getSystemEventsList().get(1).setT(eventList.getFirst().getT());

                    return;
                }

                eventListManager.incrementCars();   /* New car in system */

                if (eventList.getFirst().getT() > STOP_INF) {
                    eventList.getFirst().setX(0);
                    eventListManager.setServerRicarica(eventList);
                }

                event = eventList.getFirst();
            } else {    /* Event is an internal arrival */
                event = internalEventList.getFirst();
                internalEventList.removeFirst();
            }

            if (eventListManager.getCarsInRicarica() + number <= RICARICA_SERVER) {
                service = distr.getService(2);
                s = MsqEvent.findOne(serverList, RICARICA_SERVER);

                /* Set server as active */
                eventList.get(s).setT(msqT.getCurrent() +  service);
                eventList.get(s).setX(1);

                sumList.get(s).incrementService(service);
                sumList.get(s).incrementServed();
            }

            if (e == eventList.size() - 1 && internalEventList.isEmpty()) eventList.getLast().setX(0);
        } else {        /* Process a departure */
            this.index++;
            this.number--;

            // Update number of available cars in the center depending on where the car comes from
            if (eventListManager.incrementCarsInRicarica() != 0) {
                this.number++;
                this.index--;

                List<MsqEvent> eventListNoleggio = eventListManager.getServerNoleggio();
                int nextEventNoleggio = MsqEvent.getNextEvent(eventListNoleggio, NOLEGGIO_SERVER);

                eventListManager.getSystemEventsList().get(1).setT(
                        eventListNoleggio.get(nextEventNoleggio).getT()
                );

                return; // Ho raggiunto il numero massimo di macchine in ricarica, devono restare in coda
            }

            if (jobInBatch % B == 0 && jobInBatch <= B * K) {
                batchDuration = msqT.getCurrent() - msqT.getBatchTimer();

                calculateBatchStatistics();
                nBatch++;
                msqT.setBatchTimer(msqT.getCurrent());
            }

            /* Routing job to rental station */
            event = new MsqEvent(msqT.getCurrent(), eventList.get(e).getX(), false);
            List<MsqEvent> intQueueNoleggio = eventListManager.getIntQueueNoleggio();
            intQueueNoleggio.add(event);
            eventListManager.setIntQueueNoleggio(intQueueNoleggio);
            eventListManager.getSystemEventsList().getFirst().setT(msqT.getCurrent());

            s = e;
            if (number >= RICARICA_SERVER) {        /* there is some jobs in queue, place another job in this server */
                service = distr.getService(2);
                eventList.get(s).setT(msqT.getCurrent() + service);

                sumList.get(s).incrementService(service);
                sumList.get(s).incrementServed();
            } else                                    /* no job in queue, simply remove it from server */
                eventList.get(s).setX(0);
        }

        eventListManager.setServerRicarica(eventList);
        eventListManager.setIntQueueRicarica(internalEventList);

        int nextEvent = MsqEvent.getNextEvent(eventList, RICARICA_SERVER);
        if (nextEvent == -1) {
            eventListManager.getSystemEventsList().get(1).setX(0);
            return;
        }

        eventListManager.getSystemEventsList().get(1).setT(eventList.get(nextEvent).getT());
    }

    @Override
    public void calculateBatchStatistics() {
        double avgPopulationInNode = area / batchDuration;
        double responseTime = area / index;

        batchRicarica.insertAvgPopulationInNode(avgPopulationInNode, nBatch);
        batchRicarica.insertResponseTime(responseTime, nBatch);

        System.out.println("Ricarica batch statistics\n\n");
        System.out.println("E[N_s]: " + avgPopulationInNode);
        System.out.println("E[T_s]: " + responseTime);

        double sum = 0;
        for(int i = 1; i <= RICARICA_SERVER; i++) {
            sum += sumList.get(i).getService();
            sumList.get(i).setService(0);
            sumList.get(i).setServed(0);
        }

        double waitingTimeInQueue = (area - sum) / index;
        double avgPopulationInQueue = (area - sum) / batchDuration;
        double utilization = sum / (batchDuration * RICARICA_SERVER);

        batchRicarica.insertWaitingTimeInQueue(waitingTimeInQueue, nBatch);
        batchRicarica.insertAvgPopulationInQueue(avgPopulationInQueue, nBatch);
        batchRicarica.insertUtilization(utilization, nBatch);

        System.out.println("E[T_q]: " + waitingTimeInQueue);
        System.out.println("E[N_q]: " + avgPopulationInQueue);
        System.out.println("Utilization: " + utilization);

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

        System.out.println("Ricarica\n\n");
        System.out.println("for " + index + " jobs the service node statistics are:\n\n");
        System.out.println("  avg interarrivals .. = " + eventListManager.getSystemEventsList().getFirst().getT() / index);
        System.out.println("  avg wait ........... = " + responseTime);
        System.out.println("  avg # in node ...... = " + avgPopulationInNode);

        for(int i = 1; i <= RICARICA_SERVER; i++) {
            area -= sumList.get(i).getService();
        }

        double waitingTime = area / index;
        double avgPopulationInQueue = area / msqT.getCurrent();
        System.out.println("  avg delay .......... = " + waitingTime);
        System.out.println("  avg # in queue ..... = " + avgPopulationInQueue);
        System.out.println("\nthe server statistics are:\n\n");
        System.out.println("\tserver\tutilization\t avg service\t share\n");
        for(int i = 1; i <= RICARICA_SERVER; i++) {
            System.out.println("\t" + i + "\t\t" + f.format(sumList.get(i).getService() / msqT.getCurrent()) + "\t " + f.format(sumList.get(i).getService() / sumList.get(i).getServed()) + "\t " + f.format(((double)sumList.get(i).getServed() / index)));
        }

        /* Calculate recharged cost */
        rentalProfit.setRechargeCost((index * RECHARGE_COST));

        System.out.println("\n");
    }
}
