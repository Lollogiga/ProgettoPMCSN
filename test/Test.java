import Controller.EventListManager;
import Controller.Parcheggio;
import Model.MsqEvent;

import java.util.ArrayList;
import java.util.List;

public class Test {

    public static void main(String[] args) {
        new Test().testExternalParcheggio();
    }

    private void testParcheggio() {
        EventListManager eventListManager = EventListManager.getInstance();

        Parcheggio park = new Parcheggio();

        List<MsqEvent> intQueueParcheggio = eventListManager.getIntQueueParcheggio();
        intQueueParcheggio.add(new MsqEvent(2, 1));
        eventListManager.setIntQueueParcheggio(intQueueParcheggio);

        park.simpleSimulation();
        park.simpleSimulation();

        park.printResult();
    }

    private void testExternalParcheggio(){
        EventListManager eventListManager = EventListManager.getInstance();
        Parcheggio park = new Parcheggio();
        List<MsqEvent> intQueueParcheggio = eventListManager.getIntQueueParcheggio();

        intQueueParcheggio.getLast().setX(1);
        intQueueParcheggio.getLast().setT(2);

        park.simpleSimulation();

        park.printResult();
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

        System.out.println("rList: " + rList);
    }
}
