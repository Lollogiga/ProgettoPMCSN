package Controller;

public interface Center {
    /* Finite horizon simulation */
    void simpleSimulation();

    /* Infinite horizon simulation */
    void infiniteSimulation();

    void printResult();

    void calculateBatchStatistics();
}
