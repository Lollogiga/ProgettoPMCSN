package Controller;

import Libs.Rngs;
import Model.MsqEvent;
import Model.MsqSum;
import Model.MsqT;
import Util.Distribution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static Model.Constants.*;

public class Sistema {
    // TODO all'inizio della simulazione impostare la lista intQueueNoleggio
    // TODO inizializzare il numero di macchine nel sistema
    // TODO all'inizio della simulazione impostare in eventListManager carsInParcheggio e carsInRicarica

    private final EventListManager eventListManager;

    long number = 0;                     /* number of jobs in the node         */
    int e;                               /* next event index                   */
    int s;                               /* server index                       */
    long index = 0;                      /* used to count processed jobs       */
    double area = 0.0;           /* time integrated number in the node */
    double service;

    private MsqT msqT = new MsqT();

    private final List<MsqEvent> systemList = new ArrayList<>(NODES);
    private final List<MsqSum> sumList = new ArrayList<>(NODES + 1);
    private final Distribution distr;

    private List<Center> controllerList = new ArrayList<>();

    public Sistema() {
        eventListManager = EventListManager.getInstance();
        distr = Distribution.getInstance();

        Rngs rngs = new Rngs();
        rngs.plantSeeds(SEED);

        Parcheggio parcheggio = new Parcheggio();
        Noleggio noleggio = new Noleggio();
        Strada strada = new Strada();
        Ricarica ricarica = new Ricarica();

        controllerList.addAll(Arrays.asList(noleggio, ricarica, parcheggio, strada));

        /* 0 - noleggio, 1 - ricarica, 2 - parcheggio, 3 - strada */

        // Initialize noleggio
        List<MsqEvent> noleggioList = eventListManager.getServerNoleggio();
        int nextEventNoleggio = MsqEvent.getNextEvent(noleggioList, NOLEGGIO_SERVER);
        systemList.addFirst(new MsqEvent(noleggioList.get(nextEventNoleggio).getT(), 1));
        sumList.addFirst(new MsqSum());

        // Initialize ricarica
        systemList.add(1, new MsqEvent(0, 0));
        sumList.add(1, new MsqSum());


        // Initialize parcheggio
        systemList.add(2, new MsqEvent(0, 0));
        sumList.add(2, new MsqSum());

        // Initialize strada
        systemList.add(3, new MsqEvent(0, 0));
        sumList.add(3, new MsqSum());

        eventListManager.setSystemEventsList(systemList);
    }

    public void simulation() throws Exception {
        System.out.println("Avvio simulazione");
        simpleSimulation();
    }

    private void simpleSimulation() throws Exception {
        int e;
        List<MsqEvent> eventList = eventListManager.getSystemEventsList();

        while (getNextEvent(eventList) != -1) {
            e = getNextEvent(eventList);
            msqT.setNext(eventList.get(e).getT());
            this.area = this.area + (msqT.getNext() - msqT.getCurrent()) * number;
            msqT.setCurrent(msqT.getNext());

            if (e < 3) {
                controllerList.get(e).simpleSimulation();
                eventList = eventListManager.getSystemEventsList();
            } else throw new Exception("Invalid event");
        }
    }

    /* Fetch index of most imminent event */
    private int getNextEvent(List<MsqEvent> eventList) {
        double threshold = Double.MAX_VALUE;
        int e = -1;
        int i = 0;

        for (MsqEvent event: eventList) {
            if(event.getT() < threshold && event.getX() == 1){
                threshold = event.getT();
                e = i;
            }
            i++;
        }
        return e;
    }
}
