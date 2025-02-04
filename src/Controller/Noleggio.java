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

public class Noleggio implements Center {
    private final EventListManager eventListManager;
    private RentalProfit rentalProfit;

    long number = 0;                /* number in the node: Somma dei job in coda + in servizio                 */
    int e;                          /* next event index                   */
    int s;                          /* server index                       */
    long index = 0;                 /* used to count processed jobs: Tutti i job processati       */
    double service;
    double area = 0.0;              /* time integrated number in the node */

    int nBatch = 0;
    int jobInBatch = 0;
    double batchDuration = 0L;

    private long seed = 0L;

    private final MsqT msqT = new MsqT();

    // λ_ext, λ_int
    private List<MsqEvent> serverList = new ArrayList<>(2);
    private final List<MsqSum> sumList = new ArrayList<>(2);

    private final Distribution distr;
    private final Rvms rvms = new Rvms();

    private final SimulationResults batchNoleggio = new SimulationResults();
    private final FileCSVGenerator fileCSVGenerator = FileCSVGenerator.getInstance();

    /* Singleton types
     * 0 -> Strada
     * 1 -> Ricarica
     * 2 -> Parcheggio
     * 3 -> Noleggio */
    private final ReplicationStats repNoleggio = ReplicationStats.getInstance(3);

    public Noleggio() {
        this.eventListManager = EventListManager.getInstance();

        this.distr = Distribution.getInstance();

        for (s = 0; s <  2; s++) {
            this.serverList.add(s, new MsqEvent(0, 0));
            this.sumList.add(s, new MsqSum());
        }

        // First arrival event (Passenger)
        double arrival = distr.getArrival(0);

        // Add this new event and setting time to arrival time
        serverList.set(0, new MsqEvent(arrival, 1));     // Setto l'arrivo esterno (0). Setto a 1 processabile.

        // Setting event list in eventListManager
        eventListManager.setServerNoleggio(serverList);
    }

