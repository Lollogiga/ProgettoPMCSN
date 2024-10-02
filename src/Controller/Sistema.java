package Controller;

import Libs.Rngs;
import Model.MsqEvent;
import Model.MsqSum;
import Model.MsqT;
import Utils.Distribution;
import Utils.FileCSVGenerator;
import Utils.RentalProfit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static Utils.Constants.*;

public class Sistema {
    long number = 0;                     /* number of jobs in the node         */
    int e;                               /* next event index                   */
    int s;                               /* server index                       */
    long index = 0;                      /* used to count processed jobs       */
    double area = 0.0;           /* time integrated number in the node */

    double service;

    private final MsqT msqT = new MsqT();

    private final EventListManager eventListManager;
    private final Distribution distr;
    private final RentalProfit rentalProfit;

    private final List<MsqEvent> systemList = new ArrayList<>(NODES);
    private final List<MsqSum> sumList = new ArrayList<>(NODES + 1);

    public static final List<Center> centerList = new ArrayList<>();

    public Sistema(Rngs rngs) {
        centerList.clear();

        eventListManager = EventListManager.getInstance();
        distr = Distribution.getInstance(rngs);
        rentalProfit = RentalProfit.getInstance();

        eventListManager.resetState();
        rentalProfit.resetPenalty();
        rentalProfit.resetExternalCars();

        Parcheggio parcheggio = new Parcheggio();
        Noleggio noleggio = new Noleggio();
        Strada strada = new Strada();
        Ricarica ricarica = new Ricarica();

        centerList.addAll(Arrays.asList(noleggio, ricarica, parcheggio, strada));

        /* 0 - noleggio, 1 - ricarica, 2 - parcheggio, 3 - strada */
        for (int i = 0; i < 4; i++) {
            systemList.add(i, new MsqEvent(0, 0));
            sumList.add(i, new MsqSum(0, 0));
        }

        // Initialize noleggio in system list
        List<MsqEvent> noleggioList = eventListManager.getServerNoleggio();
        int nextEventNoleggio = MsqEvent.getNextEvent(noleggioList);
        systemList.set(0, new MsqEvent(noleggioList.get(nextEventNoleggio).getT(), 1));

        // Initialize ricarica in system list
        List<MsqEvent> chargingList = eventListManager.getServerRicarica();
        int nextEventRicarica = MsqEvent.getNextEvent(chargingList);
        systemList.set(1, new MsqEvent(chargingList.get(nextEventRicarica).getT(), 1));

        // Initialize parcheggio in system list
        List<MsqEvent> parcheggioList = eventListManager.getServerParcheggio();
        int nextEventParcheggio = MsqEvent.getNextEvent(parcheggioList);
        systemList.set(2, new MsqEvent(parcheggioList.get(nextEventParcheggio).getT(), 1));

        // Initialize cars in Parcheggio
        for (int i = 0; i < INIT_PARK_CARS; i++) parcheggioList.get(i + 2).setX(2);

        eventListManager.setSystemEventsList(systemList);
    }

    public void simulation(int simulationType, long seed, int runNumber) throws Exception {
        System.out.println("Starting simulation - seed: " + seed);

        switch (simulationType) {
            case 0:
                simpleSimulation(seed, runNumber);
                break;
            case 1:
                infiniteSimulation(seed);
                break;
            default:
                throw new IllegalArgumentException("Invalid simulation choice");
        }
    }

    /* Finite horizon simulation */
    public void simpleSimulation(long seed, int runNumber) throws Exception {
        int e;
        List<MsqEvent> eventList = eventListManager.getSystemEventsList();

        while (msqT.getCurrent() < STOP_FIN) {
            if ((e = getNextEvent(eventList)) == -1) break;

            msqT.setNext(eventList.get(e).getT());
            this.area = this.area + (msqT.getNext() - msqT.getCurrent()) * number;
            msqT.setCurrent(msqT.getNext());

            if (e < 4) {
                centerList.get(e).setSeed(seed);

                // Generate CSV file to calculate Stada's lambda
                if (e == 3 && MsqEvent.getNextEvent(eventListManager.getServerStrada()) == 0) {
                    FileCSVGenerator.writeStradaArrival(true, seed, 3, msqT.getCurrent());
                }

                centerList.get(e).simpleSimulation();

                centerList.get(e).printIteration(true, seed, e, runNumber, msqT.getCurrent());

                eventList = eventListManager.getSystemEventsList();
            } else throw new Exception("Invalid event");
        }

        System.out.println("\n\n");

        for (int i = 0; i < NODES; i++) centerList.get(i).printResult(runNumber, seed);

        /* Calculate profit */
        printProfit(msqT.getCurrent());
    }

    /* Infinite horizon simulation */
    public void infiniteSimulation(long seed) throws Exception {
        int e;

        msqT.setCurrent(START);
        msqT.setNext(START);

        List<MsqEvent> eventList = eventListManager.getSystemEventsList();

        while (pendingEvents()) {
            if ((e = getNextEvent(eventList)) == -1) break;

            msqT.setNext(eventList.get(e).getT());
            this.area = this.area + (msqT.getNext() - msqT.getCurrent()) * number;
            msqT.setCurrent(msqT.getNext());

            if (e < 4) {
                centerList.get(e).setSeed(seed);

                centerList.get(e).infiniteSimulation();
                eventList = eventListManager.getSystemEventsList();
            } else throw new Exception("Invalid event");
        }

        for (int i = 0; i < NODES; i++) centerList.get(i).printFinalStatsStazionario();

        /* Calculate profit */
        printProfit(msqT.getCurrent());
    }

    private void printProfit(double lastEventTime) {
        System.out.println("\n\nProfit Balance:\n");
        double income = rentalProfit.getProfit();
        double cost = rentalProfit.getCost(lastEventTime);
        System.out.println("  Income .. = " + income);
        System.out.println("  Cost .... = " + cost);
        System.out.println(" -----------------------------------");
        System.out.println("  Profit .... = " + (income - cost));
    }

    /* Fetch index of most imminent event among all servers */
    private int getNextEvent(List<MsqEvent> eventList) {
        double threshold = Double.MAX_VALUE;
        int e = -1;
        int i = 0;

        for (MsqEvent event : eventList) {
            if (event.getT() < threshold && event.getX() == 1) {
                threshold = event.getT();
                e = i;
            }
            i++;
        }
        return e;
    }

    /* Check if there is a centre that has not processed B*K events */
    private boolean pendingEvents() {
        for (Center center : centerList) {
            if (center.getJobInBatch() <= B * K) {
                return true;
            }
        }

        return false;
    }
}
