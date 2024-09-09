package Utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileCSVGenerator {
    private static final String RESULT = "results/";
    private static final String MAIN_PATH = "resources/";
    private static final String FINITE_HORIZON = "finite_horizon/";
    private static final String INFINITE_HORIZON = "infinite_horizon/";

    private static String directoryPath;
    private static FileCSVGenerator instance;

    public FileCSVGenerator(String directoryPath) {
        FileCSVGenerator.directoryPath = directoryPath;

        createDirectories();
    }

    public static FileCSVGenerator getInstance() {
        if (instance == null) {
            instance = new FileCSVGenerator(FileCSVGenerator.MAIN_PATH);
        }
        return instance;
    }

    private void createDirectories() {
        Path folderPath = Paths.get(directoryPath, FileCSVGenerator.RESULT);
        Path finitePath = Paths.get(directoryPath, FileCSVGenerator.RESULT, FileCSVGenerator.FINITE_HORIZON);
        Path infinitePath = Paths.get(directoryPath, FileCSVGenerator.RESULT, FileCSVGenerator.INFINITE_HORIZON);

        try {
                if (!Files.exists(folderPath)) {
                    Files.createDirectories(folderPath);
                }

                if (!Files.exists(finitePath)) {
                    Files.createDirectories(finitePath);
                }

                if (!Files.exists(infinitePath)) {
                    Files.createDirectories(infinitePath);
                }
            } catch (IOException ex) {
                Logger.getAnonymousLogger().log(Level.INFO, "Results folders creation error");
            }
    }

    public void createSeedFolders(long seed) {
        String folderName = "Seed_" + seed + File.separator;
        Path folderPath = Paths.get(directoryPath + FileCSVGenerator.RESULT + FileCSVGenerator.FINITE_HORIZON + folderName);
        try {
            if (!Files.exists(folderPath)) {
                Files.createDirectories(folderPath);
            }
        } catch (IOException ex) {
            Logger.getAnonymousLogger().log(Level.INFO, "Seed folders creation error");
        }
    }

    public void deleteSeedDirectory() {
        Path folderPath = Paths.get(directoryPath + FileCSVGenerator.RESULT);

        try {
            Files.walkFileTree(folderPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ex) {
            // ignore: nothing to delete
        }
    }

    private void writeToFile(FileWriter fileWriter, String content) throws IOException {
        fileWriter.append(content);
        fileWriter.append("\n");
    }

    public void saveRepResults(String type, int runNumber, long seed, double responseTime, double avgPopulationInNode, double waitingTime, double avgPopulationInQueue) {
        String fileTitle = Paths.get(directoryPath, FileCSVGenerator.RESULT, FileCSVGenerator.FINITE_HORIZON, searchSeedFileName(seed), "run_" + runNumber + ".csv").toString();
        File file = new File(fileTitle);

        try (FileWriter fileWriter = new FileWriter(fileTitle, true)) {
            if (file.length() == 0)
                writeToFile(fileWriter, "Center,E[T_s],E[N_s],E[T_q],E[N_q]");
            
            if (waitingTime == -Double.MAX_VALUE && avgPopulationInQueue == -Double.MAX_VALUE) {
                writeToFile(fileWriter, type + "," + responseTime + "," +
                        avgPopulationInNode + "," + " " + "," + " ");
            } else {
                writeToFile(fileWriter, type + "," + responseTime + "," +
                        avgPopulationInNode + "," + waitingTime + "," + avgPopulationInQueue);
            }

        } catch (IOException e) {
            Logger.getAnonymousLogger().log(Level.INFO, "An error occurred while generating release info", e);
        }
    }

    public void saveBatchResults(int batchIndex, double responseTime) {
        String fileTitle = Paths.get(directoryPath + FileCSVGenerator.RESULT + FileCSVGenerator.INFINITE_HORIZON + "waitingTime.csv").toString();

        try {
            Files.createDirectories(Paths.get(fileTitle).getParent());
        } catch (IOException e) {
            Logger.getAnonymousLogger().log(Level.INFO, e.getMessage());

            return;
        }

        File file = new File(fileTitle);

        try (FileWriter fileWriter = new FileWriter(fileTitle, true)) {
            if (file.length() == 0)
                writeToFile(fileWriter, "Batch Number,E[T_Q]");

            writeToFile(fileWriter, batchIndex + "," + responseTime);

        } catch (IOException e) {
            Logger.getAnonymousLogger().log(Level.INFO, "An error occurred while generating release info", e);
        }
    }

    private String searchSeedFileName(long seed) {
        Path folderPath = Paths.get(directoryPath + FileCSVGenerator.RESULT);
        String[] fileName = {""};

        try {
            Files.walkFileTree(folderPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (dir.getFileName().toString().contains("Seed_" + seed)) {
                        fileName[0] = dir.getFileName().toString();
                        return FileVisitResult.TERMINATE;
                    } else return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            System.err.println("Errore durante la ricerca: " + e.getMessage());
        }
        return fileName[0];
    }
}
