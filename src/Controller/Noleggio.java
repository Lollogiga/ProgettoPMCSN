package Controller;

import Model.MsqEvent;
import Model.MsqSum;
import Model.MsqT;
import Utils.BatchMeans;
import Utils.Distribution;
import Utils.SimulationResults;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static Model.Constants.*;

public class Noleggio implements Center {
    private final EventListManager eventListManager;

    long nUsers = 0;                /* number in the node                 */
    int e;                          /* next event index                   */
    int s;                          /* server index                       */
    long index = 0;                 /* used to count processed jobs       */
    double service;
    double area = 0.0;              /* time integrated number in the node */
    boolean first_try = true;

    int nBatch = 0;
    int jobInBatch = 0;
    double batchDuration = 0L;

    private final List<MsqEvent> eventList = new ArrayList<>(NOLEGGIO_SERVER + 1);
    private final List<MsqSum> sumList = new ArrayList<>(NOLEGGIO_SERVER + 1);
    private final MsqT msqT = new MsqT();

    private final SimulationResults batchNoleggio = new SimulationResults();
    private final Distribution distr = Distribution.getInstance();

    public Noleggio() {
        this.eventListManager = EventListManager.getInstance();

        for (s = 0; s < NOLEGGIO_SERVER + 1; s++) {
            this.eventList.add(s, new MsqEvent(0, 0));
            this.sumList.add(s, new MsqSum());
        }

        // First arrival event (Passenger)
        double arrival = distr.getArrival(0);

        // Add this new event and setting time to arrival time
        eventList.set(0, new MsqEvent(arrival, 1));

        // Setting event list in eventListManager
        eventListManager.setServerNoleggio(eventList);
    }

    /* Finite horizon simulation */
    @Override
    public void simpleSimulation() {
        int e;
        MsqEvent event;

        List<MsqEvent> eventList = eventListManager.getServerNoleggio();
        List<MsqEvent> internalEventList = eventListManager.getIntQueueNoleggio();

        /* No event to process */
        if (eventList.getFirst().getX() == 0 && internalEventList.isEmpty() && this.nUsers == 0) return;

        if ((e = MsqEvent.getNextEvent(eventList, NOLEGGIO_SERVER)) >= eventList.size()) return;

        if (e == 0 && eventList.get(e).getT() > eventListManager.getSystemEventsList().getFirst().getT()) {
            if (this.nUsers == 0) {
                eventListManager.getSystemEventsList().getFirst().setT(eventList.get(e).getT());

                return;
            }

            msqT.setNext(eventListManager.getSystemEventsList().getFirst().getT());
            area += (msqT.getNext() - msqT.getCurrent()) * nUsers;
        } else {
            msqT.setNext(eventList.get(e).getT());
            if (e == 0) area += (msqT.getNext() - msqT.getCurrent()) * nUsers;
        }
        msqT.setCurrent(msqT.getNext());

        if (e == 0 && !internalEventList.isEmpty()) { /* External arrival (λ) and a car is ready to be rented */
            // Controllo che il prossimo evento sia un evento di arrivo e che l'evento nel sistema < evento più prossimo trovato -> mi è arrivata una macchina mentre non stavo servendo nessuno ed ho utenti pendenti
            if (eventList.getFirst().getT() <= eventListManager.getSystemEventsList().getFirst().getT()) {
                this.nUsers++;

                eventList.getFirst().setT(msqT.getCurrent() + distr.getArrival(0)); /* Get new arrival from passenger arrival */
            }

            if (eventList.getFirst().getT() > STOP_FIN) {
                eventList.getFirst().setX(0);
                eventListManager.setServerNoleggio(eventList);
            }

//            if (number <= NOLEGGIO_SERVER) {
            if (eventList.get(1).getX() == 0) { // TODO: condizione da verificare
                /* Update number of available cars in the center depending on where the car comes from */
                if (internalEventList.getFirst().isFromParking()) {
                    if (eventListManager.reduceCarsInParcheggio() != 0) return;

                    if (eventListManager.getCarsInParcheggio() == PARCHEGGIO_SERVER - 1)
                        eventListManager.getSystemEventsList().get(2).setT(msqT.getCurrent() + 1);
                } else {
                    if (eventListManager.reduceCarsInRicarica() != 0) return;

                    if (eventListManager.getCarsInRicarica() == RICARICA_SERVER - 1)
                        eventListManager.getSystemEventsList().get(1).setT(msqT.getCurrent() + 1);
                }

                /* Set server as active */
                eventList.get(1).setT(msqT.getCurrent());
                eventList.get(1).setX(1);
                internalEventList.removeFirst();

                sumList.get(1).incrementServed();
            }
        } else if (e == 0) {                /* External arrival (λ) but there aren't cars to rent */
            this.nUsers++;

            eventList.getFirst().setT(msqT.getCurrent() + distr.getArrival(0)); /* Get new arrival from passenger arrival */

            if ((e = MsqEvent.getNextEvent(eventList, NOLEGGIO_SERVER)) != 0) {
                eventListManager.getSystemEventsList().getFirst().setX(0);

                return;
            }

            eventListManager.getSystemEventsList().getFirst().setT(eventList.get(e).getT());

            return;
        } else {    /* Process a departure */
            this.index++;
            this.nUsers--;

            /* Virtual move of job from Noleggio to Strada */
            event = new MsqEvent(eventList.get(e).getT(), eventList.get(e).getX());

            /* Routing */
            List<MsqEvent> serverStrada = eventListManager.getServerStrada();
            serverStrada.getFirst().setT(event.getT());
            serverStrada.getFirst().setX(event.getX());
            eventListManager.setServerStrada(serverStrada);

            s = e;
            if (nUsers >= NOLEGGIO_SERVER && !internalEventList.isEmpty()) {        /* there is some jobs in queue, place another job in this server */
                service = distr.getService(0);

                /* Update number of available cars in the center depending on where the car comes from */
                if (internalEventList.getFirst().isFromParking()) {
                    if (eventListManager.reduceCarsInParcheggio() != 0) {
                        this.nUsers++;
                        this.index--;

                        eventList.get(2).setT(msqT.getCurrent());

                        return;
                    }
                } else {
                    if (eventListManager.reduceCarsInRicarica() != 0) {
                        this.nUsers++;
                        this.index--;

                        eventList.get(1).setT(msqT.getCurrent());

                        return;
                    }
                }

                eventList.get(s).setT(msqT.getCurrent() + service);
                internalEventList.removeFirst();

                sumList.get(s).incrementService(service);
                sumList.get(s).incrementServed();
            } else                                    /* no job in queue, simply remove it from server */
                eventList.get(s).setX(0);

            /* Update centralized event list */
            List<MsqEvent> systemList = eventListManager.getSystemEventsList();
            systemList.get(3).setX(1);
            systemList.get(3).setT(event.getT());
        }

        eventListManager.setServerNoleggio(eventList);
        eventListManager.setIntQueueNoleggio(internalEventList);

        int nextEvent = MsqEvent.getNextEvent(eventList, NOLEGGIO_SERVER);
        if (nextEvent == -1) {
            eventListManager.getSystemEventsList().getFirst().setX(0);
            return;
        }

        eventListManager.getSystemEventsList().getFirst().setT(eventList.get(nextEvent).getT());
    }

