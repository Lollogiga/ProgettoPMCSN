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

    private final MsqT msqT = new MsqT();

    // λ_ext, λ_int
    private List<MsqEvent> eventList = new ArrayList<>(2);
    private final List<MsqSum> sumList = new ArrayList<>(2);

    private final Distribution distr;
    private final Rvms rvms = new Rvms();

    private final ReplicationStats repNoleggio = new ReplicationStats();
    private final SimulationResults batchNoleggio = new SimulationResults();

    private final FileCSVGenerator fileCSVGenerator = FileCSVGenerator.getInstance();

    public Noleggio() {
        this.eventListManager = EventListManager.getInstance();

        this.distr = Distribution.getInstance();

        for (s = 0; s <  2; s++) {
            this.eventList.add(s, new MsqEvent(0, 0));
            this.sumList.add(s, new MsqSum());
        }

        // First arrival event (Passenger)
        double arrival = distr.getArrival(0);

        // Add this new event and setting time to arrival time
        eventList.set(0, new MsqEvent(arrival, 1));     // Setto l'arrivo esterno (0). Setto a 1 processabile.

        // Setting event list in eventListManager
        eventListManager.setServerNoleggio(eventList);
    }

    /* Finite horizon simulation */
    @Override
    public void simpleSimulation() {
        eventList = eventListManager.getServerNoleggio();
        rentalProfit = RentalProfit.getInstance();

        /* Exit condition : There are no external or internal arrivals, and I haven't processing job. */
        if (eventList.getFirst().getX() == 0 && eventList.size() == 1) return;

        if ((e = MsqEvent.getNextEvent(eventList)) == -1) return;
        msqT.setNext(eventList.get(e).getT());
        area += (msqT.getNext() - msqT.getCurrent()) * number;
        msqT.setCurrent(msqT.getNext());

        if (e == 0 || e == 1) {
            if (e == 0) {       /* Manage external arrival */
                this.number++;

                eventList.getFirst().setT(msqT.getCurrent() + distr.getArrival(0));     /* Get new user arrival */
            } else {
                eventList.get(1).setX(0);
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

                /*
                * Non ho macchine disponibili
                * Il prossimo evento può essere:
                * - un arrivo esterno di un altro utente
                * - una macchina che si libera da parcheggio
                * - una macchina che si libera da ricarica
                *
                * Devo impostare il tempo di noleggio in sistema pari all'evento più prossimo? NO!
                * Mi basta impostare λ* pari a 1
                *
                * Così facendo gestisco tutti e tre i casi!
                * */
                eventList.get(1).setX(1);

                int nextEvent = MsqEvent.getNextEvent(eventList);
                if (nextEvent == -1) {
                    eventListManager.getSystemEventsList().getFirst().setX(0);
                    return;
                }

                eventListManager.getSystemEventsList().getFirst().setT(eventList.get(nextEvent).getT());

                return;
            }

            if (sP != -1 && sR != -1) {     /* Both parcheggio and ricarica have cars available:  */
                if (serverParcheggio.get(sP).getT() < serverRicarica.get(sR).getT())    /* Search for the machine that has been waiting the longest*/
                    eventListManager.getServerParcheggio().get(sP).setX(0);             /* Car in parcheggio rented */
                else eventListManager.getServerRicarica().get(sR).setX(0);              /* Car in ricarica rented*/

            } else if (sP != -1) {          /* Available cars only in Parcheggio */
                eventListManager.getServerParcheggio().get(sP).setX(0);

            } else {                        /* Available cars only in Ricarica */
                eventListManager.getServerRicarica().get(sR).setX(0);
            }

            service = distr.getService(0);
            s = MsqEvent.findOne(eventList);
            if (s == -1) {      /* Setup new server */
                eventList.add(new MsqEvent(msqT.getCurrent() +  service, 1));
                sumList.add(new MsqSum(service, 1));
            } else {            /* Set existing server as active */
                eventList.get(s).setT(msqT.getCurrent() +  service);
                eventList.get(s).setX(1);

                sumList.get(s).incrementService(service);
                sumList.get(s).incrementServed();
            }
        } else {    /* Process a departure */
            this.index++;
            this.number--;

            if (this.number == 0) eventList.get(1).setX(0);

            eventList.get(e).setX(0);

            /* Routing from Noleggio to Strada */
            List<MsqEvent> serverStrada = eventListManager.getServerStrada();
            serverStrada.getFirst().setT(eventList.get(e).getT());
            serverStrada.getFirst().setX(1);

            /* Update centralized event list */
            List<MsqEvent> systemList = eventListManager.getSystemEventsList();
            systemList.get(3).setT(eventList.get(e).getT());
            systemList.get(3).setX(1);
        }

        int nextEvent = MsqEvent.getNextEvent(eventList);
        if (nextEvent == -1) {
            eventListManager.getSystemEventsList().getFirst().setX(0);
            return;
        }

        eventListManager.getSystemEventsList().getFirst().setT(eventList.get(nextEvent).getT());
    }

    /* Infinite horizon simulation */
    @Override
    public void infiniteSimulation() {
        eventList = eventListManager.getServerNoleggio();

        /* Exit condition : There are no external or internal arrivals, and I haven't processing job. */
        if (eventList.getFirst().getX() == 0 && eventList.size() == 1) return;

        if ((e = MsqEvent.getNextEvent(eventList)) == -1) return;
        msqT.setNext(eventList.get(e).getT());
        area += (msqT.getNext() - msqT.getCurrent()) * number;
        msqT.setCurrent(msqT.getNext());

        if (e == 0 || e == 1) {
            if (e == 0) {       /* Manage external arrival */
                this.number++;

                eventList.getFirst().setT(msqT.getCurrent() + distr.getArrival(0));     /* Get new user arrival */

                BatchMeans.incrementJobInBatch();
                jobInBatch++;

                if (jobInBatch % B == 0 && jobInBatch <= B * K) {
                    batchDuration = msqT.getCurrent() - msqT.getBatchTimer();

                    calculateBatchStatistics();
                    nBatch++;
                    msqT.setBatchTimer(msqT.getCurrent());
                }
            } else {
                eventList.get(1).setX(0);
            }

            List<MsqEvent> serverParcheggio = eventListManager.getServerParcheggio();
            int sP;

            List<MsqEvent> serverRicarica = eventListManager.getServerRicarica();
            int sR;

            /* Get avaible car in Parcheggio and ricarica*/
            sP = MsqEvent.findAvailableCar(serverParcheggio);
            sR = MsqEvent.findAvailableCar(serverRicarica);

            if (sP == -1 && sR == -1) {     /* No car available */
                rentalProfit.incrementPenalty();
                /*
                 * Non ho macchine disponibili
                 * Il prossimo evento può essere:
                 * - un arrivo esterno di un altro utente
                 * - una macchina che si libera da parcheggio
                 * - una macchina che si libera da ricarica
                 *
                 * Devo impostare il tempo di noleggio in sistema pari all'evento più prossimo? NO!
                 * Mi basta impostare λ* pari a 1
                 *
                 * Così facendo gestisco tutti e tre i casi!
                 * */
                eventList.get(1).setX(1);

                int nextEvent = MsqEvent.getNextEvent(eventList);
                if (nextEvent == -1) {
                    eventListManager.getSystemEventsList().getFirst().setX(0);
                    return;
                }

                eventListManager.getSystemEventsList().getFirst().setT(eventList.get(nextEvent).getT());

                return;
            }

            if (sP != -1 && sR != -1) {     /* Both parcheggio and ricarica have cars available:  */
                if (serverParcheggio.get(sP).getT() < serverRicarica.get(sR).getT())    /* Search for the machine that has been waiting the longest*/
                    eventListManager.getServerParcheggio().get(sP).setX(0);             /* Car in parcheggio rented */
                else eventListManager.getServerRicarica().get(sR).setX(0);              /* Car in ricarica rented*/

            } else if (sP != -1) {          /* Available cars only in Parcheggio */
                eventListManager.getServerParcheggio().get(sP).setX(0);

            } else {                        /* Available cars only in Ricarica */
                eventListManager.getServerRicarica().get(sR).setX(0);
            }

            service = distr.getService(0);
            s = MsqEvent.findOne(eventList);
            if (s == -1) {      /* Setup new server */
                eventList.add(new MsqEvent(msqT.getCurrent() +  service, 1));
                sumList.add(new MsqSum(service, 1));
            } else {            /* Set existing server as active */
                eventList.get(s).setT(msqT.getCurrent() +  service);
                eventList.get(s).setX(1);

                sumList.get(s).incrementService(service);
                sumList.get(s).incrementServed();
            }
        } else {    /* Process a departure */
            this.index++;
            this.number--;

            if (this.number == 0) eventList.get(1).setX(0);

            eventList.get(e).setX(0);

            /* Routing from Noleggio to Strada */
            List<MsqEvent> serverStrada = eventListManager.getServerStrada();
            serverStrada.getFirst().setT(eventList.get(e).getT());
            serverStrada.getFirst().setX(1);

            /* Update centralized event list */
            List<MsqEvent> systemList = eventListManager.getSystemEventsList();
            systemList.get(3).setT(eventList.get(e).getT());
            systemList.get(3).setX(1);
        }

        int nextEvent = MsqEvent.getNextEvent(eventList);
        if (nextEvent == -1) {
            eventListManager.getSystemEventsList().getFirst().setX(0);
            return;
        }

        eventListManager.getSystemEventsList().getFirst().setT(eventList.get(nextEvent).getT());
    }

    @Override
    public void calculateBatchStatistics() {
        double avgPopulationInNode = area / batchDuration;
        double responseTime = area / index;

        System.out.println("Noleggio batch statistics\n\n");
        System.out.println("E[N_s]: " + avgPopulationInNode);
        System.out.println("E[T_s]: " + responseTime);

        batchNoleggio.insertAvgPopulationInNode(avgPopulationInNode, nBatch);
        batchNoleggio.insertResponseTime(responseTime, nBatch);

        double sum = 0;
        for(int i = 1; i == NOLEGGIO_SERVER; i++) {
            sum += sumList.get(i).getService();
            sumList.get(i).setService(0);
            sumList.get(i).setServed(0);
        }

        double waitingTimeInQueue = (area - sum) / index;
        double avgPopulationInQueue = (area - sum) / batchDuration;
        double utilization = sum / (batchDuration * NOLEGGIO_SERVER);

        batchNoleggio.insertWaitingTimeInQueue(waitingTimeInQueue, nBatch);
        batchNoleggio.insertAvgPopulationInQueue(avgPopulationInQueue, nBatch);
        batchNoleggio.insertUtilization(utilization, nBatch);

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
    public void printIteration(boolean isFinite, long seed, int event, int runNumber, double time) {
        double responseTime = area / index;
        double avgPopulationInNode = area / msqT.getCurrent();

        double area = this.area;
        for(int i = 1; i == NOLEGGIO_SERVER; i++) {
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

        System.out.println("\n\nNoleggio\n");
        System.out.println("for " + index + " jobs the service node statistics are:\n\n");
        System.out.println("  avg interarrivals .. = " + eventListManager.getSystemEventsList().getFirst().getT() / index);
        System.out.println("  avg wait ........... = " + responseTime);
        System.out.println("  avg # in node ...... = " + avgPopulationInNode);

        for(int i = 1; i == NOLEGGIO_SERVER; i++) {
            area -= sumList.get(i).getService();
        }

        double waitingTime = area / index;
        double avgPopulationInQueue = area / msqT.getCurrent();

        System.out.println("  avg delay .......... = " + waitingTime);
        System.out.println("  avg # in queue ..... = " + avgPopulationInQueue);
        System.out.println("\tserver\tutilization\t avg service\t share\n");
        for(int i = 1; i == NOLEGGIO_SERVER; i++) {
            System.out.println("\t" + i + "\t\t" + f.format(sumList.get(i).getService() / msqT.getCurrent()) + "\t" + f.format(sumList.get(i).getService() / sumList.get(i).getServed()) + "\t" + f.format(((double)sumList.get(i).getServed() / index)));
        }
        System.out.println("\n");

        if (waitingTime <= 0.0) waitingTime = 0L;
        if (avgPopulationInQueue <= 0.0) avgPopulationInQueue = 0L;

        if (runNumber > 0 && seed > 0)
            fileCSVGenerator.saveRepResults(NOLEGGIO, runNumber, responseTime, avgPopulationInNode, waitingTime, avgPopulationInQueue);

        repNoleggio.insertAvgPopulationInNode(avgPopulationInNode, runNumber - 1);
        repNoleggio.insertUtilization(0.0, runNumber - 1);
        repNoleggio.insertResponseTime(responseTime, runNumber - 1);
        repNoleggio.insertWaitingTimeInQueue(waitingTime, runNumber - 1);
        repNoleggio.insertWaitingTimeInQueue(avgPopulationInQueue, runNumber - 1);
    }

    @Override
    public void printFinalStatsTransitorio() {
        double critical_value = rvms.idfStudent(K - 1, 1 - ALPHA/2);

        System.out.println("\n\nNoleggio\n");

        repNoleggio.setStandardDeviation(repNoleggio.getWaitingTimeInQueue(), 4);
        System.out.println("Critical endpoints E[T_Q] =  " + repNoleggio.getMeanWaitingTimeInQueue() + " +/- " + critical_value * repNoleggio.getStandardDeviation(4) / (Math.sqrt(K - 1)));

        repNoleggio.setStandardDeviation(repNoleggio.getAvgPopulationInQueue(), 0);
        System.out.println("Critical endpoints E[N_Q] =  " + repNoleggio.getMeanPopulationInQueue() + " +/- " + critical_value * repNoleggio.getStandardDeviation(0) / (Math.sqrt(K - 1)));

        repNoleggio.setStandardDeviation(repNoleggio.getResponseTime(), 2);
        System.out.println("Critical endpoints E[T_S] =  " + repNoleggio.getMeanResponseTime() + " +/- " + critical_value * repNoleggio.getStandardDeviation(2) / (Math.sqrt(K - 1)));

        repNoleggio.setStandardDeviation(repNoleggio.getAvgPopulationInNode(), 1);
        System.out.println("Critical endpoints E[N_S] =  " + repNoleggio.getMeanPopulationInNode() + " +/- " + critical_value * repNoleggio.getStandardDeviation(1) / (Math.sqrt(K - 1)));

        repNoleggio.setStandardDeviation(repNoleggio.getUtilization(), 3);
        System.out.println("Critical endpoints rho =  " + repNoleggio.getMeanUtilization() + " +/- " + critical_value * repNoleggio.getStandardDeviation(3) / (Math.sqrt(K - 1)));

    }

    @Override
    public void printFinalStatsStazionario() {
        double critical_value = rvms.idfStudent(K - 1, 1 - ALPHA/2);

        System.out.println("\n\nNoleggio\n");

        batchNoleggio.setStandardDeviation(batchNoleggio.getWaitingTimeInQueue(), 4);
        System.out.println("Critical endpoints E[T_Q] =  " + batchNoleggio.getMeanWaitingTimeInQueue() + " +/- " + critical_value * batchNoleggio.getStandardDeviation(4) / (Math.sqrt(K - 1)));

        batchNoleggio.setStandardDeviation(batchNoleggio.getAvgPopulationInQueue(), 0);
        System.out.println("Critical endpoints E[N_Q] =  " + batchNoleggio.getMeanPopulationInQueue() + " +/- " + critical_value * batchNoleggio.getStandardDeviation(0) / (Math.sqrt(K - 1)));

        batchNoleggio.setStandardDeviation(batchNoleggio.getResponseTime(), 2);
        System.out.println("Critical endpoints E[T_S] =  " + batchNoleggio.getMeanResponseTime() + " +/- " + critical_value * batchNoleggio.getStandardDeviation(2) / (Math.sqrt(K - 1)));

        batchNoleggio.setStandardDeviation(batchNoleggio.getAvgPopulationInNode(), 1);
        System.out.println("Critical endpoints E[N_S] =  " + batchNoleggio.getMeanPopulationInNode() + " +/- " + critical_value * batchNoleggio.getStandardDeviation(1) / (Math.sqrt(K - 1)));

        batchNoleggio.setStandardDeviation(batchNoleggio.getUtilization(), 3);
        System.out.println("Critical endpoints rho =  " + batchNoleggio.getMeanUtilization() + " +/- " + critical_value * batchNoleggio.getStandardDeviation(3) / (Math.sqrt(K - 1)));
    }
}
