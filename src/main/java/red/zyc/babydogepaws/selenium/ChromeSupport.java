package red.zyc.babydogepaws.selenium;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * chrome浏览器相关的操作
 *
 * @author allurx
 */
public final class ChromeSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChromeSupport.class);

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(ChromeSupport::cleanChromeAndDriverProcess));
    }

    private ChromeSupport() {
    }

    /**
     * 启动一个新的chrome进程
     *
     * @param chromeDataDir chrome浏览器用户数据目录
     * @return 一个新的chrome进程
     */
    public static Process startChromeProcess(String chromeDataDir) {
        try {
            int port = findAvailablePort();
            BabyDogePawsContext.set(BabyDogePawsContextItem.CHROME_PROCESS_PORT, port);
            String[] startChromeCommand = new String[]{
                    "chrome",
                    "--no-first-run",
                    "--start-maximized",
                    "--headless=new",
                    "--user-data-dir=" + chromeDataDir,
                    "--remote-debugging-port=" + port
            };
            Process process = (new ProcessBuilder(startChromeCommand)).start();
            BabyDogePawsContext.set(BabyDogePawsContextItem.CHROME_PROCESS, process);
            return process;
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * @return 本机随机可用端口
     */
    public static int findAvailablePort() {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            // 端口号为 0，表示让操作系统分配一个随机可用的端口
            return serverSocket.getLocalPort();
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * @return 启动一个新的chromedriver
     */
    public static WebDriver startChromeDriver() {
        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("debuggerAddress", "127.0.0.1:" + BabyDogePawsContext.<Integer>get(BabyDogePawsContextItem.CHROME_PROCESS_PORT));
        ChromeDriver chromeDriver = new ChromeDriver(options);
        BabyDogePawsContext.set(BabyDogePawsContextItem.WEB_DRIVER, chromeDriver);
        return chromeDriver;
    }

    /**
     * 清除系统中chrome和chromedriver进程
     */
    public static void cleanChromeAndDriverProcess() {
        try {
            (new ProcessBuilder(new String[]{"taskkill", "/F", "/T", "/IM", "chrome.exe"})).start();
            (new ProcessBuilder(new String[]{"taskkill", "/F", "/T", "/IM", "chromedriver.exe"})).start();
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

}
