package Utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileCSVGenerator {
    private static final String RESULT = "results/";
    private static final String MAIN_PATH = "resources/";

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

        try {
            if (!Files.exists(folderPath)) {
                Files.createDirectories(folderPath);
            }
        } catch (IOException ex) {
            Logger.getAnonymousLogger().log(Level.INFO, "Results folders creation error");
        }
    }

    private void writeToFile(FileWriter fileWriter, String content) throws IOException {
        fileWriter.append(content);
        fileWriter.append("\n");
    }

    public void saveRepResults(String type, int runNumber, double responseTime, double avgPopulationInNode, double waitingTime, double avgPopulationInQueue) {
        String fileTitle = Paths.get(directoryPath, FileCSVGenerator.RESULT, "finiteHorizonStats.csv").toString();
        File file = new File(fileTitle);

        try (FileWriter fileWriter = new FileWriter(fileTitle, true)) {
            if (file.length() == 0)
                writeToFile(fileWriter, "Run Index,Center,E[T_S],E[N_S],E[T_Q],E[N_Q]");
            
            if (waitingTime == -Double.MAX_VALUE && avgPopulationInQueue == -Double.MAX_VALUE) {
                writeToFile(fileWriter, runNumber + "," + type + "," + responseTime + "," +
                        avgPopulationInNode + "," + " " + "," + " ");
            } else {
                writeToFile(fileWriter, runNumber + "," + type + "," + responseTime + "," +
                        avgPopulationInNode + "," + waitingTime + "," + avgPopulationInQueue);
            }

        } catch (IOException e) {
            Logger.getAnonymousLogger().log(Level.INFO, "An error occurred while generating release info", e);
        }
    }

    public void saveBatchResults(int batchIndex, double responseTime, String centerName) {
        String fileTitle = Paths.get(directoryPath + FileCSVGenerator.RESULT  + "infiniteHorizonStats" + centerName + ".csv").toString();

        try {
            Files.createDirectories(Paths.get(fileTitle).getParent());
        } catch (IOException e) {
            Logger.getAnonymousLogger().log(Level.INFO, e.getMessage());

            return;
        }

        File file = new File(fileTitle);

        try (FileWriter fileWriter = new FileWriter(fileTitle, true)) {
            if (file.length() == 0) {
                writeToFile(fileWriter, "Batch Number,E[T_S]");
                writeToFile(fileWriter, batchIndex + "," + 0);
            }

            writeToFile(fileWriter, (batchIndex + 1) + "," + responseTime);

        } catch (IOException e) {
            Logger.getAnonymousLogger().log(Level.INFO, "An error occurred while generating release info", e);
        }
    }

    public static void writeRepData(Boolean isFinite, long seed, int event, int runNumber, double time, double responseTime, double avgPopulationInNode, double waitingTime, double avgPopulationInQueue) {
        String center = switch (event) {
            case 0 -> "Noleggio";
            case 1 -> "Ricarica";
            case 2 -> "Parcheggio";
            case 3 -> "Strada";
            default -> null;
        };

        if (center == null) return;

        File file = new File(MAIN_PATH + RESULT + ((isFinite) ? "finite" : "infinite") + center + ".csv");

        try {
            boolean isCreated = false;
            if (file.createNewFile()) {
                System.out.println("File created: " + file.getAbsolutePath());
                isCreated = true;
            }

            // Now you can open the file for writing or reading
            FileWriter writer = new FileWriter(file, true); // 'true' for append mode

            if(isCreated)
                writer.write( "Run Index,Seed,Center,Time,E[T_S],E[N_S],E[T_Q],E[N_Q]\n");

            writer.write(runNumber + "," + seed + "," + center + "," + time + "," + responseTime + "," +
                    avgPopulationInNode + "," + waitingTime + "," + avgPopulationInQueue + "\n");

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeStradaArrival(Boolean isFinite, long seed, int event, double time) {
        String center = switch (event) {
            case 0 -> "Noleggio";
            case 1 -> "Ricarica";
            case 2 -> "Parcheggio";
            case 3 -> "Strada";
            default -> null;
        };

        if (center == null) return;

        File file = new File(MAIN_PATH + RESULT + ((isFinite) ? "finite" : "infinite") + center + "Lambda.csv");

        try {
            boolean isCreated = false;
            if (file.createNewFile()) {
                System.out.println("File created: " + file.getAbsolutePath());
                isCreated = true;
            }

            // Now you can open the file for writing or reading
            FileWriter writer = new FileWriter(file, true); // 'true' for append mode

            if(isCreated)
                writer.write( "Seed,Center,Time\n");

            writer.write(seed + "," + center + "," + time +  "\n");

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeTimeCars(Boolean isFinite, long seed, String center, double completionTime, double takenTime) {
        File file = new File(MAIN_PATH + RESULT + ((isFinite) ? "finite" : "infinite") + "CarMu.csv");

        try {
            boolean isCreated = false;
            if (file.createNewFile()) {
                System.out.println("File created: " + file.getAbsolutePath());
                isCreated = true;
            }

            // Now you can open the file for writing or reading
            FileWriter writer = new FileWriter(file, true); // 'true' for append mode

            if(isCreated)
                writer.write( "Seed,Center,Completion Time,Taken Time\n");

            writer.write(seed + "," + center + "," + completionTime + "," + takenTime + "\n");

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void deleteFolder(String stringPath) {
        Path folderPath = Paths.get(stringPath);

        try {
            Files.walk(folderPath)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            System.out.println("No folder to delete\n");
        }
    }
}
