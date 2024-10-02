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

    private long seed = 0L;

    private final MsqT msqT = new MsqT();

    private final List<MsqEvent> serverList = new ArrayList<>(2 + RICARICA_SERVER);
    private final List<MsqSum> sumList = new ArrayList<>(2 + RICARICA_SERVER);

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
        for (s = 0; s < 2 + RICARICA_SERVER; s++) {
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

        // Exit condition : There are no external or internal arrivals, and I haven't processing job.
        if (eventList.getFirst().getX() == 0 && eventList.get(1).getX() == 0 && this.number == 0) return;

        if ((e = MsqEvent.getNextEvent(eventList)) == -1) return;
        msqT.setNext(eventList.get(e).getT());
        area += (msqT.getNext() - msqT.getCurrent()) * number;
        msqT.setCurrent(msqT.getNext());

        // Whether it is an external or internal arrival: (e > 2, is a server processing)
        if (e < 2) {
            this.number++;

            if (e == 0) eventList.getFirst().setT(msqT.getCurrent() + distr.getArrival(2));     /* Get new arrival from exogenous (external) arrival */

            if (e == 1) eventList.get(1).setX(0);

            s = MsqEvent.findOne(eventList);    /* Search for an idle Server */
            if (s != -1) {                      /* Found an idle server*/
                service = distr.getService(2);

                /* Set server as active */
                eventList.get(s).setT(msqT.getCurrent() +  service);  /* Let's calculate the end of service time */
                eventList.get(s).setX(1);

                sumList.get(s).incrementService(service);
                sumList.get(s).incrementServed();
            } else {/* All servers are busy */
                if (this.number - MsqEvent.findActiveServers(eventList) > RICARICA_MAX_QUEUE) {    /* If the queue is full */
                    this.number--;

                    rentalProfit.incrementPenalty();
                }
            }
        } else {    /* Processing a job */
            this.index++;
            this.number--;

            eventList.get(e).setX(2);       /* Current server is no more usable (e = 2 car is ready to be rented) */
        }

        /* Get next Parcheggio's event*/
        int nextEvent = MsqEvent.getNextEvent(eventList);
        if (nextEvent == -1)
            eventListManager.getSystemEventsList().get(1).setX(0);

        eventListManager.getSystemEventsList().get(1).setT(eventList.get(nextEvent).getT());
    }

    /* Infinite horizon simulation */
    @Override
    public void infiniteSimulation () {
        List<MsqEvent> eventList = eventListManager.getServerRicarica();

        // Exit condition : There are no external or internal arrivals, and I haven't processing job.
        if (eventList.getFirst().getX() == 0 && eventList.get(1).getX() == 0 && this.number == 0) return;

        if ((e = MsqEvent.getNextEvent(eventList)) == -1) return;
        msqT.setNext(eventList.get(e).getT());
        area += (msqT.getNext() - msqT.getCurrent()) * number;
        msqT.setCurrent(msqT.getNext());

        // Whether it is an external or internal arrival: (e > 2, is a server processing)
        if (e < 2) {
            this.number++;

            if (e == 0) eventList.getFirst().setT(msqT.getCurrent() + distr.getArrival(2));     /* Get new arrival from exogenous (external) arrival */

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
            if (s != -1) {                      /* Found an idle server*/
                service = distr.getService(2);

                /* Set server as active */
                eventList.get(s).setT(msqT.getCurrent() +  service);  /* Let's calculate the end of service time */
                eventList.get(s).setX(1);

                sumList.get(s).incrementService(service);
                sumList.get(s).incrementServed();
            } else {/* All servers are busy */
                if (this.number - MsqEvent.findActiveServers(eventList) > RICARICA_MAX_QUEUE) {    /* If the queue is full */
                    this.number--;

                    rentalProfit.incrementPenalty();
                }
            }
        } else {    /* Processing a job */
            this.index++;
            this.number--;

            eventList.get(e).setX(2);       /* Current server is no more usable (e = 2 car is ready to be rented) */
        }

        /* Get next Parcheggio's event*/
        int nextEvent = MsqEvent.getNextEvent(eventList);
        if (nextEvent == -1)
            eventListManager.getSystemEventsList().get(1).setX(0);

        eventListManager.getSystemEventsList().get(1).setT(eventList.get(nextEvent).getT());
    }

    @Override
    public void calculateBatchStatistics() {
        double avgPopulationInNode = area / batchDuration;
        double responseTime = area / index;

        batchRicarica.insertAvgPopulationInNode(avgPopulationInNode, nBatch);
        batchRicarica.insertResponseTime(responseTime, nBatch);

        System.out.println("\n\nRicarica batch statistics\n");
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
    public void setSeed(long seed) {
        this.seed = seed;
    }

    @Override
    public void printIteration(boolean isFinite, long seed, int event, int runNumber, double time) {
        double responseTime = area / index;
        double avgPopulationInNode = area / msqT.getCurrent();

        double area = this.area;
        for(int i = 1; i == RICARICA_SERVER; i++) {
            area -= sumList.get(i).getService();
        }

        double waitingTime = area / index;
        double avgPopulationInQueue = area / msqT.getCurrent();

        FileCSVGenerator.writeRepData(isFinite, seed, event, runNumber, time, responseTime, avgPopulationInNode, waitingTime, avgPopulationInQueue);
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
        System.out.println("Critical endpoints E[T_Q] =  " + batchRicarica.getMeanWaitingTimeInQueue() / 60 + " +/- " + critical_value * batchRicarica.getStandardDeviation(4) / (Math.sqrt(K - 1)));

        batchRicarica.setStandardDeviation(batchRicarica.getAvgPopulationInQueue(), 0);
        System.out.println("Critical endpoints E[N_Q] =  " + batchRicarica.getMeanPopulationInQueue() + " +/- " + critical_value * batchRicarica.getStandardDeviation(0) / (Math.sqrt(K - 1)));

        batchRicarica.setStandardDeviation(batchRicarica.getResponseTime(), 2);
        System.out.println("Critical endpoints E[T_S] =  " + batchRicarica.getMeanResponseTime() / 60 + " +/- " + critical_value * batchRicarica.getStandardDeviation(2) / (Math.sqrt(K - 1)));

        batchRicarica.setStandardDeviation(batchRicarica.getAvgPopulationInNode(), 1);
        System.out.println("Critical endpoints E[N_S] =  " + batchRicarica.getMeanPopulationInNode() + " +/- " + critical_value * batchRicarica.getStandardDeviation(1) / (Math.sqrt(K - 1)));

        batchRicarica.setStandardDeviation(batchRicarica.getUtilization(), 3);
        System.out.println("Critical endpoints rho =  " + batchRicarica.getMeanUtilization() + " +/- " + critical_value * batchRicarica.getStandardDeviation(3) / (Math.sqrt(K - 1)));
    }
}
