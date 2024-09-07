package Controller;

public interface Center {
    /* Finite horizon simulation */
    void simpleSimulation() throws Exception;

    /* Infinite horizon simulation */
    void infiniteSimulation();

    void calculateBatchStatistics();

    int getNumJob();

    void printResult(int runNumber, long seed);
}
