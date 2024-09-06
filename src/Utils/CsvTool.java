package Utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CsvTool {
    final String fileName;

    public CsvTool(String fileName) {
        this.fileName = fileName;
    }
}

/*
public class CsvTool {
    final String fileName;

    public CsvTool(String fileName) {
        this.fileName = fileName;
    }

    public void writeOnFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("ProjName, Classifier Name, Training%, Feature Selection, Sampling, Cost Sensitive, Precision, Recall, Kappa, TP, FP, TN, FN, areaUnderROC, fMeasure");
            writer.newLine();

            for (ClassifierEvaluation ce : classifierEvaluationList) {
                writer.write(projName + ",");
                writer.write(ce.getClassifierName() + ",");
                writer.write(ce.getTrainingPerc() + ",");
                writer.write(ce.getFeatureSelection() + ",");
                writer.write(ce.getSampling() + ",");
                writer.write(ce.getCostSensitive() + ",");
                writer.write(ce.getPrecision() + ",");
                writer.write(ce.getRecall() + ",");
                writer.write(ce.getKappa() + ",");
                writer.write(ce.getTruePositives() + ",");
                writer.write(ce.getFalsePositives() + ",");
                writer.write(ce.getTrueNegatives() + ",");
                writer.write(ce.getFalseNegatives() + ",");
                writer.write(ce.getAreaUnderROC() + ",");
                writer.write(String.valueOf(ce.getfMeasure()));

                writer.newLine();
            }
        } catch (Exception e) {
            Logger.getAnonymousLogger().log(Level.INFO, e.getMessage());
        }
    }

    public void csvReleaseFile(List<Release> releaseList) {
        String filePath = dirPath + INFO + "/releaseList.csv";

        try {
            Files.createDirectories(Paths.get(filePath).getParent());
        } catch (IOException e) {
            Logger.getAnonymousLogger().log(Level.INFO, e.getMessage());

            return;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("ID, Name, Date");
            writer.newLine();

            for (Release release : releaseList) {
                writer.write(release.getId() + ",");
                writer.write(release.getName() + ",");
                writer.write(release.getDate().toString());

                writer.newLine();
            }
        } catch (Exception e) {
            Logger.getAnonymousLogger().log(Level.INFO, e.getMessage());
        }
    }

    public void csvTicketFile(List<Ticket> ticketList) {
        String filePath = dirPath + INFO + "/ticketList.csv";

        try {
            Files.createDirectories(Paths.get(filePath).getParent());
        } catch (IOException e) {
            Logger.getAnonymousLogger().log(Level.INFO, e.getMessage());

            return;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("KEY, IV, OV, FV, AV List");
            writer.newLine();

            for (Ticket ticket : ticketList) {
                writer.write(ticket.getKey() + ",");
                writer.write(ticket.getIV().getName() + ",");
                writer.write(ticket.getOV().getName() + ",");
                writer.write(ticket.getFV().getName() + ",");

                StringBuilder affectedVersions = new StringBuilder("\"{");
                int size = ticket.getAVList().size();
                for (int i = 0; i < size; i++) {
                    Release release = ticket.getAVList().get(i);
                    affectedVersions.append(release.getName());
                    if (i < size - 1) {
                        affectedVersions.append(", ");
                    }
                }
                affectedVersions.append("}\"");

                writer.write(String.valueOf(affectedVersions));

                writer.newLine();
            }
        } catch (Exception e) {
            Logger.getAnonymousLogger().log(Level.INFO, e.getMessage());
        }
    }

    public void csvAcume(List<WekaObject> javaClassList, String classifier, String filters, int index) {
        String filePath = dirPath + ACUME + classifier + "_" + filters + "_" + index + ".csv";

        try {
            Files.createDirectories(Paths.get(filePath).getParent());
        } catch (IOException e) {
            Logger.getAnonymousLogger().log(Level.INFO, e.getMessage());

            return;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("ID, Size, Predicted, Actual");
            writer.newLine();

            int id = 1;
            for (WekaObject wekaObject : javaClassList) {
                writer.write(id + ",");
                writer.write(wekaObject.getSize() + ",");
                writer.write(wekaObject.getPrediction() + ",");
                writer.write(wekaObject.getBuggyness());

                writer.newLine();

                id++;
            }
        } catch (Exception e) {
            Logger.getAnonymousLogger().log(Level.INFO, e.getMessage());
        }
    }

    public void csvWekaResult(List<ClassifierEvaluation> classifierEvaluationList) {
        String filePath = dirPath + WEKA + "/wekaResult.csv";

        try {
            Files.createDirectories(Paths.get(filePath).getParent());
        } catch (IOException e) {
            Logger.getAnonymousLogger().log(Level.INFO, e.getMessage());

            return;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("ProjName, Classifier Name, Training%, Feature Selection, Sampling, Cost Sensitive, Precision, Recall, Kappa, TP, FP, TN, FN, areaUnderROC, fMeasure");
            writer.newLine();

            for (ClassifierEvaluation ce : classifierEvaluationList) {
                writer.write(projName + ",");
                writer.write(ce.getClassifierName() + ",");
                writer.write(ce.getTrainingPerc() + ",");
                writer.write(ce.getFeatureSelection() + ",");
                writer.write(ce.getSampling() + ",");
                writer.write(ce.getCostSensitive() + ",");
                writer.write(ce.getPrecision() + ",");
                writer.write(ce.getRecall() + ",");
                writer.write(ce.getKappa() + ",");
                writer.write(ce.getTruePositives() + ",");
                writer.write(ce.getFalsePositives() + ",");
                writer.write(ce.getTrueNegatives() + ",");
                writer.write(ce.getFalseNegatives() + ",");
                writer.write(ce.getAreaUnderROC() + ",");
                writer.write(String.valueOf(ce.getfMeasure()));

                writer.newLine();
            }
        } catch (Exception e) {
            Logger.getAnonymousLogger().log(Level.INFO, e.getMessage());
        }
    }

    public void csvDatasetFile(List<Release> releaseList, int walk, boolean isTraining) {
        FileWriter fileWriter = null;

        String filePath;

        if(isTraining)
            filePath = dirPath + TRAINING + CSV + projName + "_trainingSet_" + walk + ".csv";
        else
            filePath = dirPath + TESTING + CSV + projName + "_testingSet_" + walk + ".csv";

        try {
            // Create directory if it doesn't exist
            Files.createDirectories(Paths.get(filePath).getParent());

            fileWriter = new FileWriter(filePath);
            fileWriter.append("Release ID, Filepath, Size, LOC Touched, NR, NFix, NAuth, LOC Added, Max LOC Added, Average LOC Added, Churn, Max Churn, Average Churn, Bugginess");
            fileWriter.append("\n");

            for (Release release : releaseList) {
                for (JavaClass javaClass : release.getJavaClassList())
                    writeJavaClass(fileWriter, release.getId(), javaClass);
            }
        } catch (IOException e) {
            Logger.getAnonymousLogger().log(Level.INFO, e.getMessage());
        } finally {
            closeFileWrite(fileWriter);
        }
    }

    private void writeJavaClass(FileWriter fileWriter, int releaseId, JavaClass javaClass) throws IOException {
        String str;

        // Release ID
        str = releaseId + ",";
        // Filepath
        str += javaClass.getClassPath() + ",";
        // LOC
        str += javaClass.getSize() + ",";
        // locTouched
        str += javaClass.getLocTouched() + ",";
        // nr
        str += javaClass.getNr() + ",";
        // nFix
        str += javaClass.getNFix() + ",";
        // nAuth
        str += javaClass.getNAuth() + ",";
        // locAdded
        str += javaClass.getLocAdded() + ",";
        // maxLocAdded
        str += javaClass.getMaxLocAdded() + ",";
        // averageLocAdded
        str += javaClass.getAvgLocAdded() + ",";
        // churn
        str += javaClass.getChurn() + ",";
        // maxChurn
        str += javaClass.getMaxChurn() + ",";
        // averageChurn
        str += javaClass.getAvgChurn() + ",";
        // bugginess
        str += javaClass.getBuggyness();

        fileWriter.append(str);
        fileWriter.append("\n");
    }

    private void closeFileWrite(FileWriter fileWriter) {
        if (fileWriter == null) return;

        try {
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            Logger.getAnonymousLogger().log(Level.INFO, e.getMessage());
        }
    }
}
 */