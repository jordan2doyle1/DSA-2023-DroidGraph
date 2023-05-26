package phd.research.startup;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebElement;

/**
 * @author Jordan Doyle
 */

public interface AppLaunch {
    void launch(AndroidDriver<WebElement> driver);
}