    /* Finite horizon simulation */
    @Override
    public void simpleSimulation() {
        serverList = eventListManager.getServerNoleggio();
        rentalProfit = RentalProfit.getInstance();

        /* Exit condition : There are no external or internal arrivals, and I haven't processing job. */
        if (serverList.getFirst().getX() == 0 && serverList.size() == 1) return;

        if ((e = MsqEvent.getNextEvent(serverList)) == -1) return;
        msqT.setNext(serverList.get(e).getT());
        area += (msqT.getNext() - msqT.getCurrent()) * number;
        msqT.setCurrent(msqT.getNext());

        if (e == 0 || e == 1) {
            if (e == 0) {       /* Manage external arrival */
                this.number++;

                serverList.getFirst().setT(msqT.getCurrent() + distr.getArrival(0));     /* Get new user arrival */
            } else {
                serverList.get(1).setX(0);
            }

            List<MsqEvent> serverParcheggio = eventListManager.getServerParcheggio();
            int sP;

            List<MsqEvent> serverRicarica = eventListManager.getServerRicarica();
            int sR;

            /* Get avaible car in Parcheggio and ricarica*/
            sP = MsqEvent.findAvailableCar(serverParcheggio);
            sR = MsqEvent.findAvailableCar(serverRicarica);

            if (sP == -1 && sR == -1) {     /* No car available */
                //If there is a user, I would like to serve him. I have to pay a penalty if I can't.
                rentalProfit.incrementPenalty();

                serverList.get(1).setX(1);

                // Set Noleggio's λ* time to Parcheggio's next server completition
                int nextEventToCompleteParcheggio = MsqEvent.findNextServerToComplete(serverParcheggio);
                int nextEventToCompleteRicarica = MsqEvent.findNextServerToComplete(serverRicarica);
                if (nextEventToCompleteParcheggio == -1 && nextEventToCompleteRicarica == -1) { // No server to complete, wait for the next arrival
                    List<MsqEvent> serverStrada = eventListManager.getServerStrada();

                    int nextStrada = MsqEvent.getNextEvent(serverStrada);
                    int nextRicarica = MsqEvent.getNextEvent(serverRicarica);
                    int nextParcheggio = MsqEvent.getNextEvent(serverParcheggio);

                    double timeStrada = Double.MAX_VALUE;
                    double timeRicarica = Double.MAX_VALUE;
                    double timeParcheggio = Double.MAX_VALUE;

                    if (nextStrada != -1)
                        timeStrada = serverStrada.get(nextStrada).getT();

                    if (nextRicarica != -1)
                        timeRicarica = serverRicarica.get(nextRicarica).getT();

                    if (nextParcheggio != -1)
                        timeParcheggio = serverParcheggio.get(nextParcheggio).getT();

                    double minTime = Math.min(timeStrada, timeParcheggio);
                    minTime = Math.min(timeRicarica, minTime);

                    serverList.get(1).setT(minTime + INFINITE_INCREMENT);
                } else if (nextEventToCompleteParcheggio != -1 && nextEventToCompleteRicarica != -1) {
                    if (serverParcheggio.get(nextEventToCompleteParcheggio).getT() < serverRicarica.get(nextEventToCompleteRicarica).getT()) {
                        serverList.get(1).setT(serverParcheggio.get(nextEventToCompleteParcheggio).getT() + INFINITE_INCREMENT);
                    } else {
                        serverList.get(1).setT(serverRicarica.get(nextEventToCompleteRicarica).getT() + INFINITE_INCREMENT);
                    }
                } else if (nextEventToCompleteParcheggio != -1) {
                    serverList.get(1).setT(serverParcheggio.get(nextEventToCompleteParcheggio).getT() + INFINITE_INCREMENT);
                } else {
                    serverList.get(1).setT(serverRicarica.get(nextEventToCompleteRicarica).getT() + INFINITE_INCREMENT);
                }

                int nextEvent = MsqEvent.getNextEvent(serverList);
                if (nextEvent == -1) {
                    eventListManager.getSystemEventsList().getFirst().setX(0);
                    return;
                }

                eventListManager.getSystemEventsList().getFirst().setT(serverList.get(nextEvent).getT());

                return;
            }

            if (sP != -1 && sR != -1) {     /* Both parcheggio and ricarica have cars available:  */
                /* Search for the machine that has been waiting the longest*/
                if (serverParcheggio.get(sP).getT() < serverRicarica.get(sR).getT()) {
                    eventListManager.getServerParcheggio().get(sP).setX(0);             /* Car in parcheggio rented */

                    if (eventListManager.getServerParcheggio().get(sP).getT() != 0)
                        FileCSVGenerator.writeTimeCars(true, seed, "Parcheggio", eventListManager.getServerParcheggio().get(sP).getT(), msqT.getCurrent());
                } else {
                    eventListManager.getServerRicarica().get(sR).setX(0);              /* Car in ricarica rented*/

                    if (eventListManager.getServerParcheggio().get(sP).getT() != 0)
                        FileCSVGenerator.writeTimeCars(true, seed, "Ricarica", eventListManager.getServerParcheggio().get(sP).getT(), msqT.getCurrent());
                }

            } else if (sP != -1) {          /* Available cars only in Parcheggio */
                eventListManager.getServerParcheggio().get(sP).setX(0);

                if (eventListManager.getServerParcheggio().get(sP).getT() != 0)
                    FileCSVGenerator.writeTimeCars(true, seed, "Parcheggio", eventListManager.getServerParcheggio().get(sP).getT(), msqT.getCurrent());
            } else {                        /* Available cars only in Ricarica */
                eventListManager.getServerRicarica().get(sR).setX(0);

                if (eventListManager.getServerParcheggio().get(sR).getT() != 0)
                    FileCSVGenerator.writeTimeCars(true, seed, "Ricarica", eventListManager.getServerParcheggio().get(sR).getT(), msqT.getCurrent());
            }

            service = distr.getService(0);
            s = MsqEvent.findOne(serverList);
            if (s == -1) {      /* Setup new server */
                serverList.add(new MsqEvent(msqT.getCurrent() +  service, 1));
                sumList.add(new MsqSum(service, 1));
            } else {            /* Set existing server as active */
                serverList.get(s).setT(msqT.getCurrent() +  service);
                serverList.get(s).setX(1);

                sumList.get(s).incrementService(service);
                sumList.get(s).incrementServed();
            }
        } else {    /* Process a departure */
            this.index++;
            this.number--;

            if (this.number == 0) serverList.get(1).setX(0);

            serverList.get(e).setX(0);

            /* Routing from Noleggio to Strada */
            List<MsqEvent> serverStrada = eventListManager.getServerStrada();
            serverStrada.getFirst().setT(serverList.get(e).getT());
            serverStrada.getFirst().setX(1);

            /* Update centralized event list */
            List<MsqEvent> systemList = eventListManager.getSystemEventsList();
            systemList.get(3).setT(serverList.get(e).getT());
            systemList.get(3).setX(1);
        }

        int nextEvent = MsqEvent.getNextEvent(serverList);
        if (nextEvent == -1) {
            eventListManager.getSystemEventsList().getFirst().setX(0);
            return;
        }

        eventListManager.getSystemEventsList().getFirst().setT(serverList.get(nextEvent).getT());
    }

