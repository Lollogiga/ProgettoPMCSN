package Controller;

import Libs.Rvms;
import Model.MsqEvent;
import Model.MsqSum;
import Model.MsqT;
import Utils.*;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static Utils.Constants.*;

public class Parcheggio implements Center {
    private final EventListManager eventListManager;
    private final RentalProfit rentalProfit;

    long number = 0;                     /* number of jobs in the node         */
    int e;                               /* next event index                   */
    int s;                               /* server index                       */
    long index = 0;                      /* used to count processed jobs       */
    double area = 0.0;                   /* time integrated number in the node */
    double service;

    int nBatch = 0;
    int jobInBatch = 0;
    double batchDuration = 0L;

    private final MsqT msqT = new MsqT();

    // λ_ext, λ_int, s_1, s_2, ..., s_n
    private final List<MsqEvent> serverList = new ArrayList<>(2 + PARCHEGGIO_SERVER);
    private final List<MsqSum> sumList = new ArrayList<>(2 + PARCHEGGIO_SERVER);

    private final Distribution distr;
    private final Rvms rvms = new Rvms();

    private final ReplicationStats repParcheggio = new ReplicationStats();
    private final SimulationResults batchParcheggio = new SimulationResults();

    private final FileCSVGenerator fileCSVGenerator = FileCSVGenerator.getInstance();

    public Parcheggio() {
        eventListManager = EventListManager.getInstance();
        rentalProfit = RentalProfit.getInstance();

        distr = Distribution.getInstance();

        /* Initial servers setup */
        for (s = 0; s < PARCHEGGIO_SERVER + 2; s++) {
            serverList.add(s, new MsqEvent(0, 0));
            sumList.add(s, new MsqSum());
        }

        // First arrival event (External arrival) (car to park)
        double arrival = distr.getArrival(1);

        // Add this new event and setting time to arrival time
        serverList.set(0, new MsqEvent(arrival, 1));

        eventListManager.setServerParcheggio(serverList);
    }

