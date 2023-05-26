package phd.research.main;

import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phd.research.Timer;
import phd.research.appium.Traversal;
import phd.research.singletons.TraversalSettings;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author Jordan Doyle
 */

public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        Options options = new Options();
        options.addOption(Option.builder("a").longOpt("apk-file").required().hasArg().numberOfArgs(1).argName("FILE")
                .desc("The APK file to analyse.").build());
        options.addOption(Option.builder("o").longOpt("output-directory").hasArg().numberOfArgs(1).argName("DIRECTORY")
                .desc("The directory for storing output files.").build());
        options.addOption(Option.builder("m").longOpt("max-interactions").hasArg().numberOfArgs(1).argName("INT")
                .desc("The number of interactions to perform on the app.").build());
        options.addOption(Option.builder("n").longOpt("max-no-interactions").hasArg().numberOfArgs(1).argName("INT")
                .desc("The number of iterations without an interaction before exiting loop.").build());
        options.addOption(Option.builder("c").longOpt("clean-directory").desc("Clean output directory.").build());
        options.addOption(Option.builder("h").longOpt("help").desc("Display help.").build());

        CommandLine cmd = null;
        try {
            CommandLineParser parser = new DefaultParser();
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            final PrintWriter writer = new PrintWriter(System.out);
            formatter.printUsage(writer, 80, "Traversal Coverage", options);
            writer.flush();
            System.exit(10);
        }

        if (cmd.hasOption("h")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Traversal Coverage", options);
            System.exit(0);
        }

        TraversalSettings settings = TraversalSettings.v();
        try {
            settings.setApkFile(new File(cmd.getOptionValue("a")));
        } catch (IOException e) {
            LOGGER.error("Files missing: " + e.getMessage());
            System.exit(20);
        }

        if (cmd.hasOption("o")) {
            try {
                settings.setOutputDirectory(new File(cmd.getOptionValue("o")));
            } catch (IOException e) {
                LOGGER.error("Files missing: " + e.getMessage());
                System.exit(30);
            }
        }

        if (cmd.hasOption("m")) {
            settings.setMaxInteractionCount(Integer.parseInt(cmd.getOptionValue("m")));
        }

        if (cmd.hasOption("n")) {
            settings.setMaxNoInteractionCount(Integer.parseInt(cmd.getOptionValue("n")));
        }

        try {
            settings.validate();
        } catch (IOException e) {
            LOGGER.error("Files missing: " + e.getMessage());
            System.exit(40);
        }

        if (cmd.hasOption("c")) {
            try {
                FileUtils.cleanDirectory(settings.getOutputDirectory());
            } catch (IOException e) {
                LOGGER.error("Failed to clean output directory." + e.getMessage());
            }
        }

        Timer timer = new Timer();
        LOGGER.info("Start time: " + timer.start());

        Traversal traversal = new Traversal();
        traversal.traverseUi();

        LOGGER.info("End time: " + timer.end());
        LOGGER.info("Execution time: " + timer.secondsDuration() + " second(s).");
    }
}