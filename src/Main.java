import Controller.Sistema;
import Libs.Rngs;

import static Model.Constants.*;

public class Main {
    public static void main(String[] args) throws Exception {
        Rngs rngs = new Rngs();
        rngs.plantSeeds(SEED);
        run();
    }

    public static void run() throws Exception {
        Sistema sistema = new Sistema();
        sistema.simulation();
    }
}