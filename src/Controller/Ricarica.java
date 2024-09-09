package Controller;

import Libs.Rvms;
import Model.MsqEvent;
import Model.MsqSum;
import Model.MsqT;
import Utils.*;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static Utils.Constants.*;

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

    private final ReplicationStats repRicarica = new ReplicationStats();
    private final SimulationResults batchRicarica = new SimulationResults();
    private final Distribution distr;
    private final Rvms rvms = new Rvms();
    private final FileCSVGenerator fileCSVGenerator = FileCSVGenerator.getInstance();

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

    @Override
    public void simpleSimulation() {
        List<MsqEvent> eventList = eventListManager.getServerRicarica();
        List<MsqEvent> internalEventList = eventListManager.getIntQueueRicarica();

        if (eventList.getFirst().getX() == 0 && internalEventList.isEmpty() && this.number == 0 && MsqEvent.findActiveServers(eventList, RICARICA_SERVER) == 0) return; /* no external arrivals, no internal arrivals and no jobs in the server */

        if (!internalEventList.isEmpty()) {
            eventList.getLast().setT(internalEventList.getFirst().getT());
            eventList.getLast().setX(1);
        }

        if ((e = MsqEvent.getNextEvent(eventList, RICARICA_SERVER + 1)) >= eventList.size()) return;
        msqT.setNext(eventList.get(e).getT());
        area += (msqT.getNext() - msqT.getCurrent()) * number;
        msqT.setCurrent(msqT.getNext());

        if(number < 0 )
            System.out.println("Porco cane");
        if (e == 0 || e == eventList.size() - 1) {   /* Check if event is an arrival (exogenous or endogenous) */
            this.number++;

            if (e == 0) {       /* Check if event is an external arrival */
                eventList.getFirst().setT(msqT.getCurrent() + distr.getArrival(2));     /* Get new arrival from exogenous arrival */

                if (eventListManager.getCarsInRicarica() + this.number > RICARICA_SERVER + RICARICA_MAX_QUEUE) {      /* New arrival but Ricarica's servers and queue are full */
                    this.number--;      /* Loss event */

                    rentalProfit.incrementPenalty();

                    int nextEvent = MsqEvent.getNextEvent(eventList, RICARICA_SERVER);
                    if (nextEvent == -1) {
                        eventListManager.getSystemEventsList().get(1).setX(0);
                        return;
                    }

                    eventListManager.getSystemEventsList().get(1).setT(eventList.get(nextEvent).getT());

                    return;
                }

                rentalProfit.incrementExternalCars();
                eventListManager.incrementCars();   /* New car in system */

                if (eventList.getFirst().getT() > STOP_FIN) {
                    eventList.getFirst().setX(0);   /* Set event as "not active" cause simulation end */

                    eventListManager.setServerRicarica(eventList);
                }
            } else if (e == eventList.size() - 1)
                internalEventList.removeFirst();

            // Place job in a server if there is a free server
            s = MsqEvent.findOne(eventList, RICARICA_SERVER);
            if (s != -1 && eventListManager.getCarsInRicarica() + MsqEvent.findActiveServers(eventList, RICARICA_SERVER) < RICARICA_SERVER) {
                service = distr.getService(2);

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
            if (eventListManager.incrementCarsInRicarica() != 0) {
                this.index--;
                this.number++;

                List<MsqEvent> eventListNoleggio = eventListManager.getServerNoleggio();

                int nextEventNoleggio = MsqEvent.getNextEvent(eventListNoleggio, NOLEGGIO_SERVER);
                if (nextEventNoleggio == -1)
                    eventListManager.getSystemEventsList().get(1).setT(msqT.getCurrent() + 1);
                else
                    eventListManager.getSystemEventsList().get(1).setT(
                            eventListNoleggio.get(nextEventNoleggio).getT() + 0.1
                    );

                return; // Ho raggiunto il numero massimo di macchine in Ricarica, devono restare in coda
            }

            /* Routing job to rental station */
            MsqEvent event = new MsqEvent(msqT.getCurrent(), 1, false);
            List<MsqEvent> intQueueNoleggio = eventListManager.getIntQueueNoleggio();
            intQueueNoleggio.add(event);
            eventListManager.setIntQueueNoleggio(intQueueNoleggio);

            s = e;
            if (eventListManager.getCarsInRicarica() + MsqEvent.findActiveServers(eventList, RICARICA_SERVER) < RICARICA_SERVER && MsqEvent.findActiveServers(eventList, RICARICA_SERVER) < this.number) {        /* there is some jobs in queue, place another job in this server */
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
    public void infiniteSimulation () {
        List<MsqEvent> eventList = eventListManager.getServerRicarica();
        List<MsqEvent> internalEventList = eventListManager.getIntQueueRicarica();

        if (eventList.getFirst().getX() == 0 && internalEventList.isEmpty() && this.number == 0 && MsqEvent.findActiveServers(eventList, RICARICA_SERVER) == 0) return; /* no external arrivals, no internal arrivals and no jobs in the server */

        if (!internalEventList.isEmpty()) {
            eventList.getLast().setT(internalEventList.getFirst().getT());
            eventList.getLast().setX(1);
        }

        if ((e = MsqEvent.getNextEvent(eventList, RICARICA_SERVER + 1)) >= eventList.size()) return;
        msqT.setNext(eventList.get(e).getT());
        area += (msqT.getNext() - msqT.getCurrent()) * number;
        msqT.setCurrent(msqT.getNext());

        if (e == 0 || e == eventList.size() - 1) {   /* Check if event is an arrival (exogenous or endogenous) */
            this.number++;

            if (e == 0) {       /* Check if event is an external arrival */
                eventList.getFirst().setT(msqT.getCurrent() + distr.getArrival(2));     /* Get new arrival from exogenous arrival */

                if (eventListManager.getCarsInRicarica() + this.number > RICARICA_SERVER + RICARICA_MAX_QUEUE) {      /* New arrival but Ricarica's servers and queue are full */
                    this.number--;      /* Loss event */

                    rentalProfit.incrementPenalty();

                    int nextEvent = MsqEvent.getNextEvent(eventList, RICARICA_SERVER);
                    if (nextEvent == -1) {
                        eventListManager.getSystemEventsList().get(1).setX(0);
                        return;
                    }

                    eventListManager.getSystemEventsList().get(1).setT(eventList.get(nextEvent).getT());

                    return;
                }

                rentalProfit.incrementExternalCars();
                eventListManager.incrementCars();   /* New car in system */

                if (eventList.getFirst().getT() > STOP_INF) {
                    eventList.getFirst().setX(0);   /* Set event as "not active" cause simulation end */

                    eventListManager.setServerRicarica(eventList);
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
            s = MsqEvent.findOne(eventList, RICARICA_SERVER);
            if (s != -1 && eventListManager.getCarsInRicarica() + MsqEvent.findActiveServers(eventList, RICARICA_SERVER) < RICARICA_SERVER) {
                service = distr.getService(2);

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
            if (eventListManager.incrementCarsInRicarica() != 0) {
                this.index--;
                this.number++;

                List<MsqEvent> eventListNoleggio = eventListManager.getServerNoleggio();

                int nextEventNoleggio = MsqEvent.getNextEvent(eventListNoleggio, NOLEGGIO_SERVER);
                if (nextEventNoleggio == -1)
                    eventListManager.getSystemEventsList().get(1).setT(msqT.getCurrent() + 1);
                else
                    eventListManager.getSystemEventsList().get(1).setT(
                            eventListNoleggio.get(nextEventNoleggio).getT() + 0.1
                    );

                return; // Ho raggiunto il numero massimo di macchine in Ricarica, devono restare in coda
            }

            /* Routing job to rental station */
            MsqEvent event = new MsqEvent(msqT.getCurrent(), 1, false);
            List<MsqEvent> intQueueNoleggio = eventListManager.getIntQueueNoleggio();
            intQueueNoleggio.add(event);
            eventListManager.setIntQueueNoleggio(intQueueNoleggio);

            s = e;
            if (eventListManager.getCarsInRicarica() + MsqEvent.findActiveServers(eventList, RICARICA_SERVER) < RICARICA_SERVER && MsqEvent.findActiveServers(eventList, RICARICA_SERVER) < this.number) {        /* there is some jobs in queue, place another job in this server */
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
    public int getJobInBatch() {
        return this.jobInBatch;
    }

    @Override
    public void printIteration(boolean isFinite, int event, int runNumber, double time) {
        double responseTime = area / index;
        double avgPopulationInNode = area / msqT.getCurrent();

        double area = this.area;
        for(int i = 1; i == RICARICA_SERVER; i++) {
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

        System.out.println("\n\nRicarica\n");
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

        double meanUtilization = 0;
        System.out.println("\nthe server statistics are:\n\n");
        System.out.println("\tserver\tutilization\t avg service\t share\n");
        for(int i = 1; i <= RICARICA_SERVER; i++) {
            System.out.println("\t" + i + "\t\t" + f.format(sumList.get(i).getService() / msqT.getCurrent()) + "\t " + f.format(sumList.get(i).getService() / sumList.get(i).getServed()) + "\t " + f.format(((double)sumList.get(i).getServed() / index)));
            meanUtilization += (sumList.get(i).getService() / msqT.getCurrent());
        }

        meanUtilization = meanUtilization / (RICARICA_SERVER - 1);

        /* Calculate recharged cost */
        rentalProfit.setRechargeCost((index * RECHARGE_COST));

        System.out.println("\n");

        if (waitingTime <= 0.0) waitingTime = 0L;
        if (avgPopulationInQueue <= 0.0) avgPopulationInQueue = 0L;

        if (runNumber > 0 && seed > 0)
            fileCSVGenerator.saveRepResults(RICARICA, runNumber, responseTime, avgPopulationInNode, waitingTime, avgPopulationInQueue);

        repRicarica.insertAvgPopulationInNode(avgPopulationInNode, runNumber - 1);
        repRicarica.insertUtilization(meanUtilization, runNumber - 1);
        repRicarica.insertResponseTime(responseTime, runNumber - 1);
        repRicarica.insertWaitingTimeInQueue(waitingTime, runNumber - 1);
        repRicarica.insertWaitingTimeInQueue(avgPopulationInQueue, runNumber - 1);
    }

    @Override
    public void printFinalStatsTransitorio() {
        double critical_value = rvms.idfStudent(K - 1, 1 - ALPHA/2);

        System.out.println("\n\nRicarica\n");

        repRicarica.setStandardDeviation(repRicarica.getWaitingTimeInQueue(), 4);
        System.out.println("Critical endpoints E[T_Q] =  " + repRicarica.getMeanWaitingTimeInQueue() + " +/- " + critical_value * repRicarica.getStandardDeviation(4) / (Math.sqrt(K - 1)));

        repRicarica.setStandardDeviation(repRicarica.getAvgPopulationInQueue(), 0);
        System.out.println("Critical endpoints E[N_Q] =  " + repRicarica.getMeanPopulationInQueue() + " +/- " + critical_value * repRicarica.getStandardDeviation(0) / (Math.sqrt(K - 1)));

        repRicarica.setStandardDeviation(repRicarica.getResponseTime(), 2);
        System.out.println("Critical endpoints E[T_S] =  " + repRicarica.getMeanResponseTime() + " +/- " + critical_value * repRicarica.getStandardDeviation(2) / (Math.sqrt(K - 1)));

        repRicarica.setStandardDeviation(repRicarica.getAvgPopulationInNode(), 1);
        System.out.println("Critical endpoints E[N_S] =  " + repRicarica.getMeanPopulationInNode() + " +/- " + critical_value * repRicarica.getStandardDeviation(1) / (Math.sqrt(K - 1)));

        repRicarica.setStandardDeviation(repRicarica.getUtilization(), 3);
        System.out.println("Critical endpoints rho =  " + repRicarica.getMeanUtilization() + " +/- " + critical_value * repRicarica.getStandardDeviation(3) / (Math.sqrt(K - 1)));
    }

    @Override
    public void printFinalStatsStazionario() {
        double critical_value = rvms.idfStudent(K - 1, 1 - ALPHA/2);

        System.out.println("\n\nRicarica\n");

        batchRicarica.setStandardDeviation(batchRicarica.getWaitingTimeInQueue(), 4);
        System.out.println("Critical endpoints E[T_Q] =  " + batchRicarica.getMeanWaitingTimeInQueue() + " +/- " + critical_value * batchRicarica.getStandardDeviation(4) / (Math.sqrt(K - 1)));

        batchRicarica.setStandardDeviation(batchRicarica.getAvgPopulationInQueue(), 0);
        System.out.println("Critical endpoints E[N_Q] =  " + batchRicarica.getMeanPopulationInQueue() + " +/- " + critical_value * batchRicarica.getStandardDeviation(0) / (Math.sqrt(K - 1)));

        batchRicarica.setStandardDeviation(batchRicarica.getResponseTime(), 2);
        System.out.println("Critical endpoints E[T_S] =  " + batchRicarica.getMeanResponseTime() + " +/- " + critical_value * batchRicarica.getStandardDeviation(2) / (Math.sqrt(K - 1)));

        batchRicarica.setStandardDeviation(batchRicarica.getAvgPopulationInNode(), 1);
        System.out.println("Critical endpoints E[N_S] =  " + batchRicarica.getMeanPopulationInNode() + " +/- " + critical_value * batchRicarica.getStandardDeviation(1) / (Math.sqrt(K - 1)));

        batchRicarica.setStandardDeviation(batchRicarica.getUtilization(), 3);
        System.out.println("Critical endpoints rho =  " + batchRicarica.getMeanUtilization() + " +/- " + critical_value * batchRicarica.getStandardDeviation(3) / (Math.sqrt(K - 1)));
    }
}
