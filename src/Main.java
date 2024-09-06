import Controller.Sistema;
import Libs.Rngs;

import java.util.Scanner;

import static Model.Constants.*;

public class Main {
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
        Sistema sys = new Sistema();

        switch (simulationType) {
            case 0: /* Finite horizon */
                sys.simulation(simulationType);
                break;
            case 1: /* Infinite horizon */
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