package Utils;

import java.util.ArrayList;
import java.util.List;

import static Model.Constants.K;

public class SimulationResults {
    /* Lists of batch's values */
    private List<Double> avgPopulationInNode = new ArrayList<>();
    private List<Double> responseTime = new ArrayList<>();
    private List<Double> avgPopulationInQueue = new ArrayList<>();
    private List<Double> waitingTimeInQueue = new ArrayList<>();
    private List<Double> utilization = new ArrayList<>();

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

    public SimulationResults() {
        this.avgPopulationInNode = new ArrayList<>(K);
        this.responseTime = new ArrayList<>(K);
        this.avgPopulationInQueue = new ArrayList<>(K);
        this.waitingTimeInQueue = new ArrayList<>(K);
        this.utilization = new ArrayList<>(K);
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

    public void insertResponseTime(double responseTime, int batchIndex) {
        this.responseTime.set(batchIndex, responseTime);
    }

    public void insertAvgPopulationInQueue(double avgPopulationInQueue, int batchIndex) {
        this.avgPopulationInQueue.set(batchIndex, avgPopulationInQueue);
    }

    public void insertWaitingTimeInQueue(double waitingTimeInQueue, int batchIndex) {
        this.waitingTimeInQueue.set(batchIndex, waitingTimeInQueue);
    }

    public void insertUtilization(double utilization, int batchIndex) {
        this.utilization.set(batchIndex, utilization);
    }

    public double getVariance(double[] batchMean, int type){

        if(batchMean.length == 0){
            System.out.println("Batch mean is empty");
            return -1;
        }

        //Calculate mean:
        double mean = 0.0;
        for (double elemento : batchMean) {
            mean += elemento;
        }
        mean /= K;

        // Calculate the sum of the squares of the differences from the mean
        double temp = 0.0;
        for (double element: batchMean ){
            double difference = element - mean;
            temp += difference * difference;
        }

        //Calculate variance

        double devStd = Math.sqrt(temp / K);

        if(type == 0){
            setMeanPopulationInQueue(mean);
            setDevPopulationInNode(devStd);
        }
        else if(type == 1){
            setMeanPopulationInNode(mean);
            setDevPopulationInNode(devStd);
        }
        else if(type == 2){
            setMeanResponseTime(mean);
            setDevResponseTime(devStd);
        }
        else if(type == 3){
            setMeanUtilization(mean);
            setDevUtilization(devStd);
        }
        else if(type == 4){
            setMeanWaitingTimeInQueue(mean);
            setDevWaitingTimeInQueue(devStd);
        }
        return devStd;
    }
}
