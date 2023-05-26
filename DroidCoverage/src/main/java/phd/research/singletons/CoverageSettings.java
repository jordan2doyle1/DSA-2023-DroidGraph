package phd.research.singletons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jordan Doyle
 */

@SuppressWarnings("unused")
public class CoverageSettings {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoverageSettings.class);

    private static CoverageSettings instance = null;

    private boolean loggerActive;

    private CoverageSettings() {
        this.loggerActive = true;
    }

    public static CoverageSettings v() {
        if (instance == null) {
            instance = new CoverageSettings();
        }
        return instance;
    }

    // Unit testing purposes.
    public static void resetDefaults() {
        instance = null;
        LOGGER.info("Traversal settings have been reset to default.");
    }

    @SuppressWarnings("UnusedAssignment")
    public void validate() {
        this.loggerActive = false;
        this.loggerActive = true;
    }
}
