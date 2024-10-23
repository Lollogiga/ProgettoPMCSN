package Utils;

import Libs.Rngs;

import java.util.ArrayList;
import java.util.List;

import static Utils.Constants.*;

/* Like a singleton class */
public class ReplicationStats {
    /* Lists of replication's values */
    private List<Double> avgPopulationInNode;
    private List<Double> responseTime;
    private List<Double> avgPopulationInQueue;
    private List<Double> waitingTimeInQueue;
    private List<Double> utilization;

    /* Mean estimation */
    private double meanPopulationInQueue;
    private double meanWaitingTimeInQueue;
    private double meanResponseTime;
    private double meanPopulationInNode;
    private double meanUtilization;

    /* Standard deviation estimation */
    private double devPopulationInQueue;
    private double devWaitingTimeInQueue;
    private double devResponseTime;
    private double devPopulationInNode;
    private double devUtilization;

    /* Singleton instances */
    public static ReplicationStats repStrada = null;
    public static ReplicationStats repRicarica = null;
    public static ReplicationStats repParcheggio = null;
    public static ReplicationStats repNoleggio = null;

    public static ReplicationStats getInstance(int instanceType) {
        switch (instanceType) {
            case 0:
                if (repStrada == null)
                    repStrada = new ReplicationStats();
                return repStrada;
            case 1:
                if (repRicarica == null)
                    repRicarica = new ReplicationStats();
                return repRicarica;
            case 2:
                if (repParcheggio == null)
                    repParcheggio = new ReplicationStats();
                return repParcheggio;
            case 3:
                if (repNoleggio == null)
                    repNoleggio = new ReplicationStats();
                return repNoleggio;
            default:
                throw new IllegalArgumentException("Invalid instance type");
        }
    }

    public ReplicationStats() {
        this.avgPopulationInNode = new ArrayList<>(REPLICATION);
        this.responseTime = new ArrayList<>(REPLICATION);
        this.avgPopulationInQueue = new ArrayList<>(REPLICATION);
        this.waitingTimeInQueue = new ArrayList<>(REPLICATION);
        this.utilization = new ArrayList<>(REPLICATION);

        for (int i = 0; i < REPLICATION; i++) {
            this.avgPopulationInNode.add(0.0);
            this.responseTime.add(0.0);
            this.avgPopulationInQueue.add(0.0);
            this.waitingTimeInQueue.add(0.0);
            this.utilization.add(0.0);
        }
    }

    public double getDevPopulationInNode() {
        return devPopulationInNode;
    }

    public double getDevPopulationInQueue() {
        return devPopulationInQueue;
    }

    public double getDevResponseTime() {
        return devResponseTime;
    }

    public double getDevUtilization() {
        return devUtilization;
    }

    public double getDevWaitingTimeInQueue() {
        return devWaitingTimeInQueue;
    }

    public double getMeanPopulationInNode() {
        return meanPopulationInNode;
    }

    public double getMeanPopulationInQueue() {
        return meanPopulationInQueue;
    }

    public double getMeanResponseTime() {
        return meanResponseTime;
    }

    public double getMeanUtilization() {
        return meanUtilization;
    }

    public double getMeanWaitingTimeInQueue() {
        return meanWaitingTimeInQueue;
    }

    public List<Double> getWaitingTimeInQueue() {
        return waitingTimeInQueue;
    }

    public List<Double> getAvgPopulationInNode() {
        return avgPopulationInNode;
    }

    public List<Double> getResponseTime() {
        return responseTime;
    }

    public List<Double> getAvgPopulationInQueue() {
        return avgPopulationInQueue;
    }

    public List<Double> getUtilization() {
        return utilization;
    }

    public double getStandardDeviation(int type) {
        return switch (type) {
            case 0 -> devPopulationInQueue;
            case 1 -> devPopulationInNode;
            case 2 -> devResponseTime;
            case 3 -> devUtilization;
            case 4 -> devWaitingTimeInQueue;
            default -> throw new IllegalArgumentException("Invalid type");
        };
    }

    public void setMeanPopulationInNode(double meanPopulationInNode) {
        this.meanPopulationInNode = meanPopulationInNode;
    }

    public void setMeanPopulationInQueue(double meanPopulationInQueue) {
        this.meanPopulationInQueue = meanPopulationInQueue;
    }

    public void setMeanResponseTime(double meanResponseTime) {
        this.meanResponseTime = meanResponseTime;
    }

    public void setMeanUtilization(double meanUtilization) {
        this.meanUtilization = meanUtilization;
    }

    public void setMeanWaitingTimeInQueue(double meanWaitingTimeInQueue) {
        this.meanWaitingTimeInQueue = meanWaitingTimeInQueue;
    }

    public void setDevPopulationInQueue(double devPopulationInQueue) {
        this.devPopulationInQueue = devPopulationInQueue;
    }

    public void setDevResponseTime(double devResponseTime) {
        this.devResponseTime = devResponseTime;
    }

    public void setDevUtilization(double devUtilization) {
        this.devUtilization = devUtilization;
    }

    public void setDevWaitingTimeInQueue(double devWaitingTimeInQueue) {
        this.devWaitingTimeInQueue = devWaitingTimeInQueue;
    }

    public void setDevPopulationInNode(double devPopulationInNode) {
        this.devPopulationInNode = devPopulationInNode;
    }

    public void insertAvgPopulationInNode(double avgPopulationInNode, int batchIndex) {
        this.avgPopulationInNode.set(batchIndex, avgPopulationInNode);
    }

    public void insertResponseTime(double responseTime, int repIndex) {
        this.responseTime.set(repIndex, responseTime);
    }

    public void insertAvgPopulationInQueue(double avgPopulationInQueue, int repIndex) {
        this.avgPopulationInQueue.set(repIndex, avgPopulationInQueue);
    }

    public void insertWaitingTimeInQueue(double waitingTimeInQueue, int repIndex) {
        this.waitingTimeInQueue.set(repIndex, waitingTimeInQueue);
    }

    public void insertUtilization(double utilization, int repIndex) {
        this.utilization.set(repIndex, utilization);
    }

    public void setStandardDeviation(List<Double> statList, int type) {
        if (statList.isEmpty())
            throw new IllegalArgumentException("List cannot be empty");

        // Calculate mean (μ)
        double mean = 0.0;
        for (double elemento : statList) {
            mean += elemento;
        }
        mean /= REPLICATION;

        // Calculate the sum of the squares of the differences from the mean
        double temp = 0.0;
        for (Double element : statList) {
            double difference = element - mean;
            temp += difference * difference;
        }

        // Calculate standard deviation (σ)
        double devStd = Math.sqrt(temp / REPLICATION);

        switch (type) {
            case 0:
                setMeanPopulationInQueue(mean);
                setDevPopulationInQueue(devStd);
                break;
            case 1:
                setMeanPopulationInNode(mean);
                setDevPopulationInNode(devStd);
                break;
            case 2:
                setMeanResponseTime(mean);
                setDevResponseTime(devStd);
                break;
            case 3:
                setMeanUtilization(mean);
                setDevUtilization(devStd);
                break;
            case 4:
                setMeanWaitingTimeInQueue(mean);
                setDevWaitingTimeInQueue(devStd);
                break;
            default:
                throw new IllegalArgumentException("Invalid type");
        }
    }
}
