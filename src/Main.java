import Controller.Sistema;
import Libs.Rngs;
import Utils.FileCSVGenerator;
import Utils.ReplicationStats;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static Controller.Sistema.centerList;
import static Utils.Constants.*;

public class Main {

    public static void main(String[] args) throws Exception {
        Rngs rngs = new Rngs();
        rngs.plantSeeds(SEED);

        FileCSVGenerator.deleteFolder("resources/results");

        System.out.println("---- Choose type of simulation ----");
        System.out.println("0 - Finite horizon simulation ");
        System.out.println("1 - Infinite horizon simulation ");

        int simType = getChoice();
        runSim(simType);

        // NON CANCELLARE, è un test per far eseguire lo script python in automatico. Non funziona perché non facciamo la flush sui file una volta terminata la simulazione, o almeno credo che sia per questo motivo
//        try {
//            ProcessBuilder pb = new ProcessBuilder("python", "pyGraphs\\main.py");
//            // Inizia il processo
//            Process process = pb.start();
//            int exitCode = process.waitFor();
//
//            System.out.println("\n\nProcesso Python terminato con codice di uscita: " + exitCode);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    public static void runSim(int simulationType) throws Exception {
        Rngs rngs = new Rngs();

        switch (simulationType) {
            case 0: /* Finite horizon */
                /* Initialize Seed lists */
                List<Long> seedList = new ArrayList<>(REPLICATION);
                for (int i = 0; i < REPLICATION; i++) {
                    seedList.add(0L);
                }
                /* Set first seed*/
                seedList.set(0, SEED);

                /* Simulate REPLICATION = 64 run */
                for (int i = 0; i < REPLICATION; i++) {
                    rngs.plantSeeds(seedList.get(i));;

                    /* Start simulation with seed[i] */
                    Sistema sys = new Sistema(rngs);
                    sys.simulation(simulationType, seedList.get(i), i + 1);

                    /* Generate new seed */
                    if (i + 1 < REPLICATION) {
                        rngs.selectStream(255);
                        seedList.set(i + 1, rngs.getSeed());
                    }
                }

                /* Final stats in transient state */
                for (int i = 0; i < NODES; i++)
                    centerList.get(i).printFinalStatsTransitorio();

                break;
            case 1: /* Infinite horizon */
                rngs.plantSeeds(SEED);
                Sistema sys = new Sistema(rngs);
                sys.simulation(simulationType, -1, -1); // -1 is to ignore input
                break;
            default:
                throw new Exception("Invalid simulation type");
        }
    }

    private static int getChoice() {
        Scanner input = new Scanner(System.in);
        int choice;

        while (true) {
            System.out.println("Please, make a choice: ");

            choice = input.nextInt();
            if (choice >= 0 && choice <= 1) break;

            System.out.println("Not valid choice!");
        }

        return choice;
    }
}