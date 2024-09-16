package Controller;

import Libs.Rngs;
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

    @Override
    public void simpleSimulation() {
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

    /* Finite horizon simulation */
    public void simpleSimulationOld() {
        eventList = eventListManager.getServerNoleggio();
        List<MsqEvent> internalEventList = eventListManager.getIntQueueNoleggio(); //Lista delle macchine disponibili

        /* No event to process */
        //Prima parte: Ci sono arrivi esterni?
        //Seconda parte: Non ci sono car dispo
        //terza parte: centro vuoto
        if (eventList.getFirst().getX() == 0 && internalEventList.isEmpty() && this.number == 0) return;

        if ((e = MsqEvent.getNextEvent(eventList, eventList.size() - 1)) == -1) return;
        msqT.setNext(eventList.get(e).getT());
        area += (msqT.getNext() - msqT.getCurrent()) * number;
        msqT.setCurrent(msqT.getNext());

        //prima parte: Arrivo esterno
        //Seconda parte: arrivo eserno
        //Terza parte: MAACCHINA DISPONIBILE
        if ((e == 0 || e == eventList.size() - 1) && !internalEventList.isEmpty()) {       /* External arrival (λ) and a car is ready to be rented */
            if (e == 0) {
                this.number++;

                eventList.getFirst().setT(msqT.getCurrent() + distr.getArrival(0)); /* Get new arrival from passenger arrival */ //Setti il prossimo arrivo esterno
            }

            if (eventList.get(e).getT() > STOP_FIN) {
                eventList.get(e).setX(0);
                eventListManager.setServerNoleggio(eventList);
            }

            //Se il server è disponibile(0 = libero)
            if (eventList.get(1).getX() == 0) {
                /* Update number of available cars in the center depending on where the car comes from */
                //CarsInParcheggio : # DI MACCHINE PRESENTI IN RICARICA --> Anche noleggiabile
                if (internalEventList.getFirst().isFromParking()) {
                    if (eventListManager.reduceCarsInParcheggio() != 0)
                        return;
                } else {
                    if (eventListManager.reduceCarsInRicarica() != 0)
                        return;
                }

                /* Set server as active */ //Vedo quanto tempo ci mette a finire
                eventList.get(1).setT(msqT.getCurrent()); /* mu_noleggio = inf -> no tempo di servizio */
                eventList.get(1).setX(1);
                internalEventList.removeFirst(); //Tolgo una macchina --> è andata in strada

                sumList.get(1).incrementServed();
            } else {
                //il server non è disponibile, imposto il mio tempo di servizio a quando il server si libererà
                // Il tempo che mi libera dall'attesa
                double time = eventList.get(1).getT();

                eventList.getLast().setT(time); //

                // Imposto la x di lamda* a 1
                eventList.getLast().setX(1);
            }
        } else if (e == 0 || e == eventList.size() - 1) {
            if (e == 0) {
                this.number++;

                eventList.getFirst().setT(msqT.getCurrent() + distr.getArrival(0)); /* Get new arrival from passenger arrival */
            }

            // Prendo il tempo minimo tra system.parcheggio e system.ricarica
            double time = Math.min(eventListManager.getSystemEventsList().get(1).getT(), eventListManager.getSystemEventsList().get(2).getT());

            // Imposto il tempo di lamda* al minimo trovato + un epsilon
            eventList.getLast().setT(time + 0.1);

            // Imposto la x di lamda* a 1
            eventList.getLast().setX(1);

            if((e = MsqEvent.getNextEvent(eventList, NOLEGGIO_SERVER)) != 0) {
                eventListManager.getSystemEventsList().getFirst().setX(0);

                return;
            }

            eventListManager.getSystemEventsList().getFirst().setT(eventList.get(e).getT());
        } else {
            this.index++;
            this.number--;

            if (this.number == 0) eventList.getLast().setX(0);      /* Disable λ* */

            /* Routing from Noleggio to Strada */
            List<MsqEvent> serverStrada = eventListManager.getServerStrada();
            serverStrada.getFirst().setT(eventList.get(e).getT());
            serverStrada.getFirst().setX(1);
            eventListManager.setServerStrada(serverStrada);

            s = e;
            if (number >= NOLEGGIO_SERVER && !internalEventList.isEmpty()) {        /* there is some jobs in queue, place another job in this server */
                if (internalEventList.getFirst().isFromParking()) {
                    if (eventListManager.reduceCarsInParcheggio() != 0) {
                        this.index--;
                        this.number++;

                        eventList.getLast().setX(1);

                        return;
                    }

                    if (!eventListManager.getServerParcheggio().isEmpty())
                        eventListManager.getServerParcheggio().getFirst().setT(eventList.get(e).getT());
                } else {
                    if (eventListManager.reduceCarsInRicarica() != 0) {
                        this.index--;
                        this.number++;

                        eventList.getLast().setX(1);

                        return;
                    }

                    if (!eventListManager.getServerRicarica().isEmpty())
                        eventListManager.getServerRicarica().getFirst().setT(eventList.get(e).getT());
                }

                eventList.get(s).setT(msqT.getCurrent());
                internalEventList.removeFirst();

                sumList.get(s).incrementServed();
            } else if (number == 0) {
                eventList.get(s).setX(0);
            }

            /* Update centralized event list */
            List<MsqEvent> systemList = eventListManager.getSystemEventsList();
            systemList.get(3).setT(eventList.get(e).getT());
            systemList.get(3).setX(1);
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
        List<MsqEvent> eventList = eventListManager.getServerNoleggio();
        List<MsqEvent> internalEventList = eventListManager.getIntQueueNoleggio();

        /* No event to process */
        if (eventList.getFirst().getX() == 0 && internalEventList.isEmpty() && this.number == 0) return;

        if ((e = MsqEvent.getNextEvent(eventList, eventList.size() - 1)) == -1) return;
        msqT.setNext(eventList.get(e).getT());
        area += (msqT.getNext() - msqT.getCurrent()) * number;
        msqT.setCurrent(msqT.getNext());

        if ((e == 0 || e == eventList.size() - 1) && !internalEventList.isEmpty()) {       /* External arrival (λ) and a car is ready to be rented */
            if (e == 0) {
                this.number++;

                eventList.getFirst().setT(msqT.getCurrent() + distr.getArrival(0)); /* Get new arrival from passenger arrival */
            }

            if (eventList.get(e).getT() > STOP_INF) {
                eventList.get(e).setX(0);
                eventListManager.setServerNoleggio(eventList);
            }

            BatchMeans.incrementJobInBatch();
            jobInBatch++;

            if (jobInBatch % B == 0 && jobInBatch <= B * K) {
                batchDuration = msqT.getCurrent() - msqT.getBatchTimer();

                calculateBatchStatistics();
                nBatch++;
                msqT.setBatchTimer(msqT.getCurrent());
            }

            if (eventList.get(1).getX() == 0) {
                /* Update number of available cars in the center depending on where the car comes from */
                if (internalEventList.getFirst().isFromParking()) {
                    if (eventListManager.reduceCarsInParcheggio() != 0)
                        return;
                } else {
                    if (eventListManager.reduceCarsInRicarica() != 0)
                        return;
                }

                /* Set server as active */
                eventList.get(1).setT(msqT.getCurrent()); /* mu_noleggio = inf -> no tempo di servizio */
                eventList.get(1).setX(1);
                internalEventList.removeFirst();

                sumList.get(1).incrementServed();
            } else {
                // Il tempo che mi libera dall'attesa
                double time = eventList.get(1).getT();

                eventList.getLast().setT(time);

                // Imposto la x di lamda* a 1
                eventList.getLast().setX(1);
            }
        } else if (e == 0 || e == eventList.size() - 1) {
            if (e == 0) {
                this.number++;

                eventList.getFirst().setT(msqT.getCurrent() + distr.getArrival(0)); /* Get new arrival from passenger arrival */
            }

            BatchMeans.incrementJobInBatch();
            jobInBatch++;

            if (jobInBatch % B == 0 && jobInBatch <= B * K) {
                batchDuration = msqT.getCurrent() - msqT.getBatchTimer();

                calculateBatchStatistics();
                nBatch++;
                msqT.setBatchTimer(msqT.getCurrent());
            }

            // Prendo il tempo minimo tra system.parcheggio e system.ricarica
            double time = Math.min(eventListManager.getSystemEventsList().get(1).getT(), eventListManager.getSystemEventsList().get(2).getT());

            // Imposto il tempo di lamda* al minimo trovato + un epsilon
            eventList.getLast().setT(time + 0.1);

            // Imposto la x di lamda* a 1
            eventList.getLast().setX(1);

            if((e = MsqEvent.getNextEvent(eventList, NOLEGGIO_SERVER)) != 0) {
                eventListManager.getSystemEventsList().getFirst().setX(0);

                return;
            }

            eventListManager.getSystemEventsList().getFirst().setT(eventList.get(e).getT());
        } else {
            this.index++;
            this.number--;

            if (this.number == 0) eventList.getLast().setX(0);      /* Disable λ* */

            /* Routing from Noleggio to Strada */
            List<MsqEvent> serverStrada = eventListManager.getServerStrada();
            serverStrada.getFirst().setT(eventList.get(e).getT());
            serverStrada.getFirst().setX(1);
            eventListManager.setServerStrada(serverStrada);

            s = e;
            if (number >= NOLEGGIO_SERVER && !internalEventList.isEmpty()) {        /* there is some jobs in queue, place another job in this server */
                if (internalEventList.getFirst().isFromParking()) {
                    if (eventListManager.reduceCarsInParcheggio() != 0) {
                        this.index--;
                        this.number++;

                        eventList.getLast().setX(1);

                        return;
                    }

                    if (!eventListManager.getServerParcheggio().isEmpty())
                        eventListManager.getServerParcheggio().getFirst().setT(eventList.get(e).getT());
                } else {
                    if (eventListManager.reduceCarsInRicarica() != 0) {
                        this.index--;
                        this.number++;

                        eventList.getLast().setX(1);

                        return;
                    }

                    if (!eventListManager.getServerRicarica().isEmpty())
                        eventListManager.getServerRicarica().getFirst().setT(eventList.get(e).getT());
                }

                eventList.get(s).setT(msqT.getCurrent());
                internalEventList.removeFirst();

                sumList.get(s).incrementServed();
            } else if (number == 0) {
                eventList.get(s).setX(0);
            }

            /* Update centralized event list */
            List<MsqEvent> systemList = eventListManager.getSystemEventsList();
            systemList.get(3).setT(eventList.get(e).getT());
            systemList.get(3).setX(1);
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
    public void printIteration(boolean isFinite, int event, int runNumber, double time) {
        double responseTime = area / index;
        double avgPopulationInNode = area / msqT.getCurrent();

        double area = this.area;
        for(int i = 1; i == NOLEGGIO_SERVER; i++) {
            area -= sumList.get(i).getService();
        }

        double waitingTime = area / index;
        double avgPopulationInQueue = area / msqT.getCurrent();

        FileCSVGenerator.writeFile(isFinite, event, runNumber, time, responseTime, avgPopulationInNode, waitingTime, avgPopulationInQueue);
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