    /* Infinite horizon simulation */
    @Override
    public void infiniteSimulation() {
        int e;
        MsqEvent event;

        List<MsqEvent> eventList = eventListManager.getServerNoleggio();
        List<MsqEvent> internalEventList = eventListManager.getIntQueueNoleggio();

        /* No event to process */
        if (eventList.getFirst().getX() == 0 && internalEventList.isEmpty() && this.nUsers == 0) return;

        if ((e = MsqEvent.getNextEvent(eventList, NOLEGGIO_SERVER)) >= eventList.size()) return;
        msqT.setNext(eventList.get(e).getT());
        if (e == 0) area += (msqT.getNext() - msqT.getCurrent()) * nUsers;
        msqT.setCurrent(msqT.getNext());

        if (e == 0 && !internalEventList.isEmpty()) { /* External arrival (λ) and a car is ready to be rented */
            this.nUsers++;

            BatchMeans.incrementJobInBatch();
            jobInBatch++;

            eventList.get(e).setT(msqT.getCurrent() + distr.getArrival(0)); /* Get new arrival from passenger arrival */

            if (eventList.getFirst().getT() > STOP_INF) {
                eventList.getFirst().setX(0);
                eventListManager.setServerNoleggio(eventList); // TODO superfluo? Fatto alla fine
            }

            if (nUsers <= NOLEGGIO_SERVER) {
                service = 0;
                s = 1;  /* There is only one server */

                /* Update number of available cars in the center depending on where the car comes from */
                if (internalEventList.getFirst().isFromParking()) {
                    if (eventListManager.reduceCarsInParcheggio() != 0) return;
                } else {
                    if (eventListManager.reduceCarsInRicarica() != 0) return;
                }

                /* Set server as active */
                eventList.get(s).setT(msqT.getCurrent() + service);
                eventList.get(s).setX(1);
                internalEventList.removeFirst();

                sumList.get(s).incrementService(service);
                sumList.get(s).incrementServed();
            }
        } else if (e == 0) {                /* External arrival (λ) but there aren't cars to rent */
            this.nUsers++;

            eventList.getFirst().setT(msqT.getCurrent() + distr.getArrival(0)); /* Get new arrival from passenger arrival */

            List<MsqEvent> eventListParcheggio = eventListManager.getServerParcheggio();
            int nextEventParcheggio = MsqEvent.getNextEvent(eventListParcheggio, NOLEGGIO_SERVER);

            List<MsqEvent> eventListRicarica = eventListManager.getServerRicarica();
            int nextEventRicarica = MsqEvent.getNextEvent(eventListRicarica, RICARICA_SERVER);

            double nextT = Math.min(eventListParcheggio.get(nextEventParcheggio).getT(), eventListRicarica.get(nextEventRicarica).getT());

            eventListManager.getSystemEventsList().getFirst().setT(Math.min(nextT + 1, eventList.getFirst().getT()));

            return;
        } else {    /* Process a departure */
            this.index++;
            this.nUsers--;

            /* Virtual move of job from Noleggio to Strada */
            event = new MsqEvent(eventList.get(e).getT(), eventList.get(e).getX());

            List<MsqEvent> serverStrada = eventListManager.getServerStrada();
            serverStrada.getFirst().setT(event.getT());
            serverStrada.getFirst().setX(event.getX());
            eventListManager.setServerStrada(serverStrada);

            if (jobInBatch % B == 0 && jobInBatch <= B * K) {
                batchDuration = msqT.getCurrent() - msqT.getBatchTimer();

                calculateBatchStatistics();
                nBatch++;
                msqT.setBatchTimer(msqT.getCurrent());
            }

            s = e;
            if (nUsers >= NOLEGGIO_SERVER && !internalEventList.isEmpty()) {        /* there is some jobs in queue, place another job in this server */
                service = distr.getService(0);

                /* Update number of available cars in the center depending on where the car comes from */
                if (internalEventList.getFirst().isFromParking()) {
                    if (eventListManager.reduceCarsInParcheggio() != 0) {
                        this.nUsers++;
                        this.index--;
                        return;
                    }
                } else {
                    if (eventListManager.reduceCarsInRicarica() != 0) {
                        this.nUsers++;
                        this.index--;
                        return;
                    }
                }

                eventList.get(s).setT(msqT.getCurrent() + service);
                internalEventList.removeFirst();

                sumList.get(s).incrementService(service);
                sumList.get(s).incrementServed();
            } else                                    /* no job in queue, simply remove it from server */
                eventList.get(s).setX(0);

            /* Update centralized event list */
            List<MsqEvent> systemList = eventListManager.getSystemEventsList();
            systemList.get(3).setX(1);
            systemList.get(3).setT(event.getT());
        }

        eventListManager.setServerNoleggio(eventList);
        eventListManager.setIntQueueNoleggio(internalEventList);

        int nextEvent = MsqEvent.getNextEvent(eventList, NOLEGGIO_SERVER);
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
        for (int i = 1; i == NOLEGGIO_SERVER; i++) {
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
    public void printResult() {
        DecimalFormat f = new DecimalFormat("#0.00000000");

        double responseTime = area / index;
        double avgPopulationInNode = area / msqT.getCurrent();

        System.out.println("Noleggio\n\n");
        System.out.println("for " + index + " jobs the service node statistics are:\n\n");
        System.out.println("  avg interarrivals .. = " + eventListManager.getSystemEventsList().getFirst().getT() / index);
        System.out.println("  avg wait ........... = " + responseTime);
        System.out.println("  avg # in node ...... = " + avgPopulationInNode);

        for (int i = 1; i == NOLEGGIO_SERVER; i++) {
            area -= sumList.get(i).getService();
        }

        double waitingTimeInQueue = area / index;
        double avgPopulationInQueue = area / msqT.getCurrent();

        System.out.println("  avg delay .......... = " + waitingTimeInQueue);
        System.out.println("  avg # in queue ..... = " + avgPopulationInQueue);
        System.out.println("\tserver\tutilization\t avg service\t share\n");
        for (int i = 1; i == NOLEGGIO_SERVER; i++) {
            System.out.println("\t" + i + "\t\t" + f.format(sumList.get(i).getService() / msqT.getCurrent()) + "\t" + f.format(sumList.get(i).getService() / sumList.get(i).getServed()) + "\t" + f.format(((double) sumList.get(i).getServed() / index)));
        }
        System.out.println("\n");
    }
}
