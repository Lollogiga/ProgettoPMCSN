import Controller.*;
import Model.MsqEvent;

import java.util.ArrayList;
import java.util.List;

import static Model.Constants.INIT_PARK_CARS;

public class Test {

    public static void main(String[] args) {
        new Noleggio();
        new Strada();
        new Parcheggio();
        new Ricarica();

        new Sistema();

        new Test().testParcheggio();
        new Test().testStrada();
        new Test().testNoleggio();
        new Test().testExternalParcheggio();
        new Test().testMsqEventGetNextEvent();
    }



    private void testStrada() {
        EventListManager eventListManager = EventListManager.getInstance();

        Strada strada = new Strada();

        List<MsqEvent> eventList = eventListManager.getServerStrada();
        eventList.set(0, new MsqEvent(2.324, 1));

        strada.simpleSimulation();
        eventList = eventListManager.getServerStrada();
        eventList.set(0, new MsqEvent(2.524, 1));
        strada.simpleSimulation();
        eventList = eventListManager.getServerStrada();
        eventList.set(0, new MsqEvent(2.724, 1));
        strada.simpleSimulation();
        eventList = eventListManager.getServerStrada();
        eventList.set(0, new MsqEvent(2.924, 1));
        strada.simpleSimulation();
        strada.simpleSimulation();
        strada.simpleSimulation();
        strada.simpleSimulation();
    }

    private void testNoleggio() {
        EventListManager eventListManager = EventListManager.getInstance();

        new Strada();

        List<MsqEvent> carInRentalStation = eventListManager.getIntQueueNoleggio();
        for (int i = 0; i < INIT_PARK_CARS; i++) {
            carInRentalStation.add(i, new MsqEvent(0, 1, true));
        }
        eventListManager.setIntQueueNoleggio(carInRentalStation);

        Noleggio noleggio = new Noleggio();

        noleggio.simpleSimulation();
        noleggio.simpleSimulation();
        noleggio.simpleSimulation();
        noleggio.simpleSimulation();
        noleggio.simpleSimulation();
        noleggio.simpleSimulation();

        noleggio.printResult();
    }

    private void testParcheggio() {
        EventListManager eventListManager = EventListManager.getInstance();

        Parcheggio park = new Parcheggio();

        List<MsqEvent> intQueueParcheggio = eventListManager.getIntQueueParcheggio();
        intQueueParcheggio.add(new MsqEvent(2, 1));
        eventListManager.setIntQueueParcheggio(intQueueParcheggio);

        park.simpleSimulation();
        park.simpleSimulation();
    }

    private void testExternalParcheggio(){
        EventListManager eventListManager = EventListManager.getInstance();
        Parcheggio park = new Parcheggio();
        List<MsqEvent> intQueueParcheggio = eventListManager.getServerParcheggio();

        intQueueParcheggio.getFirst().setX(1);
        intQueueParcheggio.getFirst().setT(2);

        park.simpleSimulation();
    }

    private void testMsqEventGetNextEvent() {
        List<MsqEvent> eventList = new ArrayList<>(5 + 2);

        for (int i = 0; i < 7; i++) {
            eventList.add(new MsqEvent(0, 0));
        }

        eventList.getFirst().setX(1);
        eventList.getFirst().setT(4);

        eventList.getLast().setX(1);
        eventList.getLast().setT(3);

        int rList = MsqEvent.getNextEvent(eventList, 6);
    }
}
