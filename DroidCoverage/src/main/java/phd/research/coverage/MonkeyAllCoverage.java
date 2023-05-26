package phd.research.coverage;

import phd.research.Pair;
import phd.research.core.DroidGraph;
import phd.research.utilities.LogHandler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Jordan Doyle
 */

public class MonkeyAllCoverage extends Coverage {

    public MonkeyAllCoverage() {
        super();
    }

    @Override
    public void addTestCoverage(int testNumber, File log, DroidGraph droidGraph) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(log));
        Map<Integer, Pair<Float, Float>> testCoverage = new HashMap<>();

        String line = reader.readLine();
        if (line != null) {
            do {
                if (line.contains(LogHandler.M_TAG) || line.contains(LogHandler.C_TAG)) {
                    LogHandler.handleInstrumentLog(line, droidGraph);
                }
            } while ((line = reader.readLine()) != null);
        }

        Pair<Float, Float> finalCoverage = droidGraph.calculateCoverage();
        testCoverage.put(0, finalCoverage);
        super.testResults.put(testNumber, testCoverage);
    }
}
