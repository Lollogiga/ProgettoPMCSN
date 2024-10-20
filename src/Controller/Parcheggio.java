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

    private long seed = 0L;

    private final MsqT msqT = new MsqT();

    // λ_ext, λ_int, s_1, s_2, ..., s_n
    private final List<MsqEvent> serverList = new ArrayList<>(2 + PARCHEGGIO_SERVER);
    private final List<MsqSum> sumList = new ArrayList<>(2 + PARCHEGGIO_SERVER);

    private final Distribution distr;
    private final Rvms rvms = new Rvms();

    private final SimulationResults batchParcheggio = new SimulationResults();
    private final FileCSVGenerator fileCSVGenerator = FileCSVGenerator.getInstance();

    /* Singleton types
     * 0 -> Strada
     * 1 -> Ricarica
     * 2 -> Parcheggio
     * 3 -> Noleggio */
    private final ReplicationStats repParcheggio = ReplicationStats.getInstance(2);

    public Parcheggio() {
        eventListManager = EventListManager.getInstance();
        rentalProfit = RentalProfit.getInstance();

        distr = Distribution.getInstance();

        /* Initial servers setup */
        for (s = 0; s < 2 + PARCHEGGIO_SERVER; s++) {
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

            if (e == 1) eventList.get(1).setX(0);

            s = MsqEvent.findOne(eventList);    /* Search for an idle Server */
            if (s != -1) {                      /* Found an idle server*/
                service = distr.getService(1);

                /* Set server as active */
                eventList.get(s).setT(msqT.getCurrent() +  service);  /* Let's calculate the end of service time */
                eventList.get(s).setX(1);

                sumList.get(s).incrementService(service);
                sumList.get(s).incrementServed();
            } else {        /* All servers are busy */
                if (this.number - MsqEvent.findActiveServers(eventList) > PARCHEGGIO_MAX_QUEUE) {    /* If the queue is full */
                    this.number--;

                    rentalProfit.incrementPenalty();
                }
            }
        } else {    /* Processing a job */
            this.index++;
            this.number--;

            eventList.get(e).setX(2);       /* Current server is no more usable (e = 2 car is ready to be rented) */
        }

        /* Get next Parcheggio's events */
        int nextEvent = MsqEvent.getNextEvent(eventList);
        if (nextEvent == -1)
            eventListManager.getSystemEventsList().get(2).setX(0);

        eventListManager.getSystemEventsList().get(2).setT(eventList.get(nextEvent).getT());
    }

    /* Infinite horizon simulation */
    @Override
    public void infiniteSimulation() {
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

            if (e == 1) eventList.get(1).setX(0);

            BatchMeans.incrementJobInBatch();
            jobInBatch++;

            if (jobInBatch % B == 0 && jobInBatch <= B * K) {
                batchDuration = msqT.getCurrent() - msqT.getBatchTimer();

                calculateBatchStatistics();
                nBatch++;
                msqT.setBatchTimer(msqT.getCurrent());
            }

            s = MsqEvent.findOne(eventList);    /* Search for an idle Server */
            if (s != -1) {                      /* Found an idle server */
                service = distr.getService(1);

                /* Set server as active */
                eventList.get(s).setT(msqT.getCurrent() +  service);  /* Let's calculate the end of service time */
                eventList.get(s).setX(1);

                sumList.get(s).incrementService(service);
                sumList.get(s).incrementServed();
            } else {        /* All servers are busy */
                if (this.number - MsqEvent.findActiveServers(eventList) > PARCHEGGIO_MAX_QUEUE) {    /* If the queue is full */
                    this.number--;

                    rentalProfit.incrementPenalty();
                }
            }
        } else {    /* Processing a job */
            this.index++;
            this.number--;

            eventList.get(e).setX(2);       /* Current server is no more usable (e = 2 car is ready to be rented) */
        }

        /* Get next Parcheggio's events */
        int nextEvent = MsqEvent.getNextEvent(eventList);
        if (nextEvent == -1)
            eventListManager.getSystemEventsList().get(2).setX(0);

        eventListManager.getSystemEventsList().get(2).setT(eventList.get(nextEvent).getT());
    }

    @Override
    public void calculateBatchStatistics() {
        double avgPopulationInNode = area / batchDuration;
        double responseTime = area / index;

        batchParcheggio.insertAvgPopulationInNode(avgPopulationInNode, nBatch);
        batchParcheggio.insertResponseTime(responseTime, nBatch);

        System.out.println("\n\nParcheggio batch statistics\n");
        System.out.println("E[N_S]: " + avgPopulationInNode);
        System.out.println("E[T_S]: " + responseTime / 60);

        double sum = 0;
        for(int i = 2; i < PARCHEGGIO_SERVER + 2; i++) {
            sum += sumList.get(i).getService();
            sumList.get(i).setService(0);
            sumList.get(i).setServed(0);
        }

        double utilization = sum / (batchDuration * PARCHEGGIO_SERVER);

        batchParcheggio.insertUtilization(utilization, nBatch);

        System.out.println("Utilization: " + utilization);

        fileCSVGenerator.saveBatchResults(nBatch, avgPopulationInNode);

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
    public void setSeed(long seed) {
        this.seed = seed;
    }

    @Override
    public void printIteration(boolean isFinite, long seed, int event, int runNumber, double time) {
        double responseTime = area / index;
        double avgPopulationInNode = area / msqT.getCurrent();

        double waitingTime = 0;
        double avgPopulationInQueue = 0;

        FileCSVGenerator.writeRepData(isFinite, seed, event, runNumber, time, responseTime, avgPopulationInNode, waitingTime, avgPopulationInQueue);
    }

    @Override
    public void printResult(int runNumber, long seed) {
        DecimalFormat f = new DecimalFormat("#0.00000000");

        double responseTime = area / index;
        double avgPopulationInNode = area / msqT.getCurrent();

        System.out.println("\n\nParcheggio\n");
        System.out.println("for " + index + " jobs the service node statistics are:\n\n");
        System.out.println("  avg interarrivals .. = " + (eventListManager.getSystemEventsList().getFirst().getT() / index) / 60);
        System.out.println("  avg wait ........... = " + responseTime / 60);
        System.out.println("  avg # in node ...... = " + avgPopulationInNode);

        for(int i = 2; i < PARCHEGGIO_SERVER + 2; i++) {
            area -= sumList.get(i).getService();
        }

        double waitingTime = area / index;
        double avgPopulationInQueue = area / msqT.getCurrent();

        double meanUtilization = 0;
        System.out.println("\nthe server statistics are:\n\n");
        System.out.println("\tserver\tutilization\t avg service\t share\n");
        for(int i = 2; i < PARCHEGGIO_SERVER + 2; i++) {
            System.out.println("\t" + (i-1) + "\t\t" + f.format(sumList.get(i).getService() / msqT.getCurrent()) + "\t " + f.format(sumList.get(i).getService() / sumList.get(i).getServed()) + "\t " + f.format(((double) sumList.get(i).getServed() / index)));
            meanUtilization += (sumList.get(i).getService() / msqT.getCurrent());
        }
        System.out.println("\n");

        meanUtilization = meanUtilization / PARCHEGGIO_SERVER;

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

        repParcheggio.setStandardDeviation(repParcheggio.getResponseTime(), 2);
        System.out.println("Critical endpoints E[T_S] =  " + repParcheggio.getMeanResponseTime() / 60 + " +/- " + (critical_value * repParcheggio.getStandardDeviation(2) / (Math.sqrt(K - 1))) / 60);

        repParcheggio.setStandardDeviation(repParcheggio.getAvgPopulationInNode(), 1);
        System.out.println("Critical endpoints E[N_S] =  " + repParcheggio.getMeanPopulationInNode() + " +/- " + critical_value * repParcheggio.getStandardDeviation(1) / (Math.sqrt(K - 1)));

        repParcheggio.setStandardDeviation(repParcheggio.getUtilization(), 3);
        System.out.println("Critical endpoints rho =  " + repParcheggio.getMeanUtilization() + " +/- " + critical_value * repParcheggio.getStandardDeviation(3) / (Math.sqrt(K - 1)));
    }

    @Override
    public void printFinalStatsStazionario() {
        double critical_value = rvms.idfStudent(K - 1, 1 - ALPHA/2);

        System.out.println("\n\nParcheggio\n");

        batchParcheggio.setStandardDeviation(batchParcheggio.getResponseTime(), 2);
        System.out.println("Critical endpoints E[T_S] =  " + batchParcheggio.getMeanResponseTime() / 60 + " +/- " + (critical_value * batchParcheggio.getStandardDeviation(2) / (Math.sqrt(K - 1))) / 60);

        batchParcheggio.setStandardDeviation(batchParcheggio.getAvgPopulationInNode(), 1);
        System.out.println("Critical endpoints E[N_S] =  " + batchParcheggio.getMeanPopulationInNode() + " +/- " + critical_value * batchParcheggio.getStandardDeviation(1) / (Math.sqrt(K - 1)));

        batchParcheggio.setStandardDeviation(batchParcheggio.getUtilization(), 3);
        System.out.println("Critical endpoints rho =  " + batchParcheggio.getMeanUtilization() + " +/- " + critical_value * batchParcheggio.getStandardDeviation(3) / (Math.sqrt(K - 1)));
    }
}