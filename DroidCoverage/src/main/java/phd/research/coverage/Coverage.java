package phd.research.coverage;

import phd.research.Pair;
import phd.research.StringTable;
import phd.research.core.DroidGraph;
import phd.research.enums.Statistic;
import phd.research.utilities.LogHandler;
import phd.research.utility.Writer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Jordan Doyle
 */

public class Coverage {

    public static final String M_SWITCH = "Monkey  : :Switch";
    public static final String M_TOUCH = "Monkey  : :Sending Touch";

    protected final Map<Integer, Map<Integer, Pair<Float, Float>>> testResults;

    public Coverage() {
        super();
        this.testResults = new HashMap<>();
    }

    public static boolean verifyInteractionCount(File log) throws IOException {
        String eventCountString = "Events injected: 500";
        String monkeyFinishedString = "// Monkey finished";

        boolean eventCountVerified = false;
        boolean monkeyFinished = false;

        BufferedReader reader = new BufferedReader(new FileReader(log));

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.contains(eventCountString)) {
                eventCountVerified = true;
            } else if (line.contains(monkeyFinishedString)) {
                monkeyFinished = true;
            }

            if (eventCountVerified && monkeyFinished) {
                return true;
            }
        }

        return false;
    }

    public static int getTestNumber(String fileName) {
        int number = -1;

        Pattern pattern = Pattern.compile("\\d+$");
        Matcher matcher = pattern.matcher(fileName);

        if (matcher.find()) {
            String numberString = matcher.group(0);
            number = Integer.parseInt(numberString);
        }

        return number;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean verifyTestsExist(File directory) throws IOException {
        if (!directory.isDirectory()) {
            throw new IOException("Provided file (" + directory.getAbsolutePath() + ") is not a directory.");
        }

        File[] testDirectories = directory.listFiles();
        if (testDirectories == null) {
            throw new IOException("Failed to list subdirectories of " + directory.getAbsolutePath() + ".");
        }

        List<String> testPaths = Arrays.stream(testDirectories).map(File::getPath).collect(Collectors.toList());
        for (int i = 1; i <= 10; i++) {
            if (!testPaths.contains(directory.getPath() + File.separator + "Test_" + i)) {
                return false;
            }
        }

        for (File testDirectory : testDirectories) {
            if (testDirectory.isDirectory() && testDirectory.getName().startsWith("Test_")) {
                int testNumber = Coverage.getTestNumber(testDirectory.getName());
                File logcatFile = new File(testDirectory + File.separator + "logcat_" + testNumber + ".log");
                File monkeyFile = new File(testDirectory + File.separator + "monkey_" + testNumber + ".log");

                if (!logcatFile.isFile() || !monkeyFile.isFile()) {
                    return false;
                }
            }
        }

        return true;
    }

    public void addTestCoverage(int testNumber, File log, DroidGraph droidGraph) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(log));
        Map<Integer, Pair<Float, Float>> testCoverage = new HashMap<>();

        int count = 0;
        String line = reader.readLine();
        if (line != null) {
            do {
                if (line.contains(LogHandler.I_TAG)) {
                    Pair<Float, Float> currentCoverage = droidGraph.calculateCoverage();
                    testCoverage.put(count, currentCoverage);
                    count++;
                } else if (line.contains(LogHandler.M_TAG) || line.contains(LogHandler.C_TAG)) {
                    LogHandler.handleInstrumentLog(line, droidGraph);
                }
            } while ((line = reader.readLine()) != null);
        }

        Pair<Float, Float> finalCoverage = droidGraph.calculateCoverage();
        testCoverage.put(count, finalCoverage);
        this.testResults.put(testNumber, testCoverage);
    }

    public void outputRawCoverageResults(Statistic statistic, File outputFile) throws IOException {
        switch (statistic) {
            case AVERAGE:
                this.outputRawCoverageResults(-1, outputFile);
                break;
            case MAXIMUM:
                this.outputRawCoverageResults(-2, outputFile);
                break;
            case MINIMUM:
                this.outputRawCoverageResults(-3, outputFile);
                break;
        }
    }

    public void outputRawCoverageResults(int testNumber, File outputFile) throws IOException {
        Map<Integer, Pair<Float, Float>> results = this.getTestResults(testNumber);
        this.outputRawCoverageResults(results, outputFile);
    }

    public void outputRawCoverageResults(Map<Integer, Pair<Float, Float>> results, File outputFile) throws IOException {
        DecimalFormat decimalFormat = new DecimalFormat("0.00");

        StringBuilder builder = new StringBuilder("interaction,control,method\n");
        for (Integer key : results.keySet()) {
            builder.append(key).append(",").append(decimalFormat.format(results.get(key).getLeft())).append(",")
                    .append(decimalFormat.format(results.get(key).getRight())).append("\n");
        }

        Writer.writeString(outputFile.getParentFile(), outputFile.getName(), builder.toString());
    }

    public void outputReadableCoverageResults(Statistic statistic, File outputFile) throws IOException {
        switch (statistic) {
            case AVERAGE:
                this.outputReadableCoverageResults(-1, outputFile);
                break;
            case MAXIMUM:
                this.outputReadableCoverageResults(-2, outputFile);
                break;
            case MINIMUM:
                this.outputReadableCoverageResults(-3, outputFile);
                break;
        }
    }

    public void outputReadableCoverageResults(int testNumber, File outputFile) throws IOException {
        Map<Integer, Pair<Float, Float>> results = getTestResults(testNumber);
        this.outputReadableCoverageResults(results, outputFile);
    }

    public void outputReadableCoverageResults(Map<Integer, Pair<Float, Float>> results, File outputFile)
            throws IOException {
        DecimalFormat decimalFormat = new DecimalFormat("0.00");

        String[][] data = new String[results.keySet().size() + 1][];
        data[0] = new String[]{"Interaction", "Control", "Method"};
        for (Integer key : results.keySet()) {
            data[key + 1] = new String[]{String.valueOf(key), decimalFormat.format(results.get(key).getLeft()),
                    decimalFormat.format(results.get(key).getRight())};
        }

        Writer.writeString(outputFile.getParentFile(), outputFile.getName(), StringTable.tableWithLines(data, true));
    }

    public Map<Integer, Pair<Float, Float>> calculateAverageResults() {
        Map<Integer, Pair<Float, Float>> totals = new HashMap<>();
        Map<Integer, Pair<Float, Float>> averages = new HashMap<>();

        int numberOfTests = this.testResults.keySet().size();

        for (Integer testKey : this.testResults.keySet()) {
            Map<Integer, Pair<Float, Float>> results = this.testResults.get(testKey);
            for (Integer resultsKey : results.keySet()) {
                Pair<Float, Float> result = results.get(resultsKey);
                Pair<Float, Float> total = totals.containsKey(resultsKey) ? totals.get(resultsKey) : new Pair<>(0f, 0f);
                Pair<Float, Float> newTotal =
                        new Pair<>(total.getLeft() + result.getLeft(), total.getRight() + result.getRight());
                totals.put(resultsKey, newTotal);
            }
        }

        for (Integer totalsKey : totals.keySet()) {
            Pair<Float, Float> total = totals.get(totalsKey);
            Pair<Float, Float> average = new Pair<>(total.getLeft() / numberOfTests, total.getRight() / numberOfTests);
            averages.put(totalsKey, average);
        }

        return averages;
    }

    public Map<Integer, Pair<Float, Float>> calculateMinimumValues() {
        Map<Integer, Pair<Float, Float>> minimums = new HashMap<>();

        for (Integer testKey : this.testResults.keySet()) {
            Map<Integer, Pair<Float, Float>> results = this.testResults.get(testKey);
            for (Integer resultsKey : results.keySet()) {
                Pair<Float, Float> result = results.get(resultsKey);
                Pair<Float, Float> min = minimums.getOrDefault(resultsKey, result);
                Pair<Float, Float> newMin =
                        new Pair<>(result.getLeft() < min.getLeft() ? result.getLeft() : min.getLeft(),
                                result.getRight() < min.getRight() ? result.getRight() : min.getRight()
                        );
                minimums.put(resultsKey, newMin);
            }
        }

        return minimums;
    }

    public Map<Integer, Pair<Float, Float>> calculateMaximumValues() {
        Map<Integer, Pair<Float, Float>> maximums = new HashMap<>();

        for (Integer testKey : this.testResults.keySet()) {
            Map<Integer, Pair<Float, Float>> results = this.testResults.get(testKey);
            for (Integer resultsKey : results.keySet()) {
                Pair<Float, Float> result = results.get(resultsKey);
                Pair<Float, Float> max = maximums.getOrDefault(resultsKey, result);
                Pair<Float, Float> newMax =
                        new Pair<>(result.getLeft() > max.getLeft() ? result.getLeft() : max.getLeft(),
                                result.getRight() > max.getRight() ? result.getRight() : max.getRight()
                        );
                maximums.put(resultsKey, newMax);
            }
        }

        return maximums;
    }

    public String getResultsOverview() {
        DecimalFormat decimalFormat = new DecimalFormat("0.00");
        Map<Integer, Pair<Float, Float>> averages = this.calculateAverageResults();
        int largestKey = Collections.max(averages.keySet());

        return "Interface " + decimalFormat.format(averages.get(largestKey).getLeft()) + "%, Method " +
                decimalFormat.format(averages.get(largestKey).getRight()) + "%";
    }

    private Map<Integer, Pair<Float, Float>> getTestResults(int testNumber) {
        switch (testNumber) {
            case -1:
                return calculateAverageResults();
            case -2:
                return calculateMaximumValues();
            case -3:
                return calculateMinimumValues();
            default:
                return this.testResults.get(testNumber);
        }
    }
}