    /* Infinite horizon simulation */
    @Override
    public void infiniteSimulation() {
        serverList = eventListManager.getServerNoleggio();
        rentalProfit = RentalProfit.getInstance();

        /* Exit condition : There are no external or internal arrivals, and I haven't processing job. */
        if (serverList.getFirst().getX() == 0 && serverList.size() == 1) return;

        if ((e = MsqEvent.getNextEvent(serverList)) == -1) return;
        msqT.setNext(serverList.get(e).getT());
        area += (msqT.getNext() - msqT.getCurrent()) * number;
        msqT.setCurrent(msqT.getNext());

        if (e == 0 || e == 1) {
            if (e == 0) {       /* Manage external arrival */
                this.number++;

                serverList.getFirst().setT(msqT.getCurrent() + distr.getArrival(0));     /* Get new user arrival */

                BatchMeans.incrementJobInBatch();
                jobInBatch++;

                if (jobInBatch % B == 0 && jobInBatch <= B * K) {
                    batchDuration = msqT.getCurrent() - msqT.getBatchTimer();

                    calculateBatchStatistics();
                    nBatch++;
                    msqT.setBatchTimer(msqT.getCurrent());
                }
            } else {
                serverList.get(1).setX(0);
            }

            List<MsqEvent> serverParcheggio = eventListManager.getServerParcheggio();
            int sP;

            List<MsqEvent> serverRicarica = eventListManager.getServerRicarica();
            int sR;

            /* Get avaible car in Parcheggio and ricarica*/
            sP = MsqEvent.findAvailableCar(serverParcheggio);
            sR = MsqEvent.findAvailableCar(serverRicarica);

            if (sP == -1 && sR == -1) {     /* No car available */
                //If there is a user, I would like to serve him. I have to pay a penalty if I can't.
                rentalProfit.incrementPenalty();

                serverList.get(1).setX(1);

                // Set Noleggio's λ* time to Parcheggio's next server completition
                int nextEventToCompleteParcheggio = MsqEvent.findNextServerToComplete(serverParcheggio);
                int nextEventToCompleteRicarica = MsqEvent.findNextServerToComplete(serverRicarica);
                if (nextEventToCompleteParcheggio == -1 && nextEventToCompleteRicarica == -1) { // No server to complete, wait for the next arrival
                    List<MsqEvent> serverStrada = eventListManager.getServerStrada();

                    int nextStrada = MsqEvent.getNextEvent(serverStrada);
                    int nextRicarica = MsqEvent.getNextEvent(serverRicarica);
                    int nextParcheggio = MsqEvent.getNextEvent(serverParcheggio);

                    double timeStrada = Double.MAX_VALUE;
                    double timeRicarica = Double.MAX_VALUE;
                    double timeParcheggio = Double.MAX_VALUE;

                    if (nextStrada != -1)
                        timeStrada = serverStrada.get(nextStrada).getT();

                    if (nextRicarica != -1)
                        timeRicarica = serverRicarica.get(nextRicarica).getT();

                    if (nextParcheggio != -1)
                        timeParcheggio = serverParcheggio.get(nextParcheggio).getT();

                    double minTime = Math.min(timeStrada, timeParcheggio);
                    minTime = Math.min(timeRicarica, minTime);

                    serverList.get(1).setT(minTime + INFINITE_INCREMENT);
                } else if (nextEventToCompleteParcheggio != -1 && nextEventToCompleteRicarica != -1) {
                    if (serverParcheggio.get(nextEventToCompleteParcheggio).getT() < serverRicarica.get(nextEventToCompleteRicarica).getT()) {
                        serverList.get(1).setT(serverParcheggio.get(nextEventToCompleteParcheggio).getT() + INFINITE_INCREMENT);
                    } else {
                        serverList.get(1).setT(serverRicarica.get(nextEventToCompleteRicarica).getT() + INFINITE_INCREMENT);
                    }
                } else if (nextEventToCompleteParcheggio != -1) {
                    serverList.get(1).setT(serverParcheggio.get(nextEventToCompleteParcheggio).getT() + INFINITE_INCREMENT);
                } else {
                    serverList.get(1).setT(serverRicarica.get(nextEventToCompleteRicarica).getT() + INFINITE_INCREMENT);
                }

                int nextEvent = MsqEvent.getNextEvent(serverList);
                if (nextEvent == -1) {
                    eventListManager.getSystemEventsList().getFirst().setX(0);
                    return;
                }

                eventListManager.getSystemEventsList().getFirst().setT(serverList.get(nextEvent).getT());

                return;
            }

            if (sP != -1 && sR != -1) {     /* Both parcheggio and ricarica have cars available:  */
                if (serverParcheggio.get(sP).getT() < serverRicarica.get(sR).getT()) {   /* Search for the machine that has been waiting the longest*/
                    eventListManager.getServerParcheggio().get(sP).setX(0);             /* Car in parcheggio rented */

                    if (eventListManager.getServerParcheggio().get(sP).getT() != 0)
                        FileCSVGenerator.writeTimeCars(false, seed, "Parcheggio", eventListManager.getServerParcheggio().get(sP).getT(), msqT.getCurrent());
                } else {
                    eventListManager.getServerRicarica().get(sR).setX(0);              /* Car in ricarica rented*/

                    if (eventListManager.getServerParcheggio().get(sP).getT() != 0)
                        FileCSVGenerator.writeTimeCars(false, seed, "Ricarica", eventListManager.getServerParcheggio().get(sP).getT(), msqT.getCurrent());
                }

            } else if (sP != -1) {          /* Available cars only in Parcheggio */
                eventListManager.getServerParcheggio().get(sP).setX(0);

                /*if (eventListManager.getServerParcheggio().get(sP).getT() != 0)
                    FileCSVGenerator.writeTimeCars(false, seed, "Parcheggio", eventListManager.getServerParcheggio().get(sP).getT(), msqT.getCurrent());
            */} else {                        /* Available cars only in Ricarica */
                eventListManager.getServerRicarica().get(sR).setX(0);

                /*if (eventListManager.getServerParcheggio().get(sR).getT() != 0)
                    FileCSVGenerator.writeTimeCars(false, seed, "Ricarica", eventListManager.getServerParcheggio().get(sP).getT(), msqT.getCurrent());
            */}

            service = distr.getService(0);
            s = MsqEvent.findOne(serverList);
            if (s == -1) {      /* Setup new server */
                serverList.add(new MsqEvent(msqT.getCurrent() +  service, 1));
                sumList.add(new MsqSum(service, 1));
            } else {            /* Set existing server as active */
                serverList.get(s).setT(msqT.getCurrent() +  service);
                serverList.get(s).setX(1);

                sumList.get(s).incrementService(service);
                sumList.get(s).incrementServed();
            }
        } else {    /* Process a departure */
            this.index++;
            this.number--;

            if (this.number == 0) serverList.get(1).setX(0);

            serverList.get(e).setX(0);

            /* Routing from Noleggio to Strada */
            List<MsqEvent> serverStrada = eventListManager.getServerStrada();
            serverStrada.getFirst().setT(serverList.get(e).getT());
            serverStrada.getFirst().setX(1);

            /* Update centralized event list */
            List<MsqEvent> systemList = eventListManager.getSystemEventsList();
            systemList.get(3).setT(serverList.get(e).getT());
            systemList.get(3).setX(1);
        }

        int nextEvent = MsqEvent.getNextEvent(serverList);
        if (nextEvent == -1) {
            eventListManager.getSystemEventsList().getFirst().setX(0);
            return;
        }

        eventListManager.getSystemEventsList().getFirst().setT(serverList.get(nextEvent).getT());
    }

