package red.zyc.babydogepaws.selenium;

import org.openqa.selenium.JavascriptExecutor;

/**
 * @author allurx
 */
public final class SeleniumSupport {

    @SuppressWarnings("unchecked")
    public static <T> T executeScript(JavascriptExecutor jsExecutor, String script, Object... args) {
        return (T) jsExecutor.executeScript(script, args);
    }
}
