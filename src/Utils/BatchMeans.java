package Utils;

public class BatchMeans {
    private static double nBatch = 1.0;
    private static int nJobInBatch = 0; /* Number of total jobs in system */

    public static void incrementNBatch(){
        nBatch++;
    }

    public static void incrementJobInBatch(){
        nJobInBatch++;
    }

    public static int getJobInBatch() {
        return nJobInBatch;
    }

    public static double getNBatch() {
        return nBatch;
    }
}
