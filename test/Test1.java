import Controller.*;
import Model.MsqEvent;

import static Model.Constants.*;

public class Test1 {
    public static void main(String[] args) throws Exception {
        testParcheggioUserQueue();
    }

    public static void testParcheggioUserQueue() throws Exception {
        EventListManager eventListManager = EventListManager.getInstance();

        Noleggio noleggio = new Noleggio();
        Parcheggio parcheggio = new Parcheggio();
        Ricarica ricarica = new Ricarica();
        Strada strada = new Strada();

        Sistema sistema = new Sistema(123456789);

//        for (int i = eventListManager.getCarsInParcheggio(); i < PARCHEGGIO_SERVER; i++) {
//            eventListManager.incrementCarsInParcheggio();
//            eventListManager.incrementCars();
//
//            eventListManager.getIntQueueNoleggio().add(new MsqEvent(0, 1, true));
//        }

        parcheggio.simpleSimulation();
        parcheggio.simpleSimulation();
        parcheggio.simpleSimulation();
        parcheggio.simpleSimulation();
        parcheggio.simpleSimulation();
        parcheggio.simpleSimulation(); // <- Lose cars from here, queue is full
        parcheggio.simpleSimulation();
        parcheggio.simpleSimulation();
        parcheggio.simpleSimulation();


        noleggio.simpleSimulation();    // <- remove one car from parking
        noleggio.simpleSimulation();
        noleggio.simpleSimulation();    // <- remove one car from parking
        noleggio.simpleSimulation();

        eventListManager.getServerParcheggio().getFirst().setT(940.231241);

        parcheggio.simpleSimulation();
        parcheggio.simpleSimulation();

        parcheggio.simpleSimulation();
        parcheggio.simpleSimulation();

        parcheggio.simpleSimulation();
        parcheggio.simpleSimulation();

        parcheggio.simpleSimulation();
        parcheggio.simpleSimulation();

        parcheggio.simpleSimulation();
        parcheggio.simpleSimulation();

        parcheggio.simpleSimulation();
        parcheggio.simpleSimulation();

        noleggio.simpleSimulation();
        noleggio.simpleSimulation();

        parcheggio.simpleSimulation();
        parcheggio.simpleSimulation();

        noleggio.simpleSimulation();
        noleggio.simpleSimulation();

        parcheggio.simpleSimulation();
        parcheggio.simpleSimulation();

        parcheggio.simpleSimulation();
        parcheggio.simpleSimulation();

        parcheggio.simpleSimulation();
        parcheggio.simpleSimulation();

        parcheggio.simpleSimulation();
        parcheggio.simpleSimulation();

        parcheggio.simpleSimulation();
        parcheggio.simpleSimulation();

        parcheggio.simpleSimulation();
        parcheggio.simpleSimulation();

        parcheggio.simpleSimulation();
        parcheggio.simpleSimulation();

        parcheggio.simpleSimulation();
        parcheggio.simpleSimulation();
    }

    private static void testNoleggioUserQueue() throws Exception {
        EventListManager eventListManager = EventListManager.getInstance();

        Noleggio noleggio = new Noleggio();
        Parcheggio parcheggio = new Parcheggio();
        Ricarica ricarica = new Ricarica();
        Strada strada = new Strada();

        Sistema sistema = new Sistema(123456789);

        for (int i = 0; i < INIT_PARK_CARS * 2; i++)
            noleggio.simpleSimulation();

        noleggio.simpleSimulation();
        noleggio.simpleSimulation();
        noleggio.simpleSimulation();
        noleggio.simpleSimulation();
        noleggio.simpleSimulation();

        parcheggio.simpleSimulation();
        parcheggio.simpleSimulation();
        parcheggio.simpleSimulation();

        noleggio.simpleSimulation();
        noleggio.simpleSimulation();

        parcheggio.simpleSimulation();
        parcheggio.simpleSimulation();

        noleggio.simpleSimulation();
        noleggio.simpleSimulation();

        for (int i = 0; i < 60; i++) {
            eventListManager.incrementCarsInParcheggio();
            eventListManager.getIntQueueNoleggio().add(new MsqEvent(1000 + i, 1, true));
        }

        for (int i = 0; i < 60; i++) {
            noleggio.simpleSimulation();
            noleggio.simpleSimulation();
        }
    }


}
