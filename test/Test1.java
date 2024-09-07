import Controller.*;
import Model.MsqEvent;

import static Model.Constants.*;

public class Test1 {
    public static void main(String[] args) {
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
