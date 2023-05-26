package phd.research.appium;

import io.appium.java_client.android.AndroidElement;
import org.openqa.selenium.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phd.research.Pair;
import phd.research.Timer;
import phd.research.singletons.TraversalSettings;
import phd.research.startup.AppLaunch;
import phd.research.startup.TimetableLaunch;
import phd.research.startup.VolumeControlLaunch;
import phd.research.utilities.LogHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Jordan Doyle
 */

public class Traversal {

    private static final Logger LOGGER = LoggerFactory.getLogger(Traversal.class);

    private final AppiumManager appiumManager;
    private final Map<String, Map<Point, Pair<Integer, Boolean>>> controls;

    private boolean initialLaunch;
    private String launchActivity;
    private String launchPackage;

    public Traversal() {
        this.initialLaunch = true;
        this.appiumManager = new AppiumManager();
        this.controls = new HashMap<>();
    }

    public static void waitForUi(float seconds) {
        try {
            long milliseconds = (long) seconds * 1000;
            LOGGER.info("Waiting " + seconds + " seconds for UI.");
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void launchApp() {
        if (initialLaunch) {
            this.logInteraction("launching app");
            try {
                this.appiumManager.launchApp();
            } catch (Exception e) {
                LOGGER.error("ERROR");
            }
            this.initialLaunch = false;

            this.launchActivity = this.appiumManager.getCurrentActivity();
            this.launchPackage = this.appiumManager.getCurrentPackage();
        } else {
            this.logInteraction("relaunching app");
            this.appiumManager.launchApp();
        }

        AppLaunch appLaunch;
        LOGGER.info("Package: " + this.launchPackage);
        switch (this.launchPackage) {
            //noinspection SpellCheckingInspection
            case "com.punksta.apps.volumecontrol":
                appLaunch = new VolumeControlLaunch();
                break;
            //noinspection SpellCheckingInspection
            case "com.asdoi.timetable":
                appLaunch = new TimetableLaunch();
                break;
            default:
                appLaunch = null;
                break;
        }

        if (appLaunch != null) {
            LOGGER.info("Running launch commands for package " + this.launchPackage);
            appLaunch.launch(this.appiumManager.getDriver());
        }
    }

    public void traverseUi() {
        Timer timer = new Timer();
        LOGGER.info("Running Traversal... (" + timer.start(true) + ")");

        this.appiumManager.startAppiumLogcatBroadcast();
        this.launchApp();

        do {
            waitForUi(0.5f);

            String currentActivity = this.appiumManager.getCurrentActivity();
            LOGGER.info("Current Activity: " + currentActivity);

            if (!this.appiumManager.getCurrentPackage().startsWith(this.launchPackage)) {
                this.launchApp();
            }

            List<AndroidElement> availableControls = this.appiumManager.getClickElementsOnScreen();
            AndroidElement nextControl = this.chooseNextControl(currentActivity, availableControls);
            this.clickControl(currentActivity, nextControl);
        } while (continueTraversal());

        this.appiumManager.cleanup();

        if (Tracker.interactionCount < Tracker.maxInteractionCount) {
            LOGGER.error("Traversal only completed " + Tracker.interactionCount + " interactions.");
        }

        LOGGER.info("(" + timer.end() + " Appium traversal took " + timer.secondsDuration() + " second(s).");
    }

    private void logInteraction(String message) {
        Tracker.interactionCount++;
        LOGGER.info("Interaction " + Tracker.interactionCount + " " + message + ".");
        LogHandler.handleMessage(LogHandler.I_TAG + " Count: " + Tracker.interactionCount + ", Info: " + message + ".");
    }

    private AndroidElement chooseNextControl(String activity, List<AndroidElement> controls) {
        if (controls.isEmpty()) {
            LOGGER.info("No controls found, recommend pressing back button.");
            return null;
        }

        Map<Point, Pair<Integer, Boolean>> activityControls = this.controls.getOrDefault(activity, new HashMap<>());

        AndroidElement nextControl = controls.get(0);
        if (!activityControls.containsKey(nextControl.getCenter())) {
            activityControls.put(nextControl.getCenter(), new Pair<>(0, false));
            LOGGER.info("Recommend '" + AppiumManager.getElementDescription(nextControl) + "' for next interaction.");
            this.controls.put(activity, activityControls);
            return nextControl;
        }

        for (AndroidElement control : controls) {
            if (activityControls.containsKey(control.getCenter())) {
                if (activityControls.get(control.getCenter()).getRight() &&
                        activityControls.get(control.getCenter()).getLeft() > 0) {
                    continue;
                }

                if (activityControls.get(control.getCenter()).getLeft() <
                        activityControls.get(nextControl.getCenter()).getLeft()) {
                    nextControl = control;
                }
            } else {
                activityControls.put(control.getCenter(), new Pair<>(0, false));
                this.controls.put(activity, activityControls);
                return control;
            }
        }

        this.controls.put(activity, activityControls);
        LOGGER.info("Recommend '" + AppiumManager.getElementDescription(nextControl) + "' for next interaction.");
        return nextControl;
    }

    private void clickControl(String activity, AndroidElement control) {
        if (control == null && activity.equals(this.launchActivity)) {
            LOGGER.warn("No control elements on the launch activity.");
            return;
        } else if (control == null) {
            Tracker.interactionCount++;
            LOGGER.info("Interaction " + Tracker.interactionCount + " pressing back");
            LogHandler.handleMessage(
                    LogHandler.I_TAG + " Count: " + Tracker.interactionCount + ", Info: pressing " + "back.");
            this.appiumManager.navigateBack();
            return;
        }

        String id = control.getAttribute("resourceId");
        if (id == null) {
            id = control.getCenter().toString().replace("(", "").replace(")", "").replace(", ", "");
            LOGGER.error("Control element has no resource id, using " + id + " instead.");
        } else {
            id = id.split("/")[1];
        }

        String controlClass = control.getAttribute("class");
        String activityBeforeClick = this.appiumManager.getCurrentActivity();
        Point controlCentrePoint = control.getCenter();

        Tracker.interactionCount++;
        LOGGER.info("Interaction " + Tracker.interactionCount + " clicking " +
                AppiumManager.getElementDescription(control) + ".");
        LogHandler.handleMessage(
                LogHandler.I_TAG + " Count: " + Tracker.interactionCount + ", Id: " + id + ", Info: clicking " +
                        AppiumManager.getElementDescription(control) + ".");
        control.click();

        if (controlClass.contains("EditText")) {
            Tracker.interactionCount++;
            LOGGER.info("Interaction " + Tracker.interactionCount + " hiding keyboard.");
            LogHandler.handleMessage(
                    LogHandler.I_TAG + " Count: " + Tracker.interactionCount + ", Info: hiding keyboard.");
            this.appiumManager.hideKeyboard();
        }

        boolean leaf = this.appiumManager.getCurrentActivity().equals(activityBeforeClick);
        if (!this.appiumManager.getCurrentPackage().startsWith(this.launchPackage)) {
            Tracker.interactionCount++;
            LOGGER.info("No longer in the test app... should press back to return.");
            LOGGER.info("Interaction " + Tracker.interactionCount + " pressing back.");
            LogHandler.handleMessage(
                    LogHandler.I_TAG + " Count: " + Tracker.interactionCount + ", Info: pressing " + "back.");
            this.appiumManager.navigateBack();
            leaf = true;
        }

        Map<Point, Pair<Integer, Boolean>> activityControls = this.controls.getOrDefault(activity, new HashMap<>());
        Pair<Integer, Boolean> controlStats = activityControls.getOrDefault(controlCentrePoint, new Pair<>(0, false));
        Pair<Integer, Boolean> updatedStats = new Pair<>(controlStats.getLeft() + 1, (controlStats.getRight() || leaf));
        activityControls.put(controlCentrePoint, updatedStats);
        this.controls.put(activity, activityControls);
    }

    private boolean continueTraversal() {
        LOGGER.info("Checking for stopping conditions.");

        if (Tracker.lastInteractionCount == Tracker.interactionCount) {
            Tracker.noInteractionCount++;
        } else {
            Tracker.lastInteractionCount = Tracker.interactionCount;
            Tracker.noInteractionCount = 0;
        }

        if (Tracker.noInteractionCount == Tracker.maxNoInteractionCount) {
            LOGGER.info("Infinite loop detected, " + Tracker.noInteractionCount + " iterations without interaction, " +
                    "stopping traversal.");
            return false;
        }

        if (Tracker.interactionCount >= Tracker.maxInteractionCount) {
            LOGGER.info("Test reached maximum interaction count of " + Tracker.interactionCount + " interactions, " +
                    "stopping traversal.");
            return false;
        }

        return true;
    }

    private static class Tracker {
        public static final int maxInteractionCount = TraversalSettings.v().getMaxInteractionCount();
        public static final int maxNoInteractionCount = TraversalSettings.v().getMaxNoInteractionCount();

        public static int lastInteractionCount = 0;
        public static int interactionCount = 0;
        public static int noInteractionCount = 0;

        @SuppressWarnings("unused")
        public static void reset() {
            Tracker.lastInteractionCount = 0;
            Tracker.interactionCount = 0;
            Tracker.noInteractionCount = 0;
        }
    }
}
