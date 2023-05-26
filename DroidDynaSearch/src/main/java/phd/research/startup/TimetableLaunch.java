package phd.research.startup;

import io.appium.java_client.MobileBy;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phd.research.appium.AppiumManager;
import phd.research.appium.Traversal;

import java.util.List;

/**
 * @author Jordan Doyle
 */

public class TimetableLaunch implements AppLaunch {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimetableLaunch.class);

    public TimetableLaunch() {

    }

    @Override
    public void launch(AndroidDriver<WebElement> driver) {
        try {
            Traversal.waitForUi(2f);

            By descriptor = MobileBy.AndroidUIAutomator("new UiSelector().clickable(true)");
            List<WebElement> elements = driver.findElements(descriptor);

            for (WebElement element : elements) {
                AndroidElement androidElement = (AndroidElement) element;
                LOGGER.info("Found view element '" + AppiumManager.getElementDescription(androidElement) + "'.");
            }

            Traversal.waitForUi(2f);
            LOGGER.info("Pressing Grant Button.");
            AndroidElement grantButton =
                    (AndroidElement) driver.findElementById("com.asdoi.timetable:id/md_buttonDefaultPositive");
            grantButton.click();

            Traversal.waitForUi(2f);
            LOGGER.info("Pressing Permission Button.");
            String automatorCommand = "new UiSelector().textContains(\"Timetable\")";
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
