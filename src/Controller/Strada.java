package Controller;

import Libs.Rngs;
import Model.MsqEvent;
import Model.MsqSum;
import Model.MsqT;
import Utils.*;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static Model.Constants.*;

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

    private final SimulationResults batchStrada = new SimulationResults();
    private final Distribution distr;
    private final Rngs rngs;
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
        if (eventList.getFirst().getX() == 0 && eventList.size() == 1) return;

        if ((e = MsqEvent.getNextEvent(eventList, eventList.size() - 1)) >= eventList.size()) return;
        msqT.setNext(eventList.get(e).getT());
        area += (msqT.getNext() - msqT.getCurrent()) * number;
        msqT.setCurrent(msqT.getNext());

        if (e == 0) {
            this.number++;

            eventList.get(e).setX(0);

            service = distr.getService(3);
            s = MsqEvent.findOne(eventList, eventList.size() - 1);

            if (s == -1 || s >= eventList.size()) {     /* Setup new server */
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
            if (pLoss < P_LOSS) {
                eventListManager.decrementCars();

                //In this case, I must pay a penalty:
                rentalProfit.incrementPenalty();

                // Sets the status of the server from which the job started equal to 0
                eventList.get(s).setX(0);

                int nextEvent = MsqEvent.getNextEvent(eventList, eventList.size() - 1);
                if (nextEvent == -1) {
                    eventListManager.getSystemEventsList().get(3).setX(0);
                    return;
                }

                eventListManager.getSystemEventsList().get(3).setT(eventList.get(nextEvent).getT());
            } else {
                /* Job stays in this system */
                double pRicarica = rngs.random();
                if (pRicarica < P_RICARICA) {
                    // Event sent to Ricarica
                    List<MsqEvent> intQueueRicarica = eventListManager.getIntQueueRicarica();
                    intQueueRicarica.add(eventList.get(s));
                    eventListManager.setIntQueueRicarica(intQueueRicarica);

                    systemList.get(1).setX(1);
                    systemList.get(1).setT(eventList.get(s).getT());
                } else {
                    // Event sent to Parcheggio
                    List<MsqEvent> intQueueParcheggio = eventListManager.getIntQueueParcheggio();
                    intQueueParcheggio.add(eventList.get(s));
                    eventListManager.setIntQueueParcheggio(intQueueParcheggio);

                    systemList.get(2).setX(1);
                    systemList.get(2).setT(eventList.get(s).getT());
                }
                eventList.get(s).setX(0);   /* Set server as idle */
            }
        }

        eventListManager.setServerStrada(eventList);

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
        if (eventList.getFirst().getX() == 0 && eventList.size() == 1) return;

        if ((e = MsqEvent.getNextEvent(eventList, eventList.size() - 1)) >= eventList.size()) return;
        msqT.setNext(eventList.get(e).getT());
        area += (msqT.getNext() - msqT.getCurrent()) * number;
        msqT.setCurrent(msqT.getNext());

        if (e == 0) {
            this.number++;

            BatchMeans.incrementJobInBatch();
            jobInBatch++;


            eventList.get(e).setX(0);

            service = distr.getService(3);
            s = MsqEvent.findOne(eventList, eventList.size() - 1);

            if (s == -1 || s >= eventList.size()) {
                /* Setup new server */
                eventList.add(new MsqEvent(msqT.getCurrent() +  service, 1));
                sumList.add(new MsqSum(service, 1));
            } else {
                /* Set existing server as active */
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

            if (jobInBatch % B == 0 && jobInBatch <= B * K) {
                batchDuration = msqT.getCurrent() - msqT.getBatchTimer();

                calculateBatchStatistics();
                nBatch++;
                msqT.setBatchTimer(msqT.getCurrent());
            }

            /* Routing */
            s = e;
            double pLoss = rngs.random();
            if (pLoss < P_LOSS) {
                eventListManager.decrementCars();

                //In this case, I must pay a penalty:
                //TODO pay penalty

                // Sets the status of the server from which the job started equal to 0
                eventList.get(s).setX(0);

                int nextEvent = MsqEvent.getNextEvent(eventList, eventList.size() - 1);
                if (nextEvent == -1) {
                    eventListManager.getSystemEventsList().get(3).setX(0);
                    return;
                }

                eventListManager.getSystemEventsList().get(3).setT(eventList.get(nextEvent).getT());
            } else {
                /* Job stays in this system */
                double pRicarica = rngs.random();
                if (pRicarica < P_RICARICA) {
                    // Event sent to Ricarica
                    List<MsqEvent> intQueueRicarica = eventListManager.getIntQueueRicarica();
                    intQueueRicarica.add(eventList.get(s));
                    eventListManager.setIntQueueRicarica(intQueueRicarica);

                    systemList.get(1).setX(1);
                    systemList.get(1).setT(eventList.get(s).getT());
                } else {
                    // Event sent to Parcheggio
                    List<MsqEvent> intQueueParcheggio = eventListManager.getIntQueueParcheggio();
                    intQueueParcheggio.add(eventList.get(s));
                    eventListManager.setIntQueueParcheggio(intQueueParcheggio);

                    systemList.get(2).setX(1);
                    systemList.get(2).setT(eventList.get(s).getT());
                }
                eventList.get(s).setX(0);   /* Set server as idle */
            }
        }

        eventListManager.setServerStrada(eventList);

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

        System.out.println("Strada batch statistics\n\n");
        System.out.println("E[N_s]: " + avgPopulationInNode);
        System.out.println("E[T_s]: " + responseTime);

        double sum = 0;
        for(int i = 1; i <= eventListManager.getServerStrada().size(); i++) {
            sum += sumList.get(i).getService();
            sumList.get(i).setService(0);
            sumList.get(i).setServed(0);
        }

        double utilization = sum / (batchDuration * eventListManager.getServerStrada().size());

        batchStrada.insertUtilization(utilization, nBatch);
        System.out.println("Rho: " + utilization);

        /* Reset parameters */
        area = 0;
        index = 0;
    }

    @Override
    public int getNumJob() {
        return this.jobInBatch;
    }

    @Override
    public void printResult(int runNumber, long seed) {
        DecimalFormat f = new DecimalFormat("#0.00000000");

        double responseTime = area / index;
        double avgPopulationInNode = area / msqT.getCurrent();

        System.out.println("Strada\n\n");
        System.out.println("for " + index + " jobs the service node statistics are:\n\n");
        System.out.println("  avg interarrivals .. = " + eventListManager.getSystemEventsList().getFirst().getT() / index);
        System.out.println("  avg wait ........... = " + responseTime);
        System.out.println("  avg # in node ...... = " + avgPopulationInNode);

        System.out.println("\nthe server statistics are:\n\n");
        System.out.println("\tserver\tutilization\t avg service\t share\n");
        for(int i = 1; i < eventListManager.getServerStrada().size(); i++) {
            System.out.println("\t" + i + "\t\t" + f.format(sumList.get(i).getService() / msqT.getCurrent()) + "\t " + f.format(sumList.get(i).getService() / sumList.get(i).getServed()) + "\t " + f.format(((double)sumList.get(i).getServed() / index)));
        }

        /* Calculate rental profit */
        double baseProfit = (responseTime / 3600) * index * RENTAL_PROFIT;
        double kmProfit = (MEAN_SPEED * (responseTime / 3600)) * RENTAL_KM_PROFIT * index;
        rentalProfit.setProfit((baseProfit + kmProfit));

        System.out.println("\n");

        if (runNumber > 0 && seed > 0)
            fileCSVGenerator.saveRepResults(STRADA, runNumber, seed, responseTime, avgPopulationInNode, -Double.MAX_VALUE, -Double.MAX_VALUE);

    }

}