    @Override
    public void calculateBatchStatistics() {
        double avgPopulationInNode = area / batchDuration;
        double responseTime = area / index;

        System.out.println("\n\nNoleggio batch statistics\n");
        System.out.println("E[N_S]: " + avgPopulationInNode);
        System.out.println("E[T_S]: " + responseTime / 60);

        batchNoleggio.insertAvgPopulationInNode(avgPopulationInNode, nBatch);
        batchNoleggio.insertResponseTime(responseTime, nBatch);

        double sum = 0;
        for(int i = 2; i < eventListManager.getServerNoleggio().size(); i++) {
            sum += sumList.get(i).getService();
            sumList.get(i).setService(0);
            sumList.get(i).setServed(0);
        }

        double utilization = sum / (batchDuration * (eventListManager.getServerNoleggio().size() - 2));
        double waitingTime = (area - sum) / index;
        double populationInQueue = (area - sum) / batchDuration;

        batchNoleggio.insertUtilization(utilization, nBatch);
        batchNoleggio.insertWaitingTimeInQueue(waitingTime, nBatch);
        batchNoleggio.insertAvgPopulationInQueue(populationInQueue, nBatch);

        System.out.println("E[N_Q]: " + populationInQueue);
        System.out.println("E[T_Q]: " + waitingTime / 60);
        System.out.println("Utilization: " + utilization);

        fileCSVGenerator.saveBatchResults(nBatch, responseTime, "Noleggio");

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

        /* Only response time graph */
        double waitingTime = 0;
        double avgPopulationInQueue = 0;

        FileCSVGenerator.writeRepData(isFinite, seed, event, runNumber, time, responseTime, avgPopulationInNode, waitingTime, avgPopulationInQueue);
    }