    @Override
    public void simpleSimulation() {
        // TODO: penalty parcheggio

        List<MsqEvent> eventList = eventListManager.getServerParcheggio();

        // Exit condition : There are no external or internal arrivals, and I haven't processing job.
        if (eventList.getFirst().getX() == 0 && eventList.get(1).getX() == 0 && this.number == 0) return;

        if ((e = MsqEvent.getNextEvent(eventList)) == -1) return;
        msqT.setNext(eventList.get(e).getT());
        area += (msqT.getNext() - msqT.getCurrent()) * number;
        msqT.setCurrent(msqT.getNext());

        // Whether it is an external or internal arrival: (e > 2, is a server processing)
        if (e < 2) {
            this.number++;

            if (e == 0) eventList.getFirst().setT(msqT.getCurrent() + distr.getArrival(1));     /* Get new arrival from exogenous (external) arrival */

            s = MsqEvent.findOne(eventList);    /* Search for an idle Server */
            if (s != -1) {                      /* Found an idle server*/
                service = distr.getService(1);

                /* Set server as active */
                eventList.get(s).setT(msqT.getCurrent() +  service);  /* Let's calculate the end of service time */
                eventList.get(s).setX(1);

                sumList.get(s).incrementService(service);
                sumList.get(s).incrementServed();
            } else {/* All servers are busy */
                if (this.number - MsqEvent.findActiveServers(eventList) > PARCHEGGIO_MAX_QUEUE) {    /* If the queue is full */
                    this.number--;

                    rentalProfit.incrementPenalty();
                }
            }
        } else {    /* Processing a job */
            this.index++;
            this.number--;

            eventList.get(e).setX(2);       /* Current server is no more usable (e = 2 car is ready to be rented) */

            // TODO: avvisare Noleggio che ho una nuova macchina a disposizione (routing)
        }

        /* Get next Parcheggio's events*/
        int nextEvent = MsqEvent.getNextEvent(eventList);
        if (nextEvent == -1)
            eventListManager.getSystemEventsList().get(2).setX(0);

        eventListManager.getSystemEventsList().get(2).setT(eventList.get(nextEvent).getT());
    }

//    @Override
    public void simpleSimulatio1() {
        List<MsqEvent> eventList = eventListManager.getServerParcheggio();
        List<MsqEvent> internalEventList = eventListManager.getIntQueueParcheggio();

        if (eventList.getFirst().getX() == 0 && internalEventList.isEmpty() && this.number == 0 && MsqEvent.findActiveServers(eventList, PARCHEGGIO_SERVER) == 0) return; /* no external arrivals, no internal arrivals and no jobs in the server */

        if (!internalEventList.isEmpty()) {
            eventList.getLast().setT(internalEventList.getFirst().getT());
            eventList.getLast().setX(1);
        }

        if ((e = MsqEvent.getNextEvent(eventList, PARCHEGGIO_SERVER + 1)) >= eventList.size()) return;
        msqT.setNext(eventList.get(e).getT());
        area += (msqT.getNext() - msqT.getCurrent()) * number;
        msqT.setCurrent(msqT.getNext());

        if (e == 0 || e == eventList.size() - 1) {   /* Check if event is an arrival (exogenous or endogenous) */
            this.number++;

            if (e == 0) {       /* Check if event is an external arrival */
                eventList.getFirst().setT(msqT.getCurrent() + distr.getArrival(1));     /* Get new arrival from exogenous arrival */

                if (eventListManager.getCarsInParcheggio() + this.number > PARCHEGGIO_SERVER + PARCHEGGIO_MAX_QUEUE) {      /* New arrival but Parcheggio's servers and queue are full */
                    this.number--;      /* Loss event */

                    rentalProfit.incrementPenalty();

                    int nextEvent = MsqEvent.getNextEvent(eventList, PARCHEGGIO_SERVER);
                    if (nextEvent == -1) {
                        eventListManager.getSystemEventsList().get(2).setX(0);
                        return;
                    }

                    eventListManager.getSystemEventsList().get(2).setT(eventList.get(nextEvent).getT());

                    return;
                }

                rentalProfit.incrementExternalCars();
                eventListManager.incrementCars();   /* New car in system */

                if (eventList.getFirst().getT() > STOP_FIN) {
                    eventList.getFirst().setX(0);   /* Set event as "not active" cause simulation end */

                    eventListManager.setServerParcheggio(eventList);
                }
            } else if (e == eventList.size() - 1)
                internalEventList.removeFirst();

            // Place job in a server if there is a free server
            s = MsqEvent.findOne(eventList, PARCHEGGIO_SERVER);
            if (s != -1 && eventListManager.getCarsInParcheggio() + MsqEvent.findActiveServers(eventList, PARCHEGGIO_SERVER) < PARCHEGGIO_SERVER) {
                service = distr.getService(1);

                /* Set server as active */
                eventList.get(s).setT(msqT.getCurrent() +  service);
                eventList.get(s).setX(1);

                sumList.get(s).incrementService(service);
                sumList.get(s).incrementServed();
            }

            if (e == eventList.size() - 1 && internalEventList.isEmpty() && s != -1) eventList.getLast().setX(0);
        } else {
            this.index++;
            this.number--;

            // Update number of available cars in the center depending on where the car comes from
            if (eventListManager.incrementCarsInParcheggio() != 0) {
                this.index--;
                this.number++;

                List<MsqEvent> eventListNoleggio = eventListManager.getServerNoleggio();

                int nextEventNoleggio = MsqEvent.getNextEvent(eventListNoleggio, NOLEGGIO_SERVER);
                if (nextEventNoleggio == -1)
                    eventListManager.getSystemEventsList().get(2).setT(msqT.getCurrent() + 1);
                else
                    eventListManager.getSystemEventsList().get(2).setT(
                        eventListNoleggio.get(nextEventNoleggio).getT() + 0.1
                    );

                return; // Ho raggiunto il numero massimo di macchine nel parcheggio, devono restare in coda
            }

            /* Routing job to rental station */
            MsqEvent event = new MsqEvent(msqT.getCurrent(), 1, true);
            List<MsqEvent> intQueueNoleggio = eventListManager.getIntQueueNoleggio();
            intQueueNoleggio.add(event);
            eventListManager.setIntQueueNoleggio(intQueueNoleggio);

            s = e;
            if (eventListManager.getCarsInParcheggio() + MsqEvent.findActiveServers(eventList, PARCHEGGIO_SERVER) < PARCHEGGIO_SERVER && MsqEvent.findActiveServers(eventList, PARCHEGGIO_SERVER) < this.number) {        /* there is some jobs in queue, place another job in this server */
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

    /* Infinite horizon simulation */
    @Override
    public void infiniteSimulation() {
        List<MsqEvent> eventList = eventListManager.getServerParcheggio();
        List<MsqEvent> internalEventList = eventListManager.getIntQueueParcheggio();

        if (eventList.getFirst().getX() == 0 && internalEventList.isEmpty() && this.number == 0 && MsqEvent.findActiveServers(eventList, PARCHEGGIO_SERVER) == 0) return; /* no external arrivals, no internal arrivals and no jobs in the server */

        if (!internalEventList.isEmpty()) {
            eventList.getLast().setT(internalEventList.getFirst().getT());
            eventList.getLast().setX(1);
        }

        if ((e = MsqEvent.getNextEvent(eventList, PARCHEGGIO_SERVER + 1)) >= eventList.size()) return;
        msqT.setNext(eventList.get(e).getT());
        area += (msqT.getNext() - msqT.getCurrent()) * number;
        msqT.setCurrent(msqT.getNext());

        if (e == 0 || e == eventList.size() - 1) {   /* Check if event is an arrival (exogenous or endogenous) */
            this.number++;

            if (e == 0) {       /* Check if event is an external arrival */
                eventList.getFirst().setT(msqT.getCurrent() + distr.getArrival(1));     /* Get new arrival from exogenous arrival */

                if (eventListManager.getCarsInParcheggio() + this.number > PARCHEGGIO_SERVER + PARCHEGGIO_MAX_QUEUE) {      /* New arrival but Parcheggio's servers and queue are full */
                    this.number--;      /* Loss event */

                    rentalProfit.incrementPenalty();

                    int nextEvent = MsqEvent.getNextEvent(eventList, PARCHEGGIO_SERVER);
                    if (nextEvent == -1) {
                        eventListManager.getSystemEventsList().get(2).setX(0);
                        return;
                    }

                    eventListManager.getSystemEventsList().get(2).setT(eventList.get(nextEvent).getT());

                    return;
                }

                rentalProfit.incrementExternalCars();
                eventListManager.incrementCars();   /* New car in system */

                if (eventList.getFirst().getT() > STOP_INF) {
                    eventList.getFirst().setX(0);   /* Set event as "not active" cause simulation end */

                    eventListManager.setServerParcheggio(eventList);
                }
            } else if (e == eventList.size() - 1)
                internalEventList.removeFirst();

            BatchMeans.incrementJobInBatch();
            jobInBatch++;

            if (jobInBatch % B == 0 && jobInBatch <= B * K) {
                batchDuration = msqT.getCurrent() - msqT.getBatchTimer();

                calculateBatchStatistics();
                nBatch++;
                msqT.setBatchTimer(msqT.getCurrent());
            }

            // Place job in a server if there is a free server
            s = MsqEvent.findOne(eventList, PARCHEGGIO_SERVER);
            if (s != -1 && eventListManager.getCarsInParcheggio() + MsqEvent.findActiveServers(eventList, PARCHEGGIO_SERVER) < PARCHEGGIO_SERVER) {
                service = distr.getService(1);

                /* Set server as active */
                eventList.get(s).setT(msqT.getCurrent() +  service);
                eventList.get(s).setX(1);

                sumList.get(s).incrementService(service);
                sumList.get(s).incrementServed();
            }

            if (e == eventList.size() - 1 && internalEventList.isEmpty() && s != -1) eventList.getLast().setX(0);
        } else {
            this.index++;
            this.number--;

            // Update number of available cars in the center depending on where the car comes from
            if (eventListManager.incrementCarsInParcheggio() != 0) {
                this.index--;
                this.number++;

                List<MsqEvent> eventListNoleggio = eventListManager.getServerNoleggio();

                int nextEventNoleggio = MsqEvent.getNextEvent(eventListNoleggio, NOLEGGIO_SERVER);
                if (nextEventNoleggio == -1)
                    eventListManager.getSystemEventsList().get(2).setT(msqT.getCurrent() + 1);
                else
                    eventListManager.getSystemEventsList().get(2).setT(
                            eventListNoleggio.get(nextEventNoleggio).getT() + 0.1
                    );

                return; // Ho raggiunto il numero massimo di macchine nel parcheggio, devono restare in coda
            }

            /* Routing job to rental station */
            MsqEvent event = new MsqEvent(msqT.getCurrent(), 1, true);
            List<MsqEvent> intQueueNoleggio = eventListManager.getIntQueueNoleggio();
            intQueueNoleggio.add(event);
            eventListManager.setIntQueueNoleggio(intQueueNoleggio);

            s = e;
            if (eventListManager.getCarsInParcheggio() + MsqEvent.findActiveServers(eventList, PARCHEGGIO_SERVER) < PARCHEGGIO_SERVER && MsqEvent.findActiveServers(eventList, PARCHEGGIO_SERVER) < this.number) {        /* there is some jobs in queue, place another job in this server */
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
        for(int i = 1; i <= PARCHEGGIO_SERVER; i++) {
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

        fileCSVGenerator.saveBatchResults(nBatch, avgPopulationInQueue);

        /* Reset parameters */
        area = 0;
        index = 0;
    }

    @Override
    public int getNumJob() {
        return this.jobInBatch;
    }

    @Override
    public int getJobInBatch() {
        return this.jobInBatch;
    }

    @Override
    public void printIteration(boolean isFinite, int event, int runNumber, double time) {
        double responseTime = area / index;
        double avgPopulationInNode = area / msqT.getCurrent();

        double area = this.area;
        for(int i = 1; i == PARCHEGGIO_SERVER; i++) {
            area -= sumList.get(i).getService();
        }

        double waitingTime = area / index;
        double avgPopulationInQueue = area / msqT.getCurrent();

        FileCSVGenerator.writeFile(isFinite, event, runNumber, time, responseTime, avgPopulationInNode, waitingTime, avgPopulationInQueue);
    }

    @Override
    public void printResult(int runNumber, long seed) {
        DecimalFormat f = new DecimalFormat("#0.00000000");

        double responseTime = area / index;
        double avgPopulationInNode = area / msqT.getCurrent();

        System.out.println("\n\nParcheggio\n");
        System.out.println("for " + index + " jobs the service node statistics are:\n\n");
        System.out.println("  avg interarrivals .. = " + eventListManager.getSystemEventsList().getFirst().getT() / index);
        System.out.println("  avg wait ........... = " + responseTime);
        System.out.println("  avg # in node ...... = " + avgPopulationInNode);

        for(int i = 1; i <= PARCHEGGIO_SERVER; i++) {
            area -= sumList.get(i).getService();
        }

        double waitingTime = area / index;
        double avgPopulationInQueue = area / msqT.getCurrent();
        System.out.println("  avg delay .......... = " + waitingTime);
        System.out.println("  avg # in queue ..... = " + avgPopulationInQueue);

        double meanUtilization = 0;
        System.out.println("\nthe server statistics are:\n\n");
        System.out.println("\tserver\tutilization\t avg service\t share\n");
        for(int i = 1; i <= PARCHEGGIO_SERVER; i++) {
            System.out.println("\t" + i + "\t\t" + f.format(sumList.get(i).getService() / msqT.getCurrent()) + "\t " + f.format(sumList.get(i).getService() / sumList.get(i).getServed()) + "\t " + f.format(((double) sumList.get(i).getServed() / index)));
            meanUtilization += (sumList.get(i).getService() / msqT.getCurrent());
        }
        System.out.println("\n");

        meanUtilization = meanUtilization / (PARCHEGGIO_SERVER - 1);

        if (waitingTime <= 0.0) waitingTime = 0L;
        if (avgPopulationInQueue <= 0.0) avgPopulationInQueue = 0L;

        if (runNumber > 0 && seed > 0)
            fileCSVGenerator.saveRepResults(PARCHEGGIO, runNumber, responseTime, avgPopulationInNode, waitingTime, avgPopulationInQueue);

        repParcheggio.insertAvgPopulationInNode(avgPopulationInNode, runNumber - 1);
        repParcheggio.insertUtilization(meanUtilization, runNumber - 1);
        repParcheggio.insertResponseTime(responseTime, runNumber - 1);
        repParcheggio.insertWaitingTimeInQueue(waitingTime, runNumber - 1);
        repParcheggio.insertWaitingTimeInQueue(avgPopulationInQueue, runNumber - 1);
    }

    @Override
    public void printFinalStatsTransitorio() {
        double critical_value = rvms.idfStudent(K - 1, 1 - ALPHA/2);

        System.out.println("\n\nParcheggio\n");

        repParcheggio.setStandardDeviation(repParcheggio.getWaitingTimeInQueue(), 4);
        System.out.println("Critical endpoints E[T_Q] =  " + repParcheggio.getMeanWaitingTimeInQueue() + " +/- " + critical_value * repParcheggio.getStandardDeviation(4) / (Math.sqrt(K - 1)));

        repParcheggio.setStandardDeviation(repParcheggio.getAvgPopulationInQueue(), 0);
        System.out.println("Critical endpoints E[N_Q] =  " + repParcheggio.getMeanPopulationInQueue() + " +/- " + critical_value * repParcheggio.getStandardDeviation(0) / (Math.sqrt(K - 1)));

        repParcheggio.setStandardDeviation(repParcheggio.getResponseTime(), 2);
        System.out.println("Critical endpoints E[T_S] =  " + repParcheggio.getMeanResponseTime() + " +/- " + critical_value * repParcheggio.getStandardDeviation(2) / (Math.sqrt(K - 1)));

        repParcheggio.setStandardDeviation(repParcheggio.getAvgPopulationInNode(), 1);
        System.out.println("Critical endpoints E[N_S] =  " + repParcheggio.getMeanPopulationInNode() + " +/- " + critical_value * repParcheggio.getStandardDeviation(1) / (Math.sqrt(K - 1)));

        repParcheggio.setStandardDeviation(repParcheggio.getUtilization(), 3);
        System.out.println("Critical endpoints rho =  " + repParcheggio.getMeanUtilization() + " +/- " + critical_value * repParcheggio.getStandardDeviation(3) / (Math.sqrt(K - 1)));

    }

    @Override
    public void printFinalStatsStazionario() {
        double critical_value = rvms.idfStudent(K - 1, 1 - ALPHA/2);

        System.out.println("\n\nParcheggio\n");

        batchParcheggio.setStandardDeviation(batchParcheggio.getWaitingTimeInQueue(), 4);
        System.out.println("Critical endpoints E[T_Q] =  " + batchParcheggio.getMeanWaitingTimeInQueue() + " +/- " + critical_value * batchParcheggio.getStandardDeviation(4) / (Math.sqrt(K - 1)));

        batchParcheggio.setStandardDeviation(batchParcheggio.getAvgPopulationInQueue(), 0);
        System.out.println("Critical endpoints E[N_Q] =  " + batchParcheggio.getMeanPopulationInQueue() + " +/- " + critical_value * batchParcheggio.getStandardDeviation(0) / (Math.sqrt(K - 1)));

        batchParcheggio.setStandardDeviation(batchParcheggio.getResponseTime(), 2);
        System.out.println("Critical endpoints E[T_S] =  " + batchParcheggio.getMeanResponseTime() + " +/- " + critical_value * batchParcheggio.getStandardDeviation(2) / (Math.sqrt(K - 1)));

        batchParcheggio.setStandardDeviation(batchParcheggio.getAvgPopulationInNode(), 1);
        System.out.println("Critical endpoints E[N_S] =  " + batchParcheggio.getMeanPopulationInNode() + " +/- " + critical_value * batchParcheggio.getStandardDeviation(1) / (Math.sqrt(K - 1)));

        batchParcheggio.setStandardDeviation(batchParcheggio.getUtilization(), 3);
        System.out.println("Critical endpoints rho =  " + batchParcheggio.getMeanUtilization() + " +/- " + critical_value * batchParcheggio.getStandardDeviation(3) / (Math.sqrt(K - 1)));
    }
}