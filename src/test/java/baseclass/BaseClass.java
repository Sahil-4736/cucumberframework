package baseclass;

import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.util.Properties;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.Platform;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import io.github.bonigarcia.wdm.WebDriverManager;
import java.net.URL;

/**
 * Centralized base class to initialize WebDriver, properties and logger.
 */
public class BaseClass {
    private static WebDriver driver;
    private static Properties p;
    private static Logger logger;

    /**
     * Initialize and return a WebDriver based on config.properties.
     * Method name fixed to initializeBrowser() so other classes can call it.
     */
    public static WebDriver initializeBrowser() throws IOException {
        // load properties (cached)
        Properties props = getProperties();

        String executionEnv = props.getProperty("execution_env", "local").trim();
        String browser = props.getProperty("browser", "chrome").trim().toLowerCase();

        if ("remote".equalsIgnoreCase(executionEnv)) {
            DesiredCapabilities capabilities = new DesiredCapabilities();

            // OS / platform
            String os = props.getProperty("os", "windows").trim().toLowerCase();
            if ("windows".equalsIgnoreCase(os)) {
                capabilities.setPlatform(Platform.WIN11);
            } else if ("mac".equalsIgnoreCase(os)) {
                capabilities.setPlatform(Platform.MAC);
            } else if ("linux".equalsIgnoreCase(os)) {
                capabilities.setPlatform(Platform.LINUX);
            } else {
                System.out.println("Unknown OS in config.properties: " + os + ". Defaulting to WINDOWS.");
                capabilities.setPlatform(Platform.WIN11);
            }

            // browser
            switch (browser) {
                case "chrome":
                    capabilities.setBrowserName("chrome");
                    break;
                case "edge":
                    capabilities.setBrowserName("MicrosoftEdge");
                    break;
                default:
                    throw new RuntimeException("Unsupported browser for remote execution: " + browser);
            }

            String gridUrl = props.getProperty("grid_url", "http://localhost:4444/wd/hub").trim();
            driver = new RemoteWebDriver(new URL(gridUrl), capabilities);

        } else if ("local".equalsIgnoreCase(executionEnv)) {
            // use WebDriverManager to avoid local driver binaries
            switch (browser) {
                case "chrome":
                    WebDriverManager.chromedriver().setup();
                    driver = new ChromeDriver();
                    break;
                case "edge":
                    WebDriverManager.edgedriver().setup();
                    driver = new EdgeDriver();
                    break;
                default:
                    throw new RuntimeException("Unsupported local browser: " + browser);
            }
        } else {
            throw new RuntimeException("Unsupported execution_env in config.properties: " + executionEnv);
        }

        if (driver == null) {
            throw new RuntimeException("WebDriver was not initialized. Check your config.properties.");
        }

        // driver basic setup
        driver.manage().deleteAllCookies();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(Long.parseLong(
                props.getProperty("implicit_wait_seconds", "10"))));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(Long.parseLong(
                props.getProperty("page_load_timeout_seconds", "30"))));

        return driver;
    }

    // Provide global access to driver if needed
    public static WebDriver getDriver() {
        return driver;
    }

    // Load properties from config file (cached)
    public static Properties getProperties() throws IOException {
        if (p == null) {
            FileReader fr = null;
            try {
                fr = new FileReader(System.getProperty("user.dir") + "/src/test/resources/config.properties");
                p = new Properties();
                p.load(fr);
            } finally {
                if (fr != null) {
                    fr.close();
                }
            }
        }
        return p;
    }

    // Logger singleton
    public static Logger getLogger() {
        if (logger == null) {
            logger = LogManager.getLogger(BaseClass.class);
        }
        return logger;
    }

    // Random helpers
    public static String randomeString() {
        return RandomStringUtils.randomAlphabetic(5);
    }

    public static String randomeNumber() {
        return RandomStringUtils.randomNumeric(10);
    }

    public static String randomAlphaNumeric() {
        return RandomStringUtils.randomAlphabetic(5) + RandomStringUtils.randomNumeric(10);
    }

    // Close/quit helper
    public static void quitDriver() {
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception e) {
                // swallow errors on quit
            } finally {
                driver = null;
            }
        }
    }
}
