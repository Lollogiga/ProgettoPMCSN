import Controller.Sistema;
import Libs.Rngs;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static Model.Constants.*;

public class Main {
    private static List<Long> seedList;

    public static void main(String[] args) throws Exception {
        Rngs rngs = new Rngs();
        rngs.plantSeeds(SEED);

        System.out.println("---- Choose type of simulation ----");
        System.out.println("0 - Finite horizon simulation ");
        System.out.println("1 - Infinite horizon simulation ");

        int simType = getChoice();
        run(simType);
    }

    public static void run(int simulationType) throws Exception {
        Rngs rngs = new Rngs();

        switch (simulationType) {
            case 0: /* Finite horizon */
                /* Initialize Seed lists */
                seedList = new ArrayList<>(REPLICATION);
                for (int i = 0; i < REPLICATION; i++) {
                    seedList.add(0L);
                }
                /* Set first seed*/
                seedList.set(0, SEED);

                /* Simulate REPLICATION = 64 run */
                for (int i = 0; i < REPLICATION; i++) {
                    /* Start simulation with seed[i] */
                    Sistema sys = new Sistema(seedList.get(i));
                    sys.simulation(simulationType);

                    /* Generate new seed */
                    if (i + 1 < REPLICATION) {
                        rngs.selectStream(255);
                        rngs.random(); // TODO: se non metto questa riga rngs.getSeed() è uguale dalla seconda iterazione fino alla fine. Così cambia.

                        seedList.set(i + 1, rngs.getSeed());
                    }
                }
                break;
            case 1: /* Infinite horizon */
                Sistema sys = new Sistema(SEED);
                sys.simulation(simulationType);
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