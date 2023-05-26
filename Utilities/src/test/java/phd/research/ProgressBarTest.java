package phd.research;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;

import static org.junit.Assert.assertEquals;

/**
 * @author Jordan Doyle
 */

public class ProgressBarTest {

    private final File outputFile = new File("ProgressBarOutput.txt");

    @Before
    public void setUp() throws FileNotFoundException {
        PrintStream testOutputStream = new PrintStream(this.outputFile);
        System.setOut(testOutputStream);
    }

    @Test
    public void printProgressLineCountTest() {
        ProgressBar.printProgress(3, 47);

        try (BufferedReader reader = new BufferedReader(new FileReader(this.outputFile))) {
            long numberOfLines = 0;
            while (reader.readLine() != null) {
                numberOfLines++;
            }
            assertEquals("Wrong line count in test output file.", 1, numberOfLines);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void printProgressIncompleteTest() {
        ProgressBar.printProgress(54, 69);

        try (BufferedReader reader = new BufferedReader(new FileReader(this.outputFile))) {
            String line = reader.readLine();
            assertEquals("Wrong output in test output file.",
                    "Progress : [#######################################-----------] 54 / 69", line
                        );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void printProgressCompletedTest() {
        ProgressBar.printProgress(306, 306);

        try (BufferedReader reader = new BufferedReader(new FileReader(this.outputFile))) {
            String line = reader.readLine();
            assertEquals("Wrong output in test output file.",
                    "Progress : [###################### DONE ######################] 306 / 306", line
                        );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @After
    public void tearDown() {
        if (!this.outputFile.delete()) {
            System.err.println("Failed to delete test output file.");
        }
    }
}