    @Override
    public void printResult(int runNumber, long seed) {
        DecimalFormat f = new DecimalFormat("#0.00000000");

        double responseTime = area / index;
        double avgPopulationInNode = area / msqT.getCurrent();

        System.out.println("\n\nNoleggio\n");
        System.out.println("for " + index + " jobs the service node statistics are:\n\n");
        System.out.println("  avg interarrivals .. = " + (eventListManager.getSystemEventsList().getFirst().getT() / index) / 60);
        System.out.println("  avg wait ........... = " + responseTime / 60);
        System.out.println("  avg # in node ...... = " + avgPopulationInNode);

        for(int i = 2; i < eventListManager.getServerNoleggio().size(); i++) {
            area -= sumList.get(i).getService();
        }

        double meanUtilization = 0.0;

        System.out.println("  avg delay .......... = " + (area / index) / 60);
        System.out.println("  avg # in queue ..... = " + area / msqT.getCurrent());
        System.out.println("\nthe server statistics are:\n");
        System.out.println("\tserver\tutilization\t avg service\t share\n");
        for(int i = 2; i < eventListManager.getServerNoleggio().size(); i++) {
            System.out.println("\t" + i + "\t\t" + f.format(sumList.get(i).getService() / msqT.getCurrent()) + "\t " + f.format(sumList.get(i).getService() / sumList.get(i).getServed()) + "\t " + f.format(((double)sumList.get(i).getServed() / index)));
            meanUtilization += (sumList.get(i).getService() / msqT.getCurrent());
        }
        System.out.println("\n");

        meanUtilization = meanUtilization / (eventListManager.getServerNoleggio().size() - 1);

        double avgPopulationInQueue = area / msqT.getCurrent();
        double waitingTimeInQueue = area / index;

        if (runNumber > 0 && seed > 0)
            fileCSVGenerator.saveRepResults(NOLEGGIO, runNumber, responseTime, avgPopulationInNode, waitingTimeInQueue, avgPopulationInQueue);

        repNoleggio.insertWaitingTimeInQueue(waitingTimeInQueue, runNumber - 1);
        repNoleggio.insertAvgPopulationInQueue(avgPopulationInQueue, runNumber - 1);
        repNoleggio.insertAvgPopulationInNode(avgPopulationInNode, runNumber - 1);
        repNoleggio.insertUtilization(meanUtilization, runNumber - 1);
        repNoleggio.insertResponseTime(responseTime, runNumber - 1);
    }

