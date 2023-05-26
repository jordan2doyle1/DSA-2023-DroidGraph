package phd.research.main;

import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phd.research.Timer;
import phd.research.core.DroidGraph;
import phd.research.coverage.Coverage;
import phd.research.coverage.MonkeyAllCoverage;
import phd.research.enums.Format;
import phd.research.enums.Statistic;
import phd.research.graph.Composition;
import phd.research.singletons.GraphSettings;
import phd.research.utilities.LogHandler;
import phd.research.utility.Writer;

import java.io.*;

/**
 * @author Jordan Doyle
 */

public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        Options options = new Options();
        options.addOption(Option.builder("a").longOpt("apk-file").required().hasArg().numberOfArgs(1).argName("FILE")
                .desc("The APK file to analyse.").build());
        options.addOption(Option.builder("i").longOpt("import-CG").required().hasArg().numberOfArgs(1).argName("FILE")
                .desc("Import AndroGuard call graph from the given file.").build());
        options.addOption(Option.builder("l").longOpt("load-CFG").hasArg().numberOfArgs(1).argName("FILE")
                .desc("Load the control flow graph from the given file.").build());
        options.addOption(Option.builder("p").longOpt("android-platform").hasArg().numberOfArgs(1).argName("DIRECTORY")
                .desc("The Android SDK platform directory.").build());
        options.addOption(Option.builder("f").longOpt("output-format").hasArg().numberOfArgs(1).argName("FORMAT")
                .desc("The graph output format ('DOT','JSON', GML, 'ALL').").build());
        options.addOption(Option.builder("o").longOpt("output-directory").hasArg().numberOfArgs(1).argName("DIRECTORY")
                .desc("The directory for storing output files.").build());
        options.addOption(Option.builder("z").longOpt("serialized-callbacks").hasArg().numberOfArgs(1).argName("FILE")
                .desc("The FlowDroid serialized callbacks file.").build());
        options.addOption(Option.builder("c").longOpt("clean-directory").desc("Clean output directory.").build());
        options.addOption(Option.builder("h").longOpt("help").desc("Display help.").build());

        options.addOption(Option.builder("t").longOpt("traversal").hasArg().numberOfArgs(1).argName("DIRECTORY")
                .desc("Directory containing traversal test output files.").build());
        options.addOption(Option.builder("ma").longOpt("monkey-all").hasArg().numberOfArgs(1).argName("DIRECTORY")
                .desc("Directory containing monkey all interaction test output files.").build());
        options.addOption(Option.builder("mc").longOpt("monkey-click").hasArg().numberOfArgs(1).argName("DIRECTORY")
                .desc("Directory containing monkey click interaction test output files.").build());

        CommandLine cmd = null;
        try {
            CommandLineParser parser = new DefaultParser();
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            final PrintWriter writer = new PrintWriter(System.out);
            formatter.printUsage(writer, 80, "Droid Coverage", options);
            writer.flush();
            System.exit(10);
        }

        if (cmd.hasOption("h")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Droid Coverage", options);
            System.exit(0);
        }

        GraphSettings settings = GraphSettings.v();
        try {
            settings.setApkFile(new File(cmd.getOptionValue("a")));
            settings.setCallGraphFile(new File(cmd.getOptionValue("i")));
        } catch (IOException e) {
            LOGGER.error("Files missing: " + e.getMessage());
            System.exit(20);
        }

        if (cmd.hasOption("p")) {
            try {
                settings.setPlatformDirectory(new File(cmd.getOptionValue("p")));
            } catch (IOException e) {
                LOGGER.error("Files missing: " + e.getMessage());
                System.exit(30);
            }
        }

        if (cmd.hasOption("f")) {
            settings.setFormat(Format.valueOf(cmd.getOptionValue("f")));
        }

        if (cmd.hasOption("o")) {
            try {
                settings.setOutputDirectory(new File(cmd.getOptionValue("o")));
            } catch (IOException e) {
                LOGGER.error("Files missing: " + e.getMessage());
                System.exit(40);
            }
        }

        if (cmd.hasOption("z")) {
            File serializedCallbacks = new File(cmd.getOptionValue("z"));
            if (serializedCallbacks.isFile()) {
                settings.setFlowDroidCallbacksFile(new File(cmd.getOptionValue("z")));
            } else {
                LOGGER.error("Files missing: FlowDroid serialized callbacks file does not exist or is not a file (" +
                        serializedCallbacks + ").");
                System.exit(60);
            }
        }

        if (cmd.hasOption("l")) {
            try {
                settings.setImportControlFlowGraph(new File(cmd.getOptionValue("l")));
            } catch (IOException e) {
                LOGGER.error("Files missing: " + e.getMessage());
                System.exit(70);
            }
        }

        try {
            settings.validate();
        } catch (IOException e) {
            LOGGER.error("Files missing: " + e.getMessage());
            System.exit(50);
        }

        if (cmd.hasOption("c")) {
            try {
                FileUtils.cleanDirectory(settings.getOutputDirectory());
            } catch (IOException e) {
                LOGGER.error("Failed to clean output directory." + e.getMessage());
            }
        }

        File traversalDirectory = null;
        File traversalLog = null;
        if (cmd.hasOption("t")) {
            traversalDirectory = new File(cmd.getOptionValue("t"));
            if (!traversalDirectory.isDirectory()) {
                LOGGER.error("Provided directory (" + traversalDirectory.getAbsolutePath() + ") does not exist.");
                System.exit(80);
            }

            traversalLog = new File(traversalDirectory + File.separator + "traversal.log");
            if (!traversalLog.isFile()) {
                LOGGER.error("Test directory (" + traversalDirectory.getAbsolutePath() + ") does not contain log.");
                System.exit(90);
            }
        }

        File monkeyAllDirectory = null;
        if (cmd.hasOption("ma")) {
            monkeyAllDirectory = new File(cmd.getOptionValue("ma"));
            if (!monkeyAllDirectory.isDirectory()) {
                LOGGER.error("Provided directory (" + monkeyAllDirectory.getAbsolutePath() + ") does not exist.");
                System.exit(80);
            }

            try {
                if (!Coverage.verifyTestsExist(monkeyAllDirectory)) {
                    LOGGER.error("Some of the Monkey test files are missing in " + monkeyAllDirectory);
                    System.exit(90);
                }
            } catch (IOException e) {
                LOGGER.error("Problem verifying Monkey test files exist. " + e.getMessage());
                System.exit(100);
            }
        }


        File monkeyClickDirectory = null;
        if (cmd.hasOption("mc")) {
            monkeyClickDirectory = new File(cmd.getOptionValue("mc"));
            if (!monkeyClickDirectory.isDirectory()) {
                LOGGER.error("Provided directory (" + monkeyClickDirectory.getAbsolutePath() + ") does not exist.");
                System.exit(80);
            }

            try {
                if (!Coverage.verifyTestsExist(monkeyClickDirectory)) {
                    LOGGER.error("Some of the Monkey test files are missing in " + monkeyClickDirectory);
                    System.exit(90);
                }
            } catch (IOException e) {
                LOGGER.error("Problem verifying Monkey test files exist. " + e.getMessage());
                System.exit(100);
            }
        }

        Timer timer = new Timer();
        LOGGER.info("Start time: " + timer.start());

        Timer cTimer = new Timer();
        LOGGER.info("Creating Monkey Traversal Logs... (" + cTimer.start(true) + ")");

        if (monkeyAllDirectory != null) {
            createMonkeyLogs(monkeyAllDirectory);
        }

        if (monkeyClickDirectory != null) {
            createMonkeyLogs(monkeyClickDirectory);
        }

        LOGGER.info("(" + cTimer.end() + ") Creating logs took " + cTimer.secondsDuration() + " second(s).");

        LOGGER.info("Updating DroidGraph... (" + cTimer.start(true) + ")");
        DroidGraph droidGraph = new DroidGraph();

        Composition previousComposition = new Composition(droidGraph.getControlFlowGraph());

        if (traversalDirectory != null) {
            LOGGER.info("Updating graph with log: " + traversalLog);
            updateGraphWithLog(traversalLog, droidGraph);
        }

        if (monkeyAllDirectory != null) {
            updateGraphWithMonkeyLogs(monkeyAllDirectory, droidGraph);
        }

        if (monkeyClickDirectory != null) {
            updateGraphWithMonkeyLogs(monkeyClickDirectory, droidGraph);
        }

        try {
            droidGraph.outputCFGDetails();
            droidGraph.writeControlFlowGraphToFile();
        } catch (IOException e) {
            LOGGER.error("Failed to output the control flow graph. " + e.getMessage());
        }

        LOGGER.info("(" + cTimer.end() + ") DroidGraph update took " + cTimer.secondsDuration() + " second(s).");

        Composition newComposition = new Composition(droidGraph.getControlFlowGraph());
        int vertexCount = newComposition.getVertex() - previousComposition.getVertex();
        int controlCount = newComposition.getControl();
        int edgeCount = newComposition.getEdge() - previousComposition.getEdge();
        int listenerCount = newComposition.getListener();

        File tableFile = new File(System.getProperty("user.dir") + File.separator + "model_enhancement.txt");

        try {
            tableFile.createNewFile();
            FileWriter fw = new FileWriter(tableFile, true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(settings.getApkFile().getName() + " & " + vertexCount + " & " + controlCount + " & " + edgeCount + " & " + listenerCount);
            bw.newLine();
            bw.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        LOGGER.info("Calculating Coverage ... (" + cTimer.start(true) + ")");

        if (traversalDirectory != null) {
            Coverage coverage = new Coverage();
            try {
                coverage.addTestCoverage(0, traversalLog, droidGraph);
            } catch (IOException e) {
                LOGGER.error("Failed to read traversal log: " + e.getMessage());
            }

            try {
                File rawResultsFile = new File(traversalDirectory + File.separator + "results_raw.csv");
                coverage.outputRawCoverageResults(0, rawResultsFile);
                File readableResultsFile = new File(traversalDirectory + File.separator + "results_readable.txt");
                coverage.outputReadableCoverageResults(0, readableResultsFile);
                File visitStatusFile = new File(traversalDirectory + File.separator + "vertex_visit_status.txt");
                droidGraph.outputVertexVisitStatus(visitStatusFile);
                droidGraph.resetVisits();
            } catch (IOException e) {
                LOGGER.error("Failed to output coverage results. " + e.getMessage());
            }

            LOGGER.info("Traversal Coverage Results: " + coverage.getResultsOverview());
        }

        if (monkeyAllDirectory != null) {
            calculateMonkeyCoverage(monkeyAllDirectory, droidGraph, true);
        }

        if (monkeyClickDirectory != null) {
            calculateMonkeyCoverage(monkeyClickDirectory, droidGraph, false);
        }

        LOGGER.info("(" + cTimer.end() + ") Coverage calculation took " + cTimer.secondsDuration() + " second(s).");

        LOGGER.info("End time: " + timer.end());
        LOGGER.info("Execution time: " + timer.secondsDuration() + " second(s).");
    }

    private static void createMonkeyLogs(File monkeyDirectory) {
        File[] testDirectories = monkeyDirectory.listFiles();
        if (testDirectories == null) {
            LOGGER.error("Problem listing subdirectories of " + monkeyDirectory + ".");
            return;
        }

        for (File directory : testDirectories) {
            if (directory.isDirectory()) {
                int testNumber = Coverage.getTestNumber(directory.getName());
                File log = new File(directory + File.separator + "logcat_" + testNumber + ".log");

                try {
                    if (!Coverage.verifyInteractionCount(log)) {
                        LOGGER.warn("Monkey test did not complete all interactions: " + log);
                    }
                } catch (IOException e) {
                    LOGGER.error("Failed to verify interaction count for log file: " + e.getMessage());
                }

                StringBuilder builder = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new FileReader(log))) {
                    int count = 0;
                    String line = reader.readLine();
                    if (line != null) {
                        do {
                            if (line.contains(Coverage.M_SWITCH)) {
                                builder.append(LogHandler.I_TAG).append(" Count: ").append(count).append(", Info: ")
                                        .append(Coverage.M_SWITCH).append(".\n");
                                count++;
                            } else if (line.contains(Coverage.M_TOUCH)) {
                                builder.append(LogHandler.I_TAG).append(" Count: ").append(count).append(", Info: ")
                                        .append(Coverage.M_TOUCH).append(".\n");
                                count++;
                            } else if (line.contains(LogHandler.M_TAG) || line.contains(LogHandler.C_TAG)) {
                                builder.append(line).append("\n");
                            }
                        } while ((line = reader.readLine()) != null);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                try {
                    Writer.writeString(directory, "traversal_" + testNumber + ".log", builder.toString());
                } catch (IOException e) {
                    LOGGER.error("Failed to write traversal log to file. " + e.getMessage());
                }
            }
        }
    }

    private static void updateGraphWithLog(File log, DroidGraph droidGraph) {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(log))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.contains(LogHandler.M_TAG) || line.contains(LogHandler.C_TAG)) {
                    LogHandler.updateGraph(line, droidGraph);
                }
                if (line.contains(LogHandler.C_TAG)) {
                    LogHandler.updateGraphEdges(line, droidGraph);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to read traversal log: " + e.getMessage());
        }
    }

    private static void updateGraphWithMonkeyLogs(File monkeyDirectory, DroidGraph droidGraph) {
        File[] testDirectories = monkeyDirectory.listFiles();
        if (testDirectories == null) {
            LOGGER.error("Problem listing subdirectories of " + monkeyDirectory + ".");
            return;
        }

        for (File directory : testDirectories) {
            if (directory.isDirectory()) {
                int testNumber = Coverage.getTestNumber(directory.getName());
                File log = new File(directory + File.separator + "traversal_" + testNumber + ".log");
                LOGGER.info("Updating graph with monkey log: " + log);
                updateGraphWithLog(log, droidGraph);
            }
        }
    }

    private static void calculateMonkeyCoverage(File monkeyDirectory, DroidGraph droidGraph, boolean all) {
        File[] testDirectories = monkeyDirectory.listFiles();
        if (testDirectories == null) {
            LOGGER.error("Problem listing subdirectories of " + monkeyDirectory + ".");
            return;
        }

        Coverage coverage = all ? new MonkeyAllCoverage() : new Coverage();

        for (File directory : testDirectories) {
            if (directory.isDirectory()) {
                int test = Coverage.getTestNumber(directory.getName());
                File log = new File(directory + File.separator + "traversal_" + test + ".log");

                try {
                    coverage.addTestCoverage(test, log, droidGraph);
                    File readableFile = new File(directory + File.separator + "results_readable_" + test + ".txt");
                    coverage.outputReadableCoverageResults(test, readableFile);
                    File rawFile = new File(directory + File.separator + "results_raw_" + test + ".csv");
                    coverage.outputRawCoverageResults(test, rawFile);
                    File statusFile = new File(directory + File.separator + "vertex_visit_status_" + test + ".txt");
                    droidGraph.outputVertexVisitStatus(statusFile);
                    droidGraph.resetVisits();
                } catch (IOException e) {
                    LOGGER.error("Problem adding test coverage from log file " + log + ". " + e.getMessage());
                }
            }
        }

        try {
            File averageReadableFile = new File(monkeyDirectory + File.separator + "average_results_readable.txt");
            coverage.outputReadableCoverageResults(Statistic.AVERAGE, averageReadableFile);
            File averageRawFile = new File(monkeyDirectory + File.separator + "average_results_raw.csv");
            coverage.outputRawCoverageResults(Statistic.AVERAGE, averageRawFile);

            File minimumReadableFile = new File(monkeyDirectory + File.separator + "minimum_results_readable.txt");
            coverage.outputReadableCoverageResults(Statistic.MINIMUM, minimumReadableFile);
            File minimumRawFile = new File(monkeyDirectory + File.separator + "minimum_results_raw.csv");
            coverage.outputRawCoverageResults(Statistic.MINIMUM, minimumRawFile);

            File maximumReadableFile = new File(monkeyDirectory + File.separator + "maximum_results_readable.txt");
            coverage.outputReadableCoverageResults(Statistic.MAXIMUM, maximumReadableFile);
            File maximumRawFile = new File(monkeyDirectory + File.separator + "maximum_results_raw.csv");
            coverage.outputRawCoverageResults(Statistic.MAXIMUM, maximumRawFile);
        } catch (IOException e) {
            LOGGER.error("Problem while outputting average test coverage. " + e.getMessage());
        }

        String monkeyTestDescription = monkeyDirectory.getName().replace("_", " ");
        LOGGER.info(monkeyTestDescription + " Coverage: " + coverage.getResultsOverview());
    }
}