package phd.research.appium;

import com.google.common.collect.Lists;
import io.appium.java_client.MobileBy;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.remote.AndroidMobileCapabilityType;
import io.appium.java_client.remote.MobileCapabilityType;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServerHasNotBeenStartedLocallyException;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import io.appium.java_client.service.local.flags.ServerArgument;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phd.research.singletons.TraversalSettings;
import phd.research.utilities.LogHandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author Jordan Doyle
 */

public class AppiumManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppiumManager.class);

    private AppiumDriverLocalService service;
    private AndroidDriver<WebElement> driver;

    public AppiumManager() {

    }

    public static String getElementDescription(AndroidElement element) {
        StringBuilder builder = new StringBuilder("[");

        AppiumManager.appendDescription(builder, "name", element.getAttribute("name"));
        AppiumManager.appendDescription(builder, "resource-id", element.getAttribute("resource-id"));
        AppiumManager.appendDescription(builder, "tag", element.getTagName());
        AppiumManager.appendDescription(builder, "text", element.getText());
        AppiumManager.appendDescription(builder, "content-desc", element.getAttribute("content-desc"));
        AppiumManager.appendDescription(builder, "location", element.getLocation().toString());
        AppiumManager.appendDescription(builder, "centre", element.getCenter().toString());
        AppiumManager.appendDescription(builder, "class", element.getAttribute("class"));

        builder.append("]");
        return builder.toString();
    }

    private static void appendDescription(StringBuilder builder, String valueId, String value) {
        if (value != null && !value.isEmpty()) {
            if (builder.charAt(builder.length() - 1) != '[') {
                builder.append(", ");
            }
            builder.append(valueId).append(": ").append(value);
        }
    }

    public void startAppiumService() {
        LOGGER.info("Starting Appium Service...");

        ServerArgument allowInsecureArg = () -> "--allow-insecure";
        AppiumServiceBuilder builder = new AppiumServiceBuilder().withArgument(allowInsecureArg, "adb_shell");

        this.service = AppiumDriverLocalService.buildService(builder);
        this.service.clearOutPutStreams();

        try {
            this.service.addOutPutStream(Files.newOutputStream(Paths.get("target/logs/appium.log")));
        } catch (IOException e) {
            LOGGER.error("Problem adding Appium log file. " + e.getMessage());
        }

        int attempts = 2;
        while (attempts > 0) {
            try {
                this.service.start();
            } catch (AppiumServerHasNotBeenStartedLocallyException e) {
                attempts -= 1;
                LOGGER.error("Failed to start Appium service (" + attempts + " attempts left): " + e.getMessage());
                continue;
            }
            break;
        }
    }

    public void createAppiumDriver() {
        LOGGER.info("Creating Appium Driver...");

        DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setCapability(MobileCapabilityType.AUTOMATION_NAME, "UIAutomator2");
        capabilities.setCapability(MobileCapabilityType.DEVICE_NAME, "Android Emulator");
        capabilities.setCapability(MobileCapabilityType.FULL_RESET, "true");
        capabilities.setCapability(AndroidMobileCapabilityType.AUTO_GRANT_PERMISSIONS, "true");
        capabilities.setCapability(MobileCapabilityType.APP, TraversalSettings.v().getApkFile().getAbsolutePath());
        capabilities.setCapability(MobileCapabilityType.NO_RESET, "false");
        capabilities.setCapability(AndroidMobileCapabilityType.AUTO_LAUNCH, "false");
        capabilities.setCapability(MobileCapabilityType.NEW_COMMAND_TIMEOUT, 3600);

        if (this.service == null) {
            this.startAppiumService();
        }

        this.driver = new AndroidDriver<>(this.service.getUrl(), capabilities);
        this.driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
    }

    public void cleanup() {
        if (this.driver != null) {
            LOGGER.info("Stopping logcat broadcast.");
            this.driver.stopLogcatBroadcast();
            LOGGER.info("Closing app.");
            this.driver.closeApp();
            LOGGER.info("Quitting Appium driver.");
            this.driver.quit();
        }

        if (this.service != null) {
            LOGGER.info("Stopping Appium service.");
            this.service.stop();
        }
    }

    public void startAppiumLogcatBroadcast() {
        LOGGER.info("Starting Appium Logcat Broadcast.");

        Consumer<String> logcatMessageConsumer = logMessage -> {
            if (logMessage.contains(LogHandler.M_TAG) || logMessage.contains(LogHandler.C_TAG) ||
                    logMessage.contains(LogHandler.I_TAG)) {
                LogHandler.handleMessage(logMessage);
            }
        };

        if (this.driver == null) {
            this.createAppiumDriver();
        }

        this.driver.addLogcatMessagesListener(logcatMessageConsumer);
        this.driver.startLogcatBroadcast();
    }

    public AndroidDriver<WebElement> getDriver() {
        if (this.driver == null) {
            this.createAppiumDriver();
        }
        return this.driver;
    }

    public String getCurrentActivity() {
        if (this.driver == null) {
            this.createAppiumDriver();
        }

        if (this.driver.currentActivity().startsWith(".")) {
            return this.driver.getCurrentPackage() + this.driver.currentActivity();
        }

        return this.driver.currentActivity();
    }

    public String getCurrentPackage() {
        if (this.driver == null) {
            this.createAppiumDriver();
        }

        return this.driver.getCurrentPackage();
    }

    public void navigateBack() {
        if (this.driver == null) {
            this.createAppiumDriver();
        }

        LOGGER.info("Navigating back.");
        this.driver.navigate().back();
    }

    public void launchApp() {
        if (this.driver == null) {
            this.createAppiumDriver();
        }

        LOGGER.info("Launching app.");
        this.driver.launchApp();
    }

    public void hideKeyboard() {
        if (this.driver == null) {
            this.createAppiumDriver();
        }

        if (this.driver.isKeyboardShown()) {
            LOGGER.info("Hiding keyboard.");
            this.driver.hideKeyboard();
        }
    }

    @SuppressWarnings("unused")
    public List<String> findFragmentsOnScreen() {
        LOGGER.info("Searching for fragments in the current Activity... ");
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("command", "dumpsys");
        arguments.put("args",
                Lists.newArrayList("activity", this.getCurrentPackage() + "/" + this.getCurrentActivity())
                     );

        List<String> fragments = new ArrayList<>();

        String scriptOutput = this.driver.executeScript("mobile: shell", arguments).toString();
        List<String> output = Arrays.stream(scriptOutput.split("\n")).map(String::trim).collect(Collectors.toList());
        for (int i = 0; i < output.size(); i++) {
            if (output.get(i).contains("Added Fragments:")) {
                if (output.get(i + 1).startsWith("#")) {
                    String fragment = output.get(i + 1).replaceFirst("#\\d+:\\s+", "").replaceFirst("\\{.+", "");
                    fragments.add(fragment);
                }
            }
        }

        LOGGER.info("Found " + fragments.size() + " fragments in the current Activity.");
        return fragments;
    }

    @SuppressWarnings("unused")
    public List<AndroidElement> getElementsOnScreen() {
        List<WebElement> elements = this.driver.findElementsByXPath("//*");
        return getAndroidElements(elements);
    }

    public List<AndroidElement> getClickElementsOnScreen() {
        By descriptor = MobileBy.AndroidUIAutomator("new UiSelector().clickable(true)");
        List<WebElement> elements = this.driver.findElements(descriptor);
        return getAndroidElements(elements);
    }

    private List<AndroidElement> getAndroidElements(List<WebElement> elements) {
        List<AndroidElement> androidElements = new ArrayList<>();

        for (WebElement element : elements) {
            AndroidElement androidElement = (AndroidElement) element;
            LOGGER.info("Found view element '" + AppiumManager.getElementDescription(androidElement) + "'.");
            androidElements.add(androidElement);
        }

        return androidElements;
    }
}
