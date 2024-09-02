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

        // noleggio
        List<MsqEvent> noleggioList = eventListManager.getServerNoleggio();
        int nextEventNoleggio = MsqEvent.getNextEvent(noleggioList, NOLEGGIO_SERVER);
        if (nextEventNoleggio != -1) {
            systemList.addFirst(new MsqEvent(noleggioList.get(nextEventNoleggio).getT(), 1));
            sumList.addFirst(new MsqSum());
        } else {
            systemList.addFirst(new MsqEvent(0, 0));
            sumList.addFirst(new MsqSum());
        }

        // ricarica
        List<MsqEvent> ricaricaList = eventListManager.getServerRicarica();
        int nextEventRicarica = MsqEvent.getNextEvent(ricaricaList, RICARICA_SERVER);
        if (nextEventRicarica != -1) {
            systemList.add(1, new MsqEvent(ricaricaList.get(nextEventRicarica).getT(), 1));
            sumList.add(1, new MsqSum());
        } else {
            systemList.add(1, new MsqEvent(0, 0));
            sumList.add(1, new MsqSum());
        }

        // parcheggio
        List<MsqEvent> parcheggioList = eventListManager.getServerParcheggio();
        int nextEventParcheggio = MsqEvent.getNextEvent(parcheggioList, PARCHEGGIO_SERVER);
        if (nextEventParcheggio != -1) {
            systemList.add(2, new MsqEvent(ricaricaList.get(nextEventParcheggio).getT(), 1));
            sumList.add(2, new MsqSum());
        } else {
            systemList.add(2, new MsqEvent(0, 0));
            sumList.add(2, new MsqSum());
        }

        // strada
        systemList.add(3, new MsqEvent(0, 0));
        sumList.add(3, new MsqSum());

        eventListManager.setSystemEventsList(systemList);
    }

    public void simulation() throws Exception {
        System.out.println("Avvio simulazione");
        simpleSimulation();
    }

    public void simpleSimulation() throws Exception {
        int e;
        //prende la lista di eventi per il sistema
        List<MsqEvent> eventList = eventListManager.getSystemEventsList();
        /*
         * il ciclo continua finché non tutti i nodi sono idle e il tempo supera lo stop time
         */
        while(getNextEvent(eventList)!=-1) {
            //prende l'indice del primo evento nella lista
            e = getNextEvent(eventList);

            //imposta il tempo del prossimo evento
            msqT.setNext(eventList.get(e).getT());
            //si calcola l'area dell'integrale
            this.area = this.area + (msqT.getNext() - msqT.getCurrent()) * number;
            //imposta il tempo corrente a quello dell'evento corrente
            msqT.setCurrent(msqT.getNext());

            //Se l'indice calcolato è maggiore di 7 ritorna errore, nel sistema ci sono 7 code
            if (e > 3) {
                throw new Exception("Errore nessun evento tra i precedenti");
            }

            controllerList.get(e).simpleSimulation();

            eventList=eventListManager.getSystemEventsList();
        }

        System.out.println("Sistema\n\n");
        System.out.println("for " + index + " jobs the service node statistics are:\n\n");
        System.out.println("  avg interarrivals .. = " + eventListManager.getSystemEventsList().getFirst().getT() / index);
        System.out.println("  avg wait ........... = " + area / index);
        System.out.println("  avg # in node ...... = " + area / msqT.getCurrent());

        for(int i = 1; i <= NODES; i++) {
            this.area -= sumList.get(i).getService();
        }
        System.out.println("  avg delay .......... = " + this.area / index);
        System.out.println("  avg # in queue ..... = " + this.area / msqT.getCurrent());
        System.out.println("\nthe server statistics are:\n\n");
        System.out.println("    server     utilization     avg service        share\n");
        for(int i = 1; i <= NODES; i++) {
            System.out.println(i + "\t" + sumList.get(i).getService() / msqT.getCurrent() + "\t" + sumList.get(i).getService() / sumList.get(i).getServed() + "\t" + ((double)sumList.get(i).getServed() / index));
        }
        System.out.println("\n");
    }

    /*public void simpleSimulation() throws Exception {
        int e;
        MsqT msqT = new MsqT();
        msqT.setCurrent(START);
        msqT.setNext(START);

        List<MsqEvent> eventList;

        while(breakCondition()) {

            eventList = eventListManager.getSystemEventsList();

            e = getNextEvent(eventList);
            msqT.setNext(eventList.get(e).getT());

            area += (msqT.getNext() - msqT.getCurrent()) * number;

            if (e < 0 || e > 3) {
                throw new Exception("Invalid event");
            }


            controllerList.get(e).simpleSimulation();
            msqT.setCurrent(msqT.getNext());
        }

        // END
    }*/

    /*private boolean breakCondition() {

    }*/

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
