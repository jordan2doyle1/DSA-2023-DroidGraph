package phd.research.startup;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phd.research.appium.Traversal;

/**
 * @author Jordan Doyle
 */

public class VolumeControlLaunch implements AppLaunch {

    private static final Logger LOGGER = LoggerFactory.getLogger(VolumeControlLaunch.class);

    public VolumeControlLaunch() {

    }

    public void launch(AndroidDriver<WebElement> driver) {
        try {
            Traversal.waitForUi(2f);
            LOGGER.info("Pressing OK Button.");
            AndroidElement okButton = (AndroidElement) driver.findElementById("android:id/button1");
            okButton.click();

            Traversal.waitForUi(2f);
            LOGGER.info("Pressing Permission Button.");
            String automatorCommand = "new UiSelector().textContains(\"Volume Control\")";
            AndroidElement permissionButton = (AndroidElement) driver.findElementByAndroidUIAutomator(automatorCommand);
            permissionButton.click();

            Traversal.waitForUi(2f);
            LOGGER.info("Pressing Allow Button.");
            AndroidElement allowButton = (AndroidElement) driver.findElementById("android:id/button1");
            allowButton.click();

            Traversal.waitForUi(2f);
            LOGGER.info("Pressing Back Button.");
            driver.navigate().back();
        } catch (NoSuchElementException ignored) {

        }
    }
}
