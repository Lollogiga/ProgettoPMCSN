package Controller;

import Libs.Rngs;
import Libs.Rvms;
import Model.MsqEvent;
import Model.MsqSum;
import Model.MsqT;
import Utils.*;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static Utils.Constants.*;

/* Modeled as an Infinite Server */
public class Strada implements Center {
    private final EventListManager eventListManager;
    private final RentalProfit rentalProfit;

    long number = 0;                /* number in the node                 */
    int e;                          /* next event index                   */
    int s;                          /* server index                       */
    long index = 0;                 /* used to count processed jobs       */
    double service;
    double area = 0.0;              /* time integrated number in the node */

    int nBatch = 0;
    int jobInBatch = 0;
    double batchDuration = 0L;

    private final MsqT msqT = new MsqT();

    private final List<MsqEvent> serverList = new ArrayList<>(1);
    private final List<MsqSum> sumList = new ArrayList<>(1);

    private final ReplicationStats repStrada = new ReplicationStats();
    private final SimulationResults batchStrada = new SimulationResults();
    private final Distribution distr;
    private final Rngs rngs;
    private final Rvms rvms = new Rvms();
    private final FileCSVGenerator fileCSVGenerator = FileCSVGenerator.getInstance();

    public Strada() {
        eventListManager = EventListManager.getInstance();

        distr = Distribution.getInstance();

        rentalProfit = RentalProfit.getInstance();

        rngs = distr.getRngs();

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
        if (eventList.getFirst().getX() == 0 && this.number == 0) return;

        if ((e = MsqEvent.getNextEvent(eventList, eventList.size() - 1)) >= eventList.size()) return;
        msqT.setNext(eventList.get(e).getT());
        area += (msqT.getNext() - msqT.getCurrent()) * number;
        msqT.setCurrent(msqT.getNext());

        if (e == 0) {
            this.number++;

            eventList.getFirst().setX(0);

            service = distr.getService(3);
            s = MsqEvent.findOne(eventList);
            if (s == -1) {     /* Setup new server */
                eventList.add(new MsqEvent(msqT.getCurrent() +  service, 1));
                sumList.add(new MsqSum(service, 1));
            } else {        /* Set existing server as active */
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
            double pLoss = rngs.random();
            if (pLoss < P_EXIT) {   /* Job exit from this system */
                eventListManager.decrementCars();

                eventList.get(s).setX(0);           /* Sets the status of the server from which the job started equal to 0 */

                int nextEvent = MsqEvent.getNextEvent(eventList, eventList.size() - 1);
                if (nextEvent == -1) {
                    eventListManager.getSystemEventsList().get(3).setX(0);
                    return;
                }

                eventListManager.getSystemEventsList().get(3).setT(eventList.get(nextEvent).getT());
            } else {                /* Job stays in this system */
                double pRicarica = rngs.random();
                if (pRicarica < P_RICARICA) {
                    // Event sent to Ricarica
                    List<MsqEvent> eventListRicarica = eventListManager.getServerRicarica();
                    eventListRicarica.get(1).setT(eventList.get(s).getT());
                    eventListRicarica.get(1).setX(1);

                    systemList.get(1).setT(eventList.get(s).getT());
                    systemList.get(1).setX(1);
                } else {
                    // Event sent to Parcheggio
                    List<MsqEvent> eventListParchegio = eventListManager.getServerParcheggio();
                    eventListParchegio.get(1).setT(eventList.get(s).getT());
                    eventListParchegio.get(1).setX(1);

                    systemList.get(2).setT(eventList.get(s).getT());
                    systemList.get(2).setX(1);
                }

                eventList.get(s).setX(0);   /* Set server as idle */
            }
        }

        /* Get next Strada's events */
        int nextEvent = MsqEvent.getNextEvent(eventList, eventList.size() - 1);
        if (nextEvent == -1) {
            eventListManager.getSystemEventsList().get(3).setX(0);
            return;
        }

        eventListManager.getSystemEventsList().get(3).setT(eventList.get(nextEvent).getT());
    }

    /* Infinite horizon simulation */
    @Override
    public void infiniteSimulation() {
        List<MsqEvent> eventList = eventListManager.getServerStrada();

        /* no external arrivals, no internal arrivals and no jobs in the server */
        if (eventList.getFirst().getX() == 0 && this.number == 0) return;

        if ((e = MsqEvent.getNextEvent(eventList, eventList.size() - 1)) >= eventList.size()) return;
        msqT.setNext(eventList.get(e).getT());
        area += (msqT.getNext() - msqT.getCurrent()) * number;
        msqT.setCurrent(msqT.getNext());

        if (e == 0) {
            this.number++;

            eventList.getFirst().setX(0);

            BatchMeans.incrementJobInBatch();
            jobInBatch++;

            if (jobInBatch % B == 0 && jobInBatch <= B * K) {
                batchDuration = msqT.getCurrent() - msqT.getBatchTimer();

                calculateBatchStatistics();
                nBatch++;
                msqT.setBatchTimer(msqT.getCurrent());
            }

            service = distr.getService(3);
            s = MsqEvent.findOne(eventList);
            if (s == -1) {     /* Setup new server */
                eventList.add(new MsqEvent(msqT.getCurrent() +  service, 1));
                sumList.add(new MsqSum(service, 1));
            } else {        /* Set existing server as active */
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
            double pLoss = rngs.random();
            if (pLoss < P_EXIT) {   /* Job exit from this system */
                eventListManager.decrementCars();

                eventList.get(s).setX(0);           /* Sets the status of the server from which the job started equal to 0 */

                int nextEvent = MsqEvent.getNextEvent(eventList, eventList.size() - 1);
                if (nextEvent == -1) {
                    eventListManager.getSystemEventsList().get(3).setX(0);
                    return;
                }

                eventListManager.getSystemEventsList().get(3).setT(eventList.get(nextEvent).getT());
            } else {                /* Job stays in this system */
                double pRicarica = rngs.random();
                if (pRicarica < P_RICARICA) {
                    // Event sent to Ricarica
                    List<MsqEvent> eventListRicarica = eventListManager.getServerRicarica();
                    eventListRicarica.get(1).setT(eventList.get(s).getT());
                    eventListRicarica.get(1).setX(1);

                    systemList.get(1).setT(eventList.get(s).getT());
                    systemList.get(1).setX(1);
                } else {
                    // Event sent to Parcheggio
                    List<MsqEvent> eventListParchegio = eventListManager.getServerParcheggio();
                    eventListParchegio.get(1).setT(eventList.get(s).getT());
                    eventListParchegio.get(1).setX(1);

                    systemList.get(2).setT(eventList.get(s).getT());
                    systemList.get(2).setX(1);
                }

                eventList.get(s).setX(0);   /* Set server as idle */
            }
        }

        /* Get next Strada's events */
        int nextEvent = MsqEvent.getNextEvent(eventList, eventList.size() - 1);
        if (nextEvent == -1) {
            eventListManager.getSystemEventsList().get(3).setX(0);
            return;
        }

        eventListManager.getSystemEventsList().get(3).setT(eventList.get(nextEvent).getT());
    }

    @Override
    public void calculateBatchStatistics() {
        double avgPopulationInNode = area / batchDuration;
        double responseTime = area / index;

        batchStrada.insertAvgPopulationInNode(avgPopulationInNode, nBatch);
        batchStrada.insertResponseTime(responseTime, nBatch);

        System.out.println("\n\nStrada batch statistics\n");
        System.out.println("E[N_s]: " + avgPopulationInNode);
        System.out.println("E[T_s]: " + responseTime);

        double sum = 0;
        for(int i = 1; i < eventListManager.getServerStrada().size(); i++) {
            sum += sumList.get(i).getService();
            sumList.get(i).setService(0);
            sumList.get(i).setServed(0);
        }

        double utilization = sum / (batchDuration * eventListManager.getServerStrada().size());

        batchStrada.insertUtilization(utilization, nBatch);
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

        System.out.println("\n\nStrada\n");
        System.out.println("for " + index + " jobs the service node statistics are:\n\n");
        System.out.println("  avg interarrivals .. = " + eventListManager.getSystemEventsList().getFirst().getT() / index);
        System.out.println("  avg wait ........... = " + responseTime);
        System.out.println("  avg # in node ...... = " + avgPopulationInNode);

        double meanUtilization = 0;
        System.out.println("\nthe server statistics are:\n\n");
        System.out.println("\tserver\tutilization\t avg service\t share\n");
        for(int i = 1; i < eventListManager.getServerStrada().size(); i++) {
            System.out.println("\t" + i + "\t\t" + f.format(sumList.get(i).getService() / msqT.getCurrent()) + "\t " + f.format(sumList.get(i).getService() / sumList.get(i).getServed()) + "\t " + f.format(((double)sumList.get(i).getServed() / index)));
            meanUtilization += (sumList.get(i).getService() / msqT.getCurrent());
        }

        meanUtilization = meanUtilization / (eventListManager.getServerStrada().size() - 1);

        /* Calculate rental profit */
        double baseProfit = (responseTime / 3600) * (index + rentalProfit.getExternalCars()) * RENTAL_PROFIT;
        double kmProfit = (MEAN_SPEED * (responseTime / 3600)) * RENTAL_KM_PROFIT * index;
        rentalProfit.setProfit((baseProfit + kmProfit));

        System.out.println("\n");

        if (runNumber > 0 && seed > 0)
            fileCSVGenerator.saveRepResults(STRADA, runNumber, responseTime, avgPopulationInNode, -Double.MAX_VALUE, -Double.MAX_VALUE);

        repStrada.insertAvgPopulationInNode(avgPopulationInNode, runNumber - 1);
        repStrada.insertUtilization(meanUtilization, runNumber - 1);
        repStrada.insertResponseTime(responseTime, runNumber - 1);
        repStrada.insertWaitingTimeInQueue(0.0, runNumber - 1);
        repStrada.insertWaitingTimeInQueue(0.0, runNumber - 1);
    }

    @Override
    public void printFinalStatsTransitorio() {
        double critical_value = rvms.idfStudent(K - 1, 1 - ALPHA/2);

        System.out.println("\n\nStrada\n");

        repStrada.setStandardDeviation(repStrada.getWaitingTimeInQueue(), 4);
        System.out.println("Critical endpoints E[T_Q] =  " + repStrada.getMeanWaitingTimeInQueue() + " +/- " + critical_value * repStrada.getStandardDeviation(4) / (Math.sqrt(K - 1)));

        repStrada.setStandardDeviation(repStrada.getAvgPopulationInQueue(), 0);
        System.out.println("Critical endpoints E[N_Q] =  " + repStrada.getMeanPopulationInQueue() + " +/- " + critical_value * repStrada.getStandardDeviation(0) / (Math.sqrt(K - 1)));

        repStrada.setStandardDeviation(repStrada.getResponseTime(), 2);
        System.out.println("Critical endpoints E[T_S] =  " + repStrada.getMeanResponseTime() + " +/- " + critical_value * repStrada.getStandardDeviation(2) / (Math.sqrt(K - 1)));

        repStrada.setStandardDeviation(repStrada.getAvgPopulationInNode(), 1);
        System.out.println("Critical endpoints E[N_S] =  " + repStrada.getMeanPopulationInNode() + " +/- " + critical_value * repStrada.getStandardDeviation(1) / (Math.sqrt(K - 1)));

        repStrada.setStandardDeviation(repStrada.getUtilization(), 3);
        System.out.println("Critical endpoints rho =  " + repStrada.getMeanUtilization() + " +/- " + critical_value * repStrada.getStandardDeviation(3) / (Math.sqrt(K - 1)));
    }

    @Override
    public void printFinalStatsStazionario() {
        double critical_value = rvms.idfStudent(K - 1, 1 - ALPHA/2);

        System.out.println("\n\nStrada\n");

        batchStrada.setStandardDeviation(batchStrada.getWaitingTimeInQueue(), 4);
        System.out.println("Critical endpoints E[T_Q] =  " + batchStrada.getMeanWaitingTimeInQueue() / 60 + " +/- " + (critical_value * batchStrada.getStandardDeviation(4) / (Math.sqrt(K - 1))) / 60);

        batchStrada.setStandardDeviation(batchStrada.getAvgPopulationInQueue(), 0);
        System.out.println("Critical endpoints E[N_Q] =  " + batchStrada.getMeanPopulationInQueue() + " +/- " + critical_value * batchStrada.getStandardDeviation(0) / (Math.sqrt(K - 1)));

        batchStrada.setStandardDeviation(batchStrada.getResponseTime(), 2);
        System.out.println("Critical endpoints E[T_S] =  " + batchStrada.getMeanResponseTime() / 60 + " +/- " + (critical_value * batchStrada.getStandardDeviation(2) / (Math.sqrt(K - 1))) / 60);

        batchStrada.setStandardDeviation(batchStrada.getAvgPopulationInNode(), 1);
        System.out.println("Critical endpoints E[N_S] =  " + batchStrada.getMeanPopulationInNode() + " +/- " + critical_value * batchStrada.getStandardDeviation(1) / (Math.sqrt(K - 1)));

        batchStrada.setStandardDeviation(batchStrada.getUtilization(), 3);
        System.out.println("Critical endpoints rho =  " + batchStrada.getMeanUtilization() + " +/- " + critical_value * batchStrada.getStandardDeviation(3) / (Math.sqrt(K - 1)));

        double baseProfit = (batchStrada.getMeanResponseTime() / 3600) * (index + rentalProfit.getExternalCars()) * RENTAL_PROFIT;
        double kmProfit = (MEAN_SPEED * (batchStrada.getMeanResponseTime() / 3600)) * RENTAL_KM_PROFIT * index;
        rentalProfit.setProfit((baseProfit + kmProfit));
    }
}