    @Override
    public void printFinalStatsTransitorio() {
        double critical_value = rvms.idfStudent(REPLICATION - 1, 1 - ALPHA/2);

        System.out.println("\n\nNoleggio\n");

        repNoleggio.setStandardDeviation(repNoleggio.getWaitingTimeInQueue(), 4);
        System.out.println("Critical endpoints E[T_Q] =  " + repNoleggio.getMeanWaitingTimeInQueue() / 60 + " +/- " + (critical_value * repNoleggio.getStandardDeviation(4) / (Math.sqrt(REPLICATION - 1))) / 60);

        repNoleggio.setStandardDeviation(repNoleggio.getAvgPopulationInQueue(), 0);
        System.out.println("Critical endpoints E[N_Q] =  " + repNoleggio.getMeanPopulationInQueue() + " +/- " + (critical_value * repNoleggio.getStandardDeviation(0) / (Math.sqrt(REPLICATION - 1))));

        repNoleggio.setStandardDeviation(repNoleggio.getResponseTime(), 2);
        System.out.println("Critical endpoints E[T_S] =  " + repNoleggio.getMeanResponseTime() / 60 + " +/- " + (critical_value * repNoleggio.getStandardDeviation(2) / (Math.sqrt(REPLICATION - 1))) / 60);

        repNoleggio.setStandardDeviation(repNoleggio.getAvgPopulationInNode(), 1);
        System.out.println("Critical endpoints E[N_S] =  " + repNoleggio.getMeanPopulationInNode() + " +/- " + critical_value * repNoleggio.getStandardDeviation(1) / (Math.sqrt(REPLICATION - 1)));

        repNoleggio.setStandardDeviation(repNoleggio.getUtilization(), 3);
        System.out.println("Critical endpoints rho =  " + repNoleggio.getMeanUtilization() + " +/- " + critical_value * repNoleggio.getStandardDeviation(3) / (Math.sqrt(REPLICATION - 1)));

    }

    @Override
    public void printFinalStatsStazionario() {
        double critical_value = rvms.idfStudent(K - 1, 1 - ALPHA/2);

        System.out.println("\n\nNoleggio\n");

        batchNoleggio.setStandardDeviation(batchNoleggio.getWaitingTimeInQueue(), 4);
        System.out.println("Critical endpoints E[T_Q] =  " + batchNoleggio.getMeanWaitingTimeInQueue() / 60 + " +/- " + (critical_value * batchNoleggio.getStandardDeviation(4) / (Math.sqrt(K - 1))) / 60);

        batchNoleggio.setStandardDeviation(batchNoleggio.getAvgPopulationInQueue(), 0);
        System.out.println("Critical endpoints E[N_Q] =  " + batchNoleggio.getMeanPopulationInQueue() + " +/- " + critical_value * batchNoleggio.getStandardDeviation(0) / (Math.sqrt(K - 1)));

        batchNoleggio.setStandardDeviation(batchNoleggio.getResponseTime(), 2);
        System.out.println("Critical endpoints E[T_S] =  " + batchNoleggio.getMeanResponseTime() / 60 + " +/- " + (critical_value * batchNoleggio.getStandardDeviation(2) / (Math.sqrt(K - 1))) / 60);

        batchNoleggio.setStandardDeviation(batchNoleggio.getAvgPopulationInNode(), 1);
        System.out.println("Critical endpoints E[N_S] =  " + batchNoleggio.getMeanPopulationInNode() + " +/- " + critical_value * batchNoleggio.getStandardDeviation(1) / (Math.sqrt(K - 1)));

        batchNoleggio.setStandardDeviation(batchNoleggio.getUtilization(), 3);
        System.out.println("Critical endpoints rho =  " + batchNoleggio.getMeanUtilization() + " +/- " + critical_value * batchNoleggio.getStandardDeviation(3) / (Math.sqrt(K - 1)));
    }
}
