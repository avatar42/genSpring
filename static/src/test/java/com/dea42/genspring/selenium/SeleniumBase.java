/**
 * 
 */
package com.dea42.genspring.selenium;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.server.LocalServerPort;

import io.github.bonigarcia.wdm.WebDriverManager;
import junit.framework.TestCase;

/**
 * Base class for Selenium tests
 * 
 * @author GenSpring
 *
 */
public class SeleniumBase extends TestCase {
	protected static final Logger LOGGER = LoggerFactory.getLogger(SeleniumBase.class.getName());

	@LocalServerPort
	protected int port;
	protected String base;
	protected String context = "genspring";
	protected WebDriver driver;
	protected int timeOutInSeconds = 30;
	protected boolean useLocal = false;

	protected WebElement getBy(By selector) {
		WebElement element = null;
		try {
			element = driver.findElement(selector);
		} catch (Exception e) {
			LOGGER.info("Element " + selector + " not found");
		}
		return element;
	}

	/**
	 * Open url in browser and wait for page to finish loading
	 * 
	 * @param url
	 */
	protected void open(String url) {
		driver.get(url);
		driver.manage().window().maximize();
		waitForPageLoaded();
	}

	protected String getSrc() {
		String rtn = null;
		try {
			rtn = driver.getPageSource();
		} catch (Exception e) {
			LOGGER.warn("failed getting page source", e);
		}

		return rtn;
	}

	/**
	 * Check to see if expected is in page source
	 * 
	 * @param expected
	 * @param doesNotContain if true checks that expected is NOT in source
	 */
	protected void sourceContains(String expected, boolean doesNotContain) {
		String src = getSrc();
		assertNotNull("checking page source not null", src);
		if (doesNotContain) {
			if (src.contains(expected)) {
				fail("'" + expected + "' was found in page source:" + src);
			}
		} else {
			if (!src.contains(expected)) {
				fail("'" + expected + "' was not found in page source:" + src);
			}
		}

	}

	/**
	 * Open a url on Spring Boot app ahnd wait for page to load
	 * 
	 * @param url without this.base bit as in /login
	 */
	protected void openTest(String url) {
		open(base + url);
	}

	/**
	 * wait for the element to appear then click it.
	 * 
	 * @param selector
	 * @return
	 */
	protected WebElement waitThenClick(By selector) {
		waitForElement(selector);
		return clickBy(selector);

	}

	protected WebElement clickLinkByText(String text) {
		WebElement element = getBy(By.linkText(text));
		assertNotNull("Getting link with text '" + text + "'", element);
		element.click();
		LOGGER.debug("Clicked link:" + text);
		return element;
	}

	protected WebElement clickBy(By selector) {
		WebElement element = getBy(selector);
		assertNotNull("Getting element" + selector, element);
		element.click();
		LOGGER.debug("Clicked:" + selector);
		return element;
	}

	protected String getAttribute(WebElement element, String attrName) {
		String rtn = null;
		try {
			rtn = element.getAttribute(attrName);
		} catch (Exception e) {
			LOGGER.warn("Failed to get attribue " + attrName + " from " + element, e);
		}

		return rtn;
	}

	protected WebElement type(By selector, String text, boolean clearFirst) {
		WebElement element = getBy(selector);
		assertNotNull("Getting element by:" + selector, element);

		String b4text = "";

		if (clearFirst) {
			element.clear();
		} else {
			b4text = element.getText();
		}

		element.sendKeys(text);
		LOGGER.debug("Clicked link:" + text);

		String actual = getAttribute(element, "value");
		assertEquals("Checking element contains what we typed", b4text + text, actual);

		return element;
	}

	public void waitForPageLoaded() {
		ExpectedCondition<Boolean> expectation = new ExpectedCondition<Boolean>() {
			public Boolean apply(WebDriver driver) {
				return ((JavascriptExecutor) driver).executeScript("return document.readyState").toString()
						.equals("complete");
			}
		};
		try {
			Thread.sleep(1000);
			WebDriverWait wait = new WebDriverWait(driver, timeOutInSeconds);
			wait.until(expectation);
		} catch (Throwable error) {
			Assert.fail("Timeout waiting for Page Load Request to complete.");
		}
	}

	public void waitForElement(By selector) {
		ExpectedCondition<Boolean> expectation = new ExpectedCondition<Boolean>() {
			public Boolean apply(WebDriver driver) {
				try {
					WebElement element = driver.findElement(selector);
					return (element != null);
				} catch (Exception e) {
					return false;
				}
			}
		};
		try {
			Thread.sleep(1000);
			WebDriverWait wait = new WebDriverWait(driver, timeOutInSeconds);
			wait.until(expectation);
		} catch (Throwable error) {
			Assert.fail("Timeout waiting for Page Load after click of " + selector + " to complete.");
		}
	}

	/**
	 * load webdriver and set base part of URL to local app
	 */
	@Before
	public void setUp() throws Exception {
// If downloading and running an exe makes you nervous (and it probably should) set useLocal to true and update path below.
		if (useLocal) {
			System.setProperty("webdriver.gecko.driver", "C:\\webdriver\\geckodriver-v0.11.1-win64\\geckodriver.exe");
			DesiredCapabilities dc = DesiredCapabilities.firefox();
			dc.setCapability("marionette", true);
		} else {
			WebDriverManager.firefoxdriver().setup();
		}
		driver = new FirefoxDriver();
		if (this.getClass().getName().endsWith("IT"))
			this.base = "http://localhost:8089/" + context;
		else
			this.base = "http://localhost:" + port;
	}

	@After
	public void teardown() {
		if (driver != null)
			driver.quit();
	}

	/**
	 * Run through the links from the home page down to the edit page, check for the
	 * save link and then cancel.
	 * 
	 * @param item        display name of the Entity class (probably the same as the
	 *                    class name).
	 * @param expectLogin if true expect and handle the login challenge.
	 */
	protected void checkEditLinks(String item, boolean expectLogin) {
		openTest("/");
		waitThenClick(By.linkText(item));
		if (expectLogin) {
			type(By.name("username"), "user", true);
			type(By.name("password"), "password", true);
			clickBy(By.xpath("//input[@value='Login']"));
		}

		waitThenClick(By.linkText("Create New " + item));
		sourceContains("Create New " + item, false);
		assertNotNull("Checking for save button", getBy(By.xpath("//button[@value='save']")));
		waitThenClick(By.xpath("//button[@value='cancel']"));

		waitThenClick(By.linkText("Edit"));
		sourceContains("Edit " + item, false);
		assertNotNull("Checking for save button", getBy(By.xpath("//button[@value='save']")));
		waitThenClick(By.xpath("//button[@value='cancel']"));

	}

	protected void checkSite() throws Exception {
		List<WebElement> links = null;
		// check statics
		// check css links work
		openTest("/css/site.css");
		sourceContains("background-color:", false);
		openTest("/css/bootstrap.min.css");
		sourceContains("Bootstrap v3.0.0", false);
		openTest("/resources/sheet.css");
		sourceContains("fonts.googleapis.com", false);

		// check js links work
		openTest("/js/jquery.min.js");
		sourceContains("jQuery v1.11.1", false);
		openTest("/js/bootstrap.min.js");
		sourceContains("bootstrap.js v3.0.0", false);

		// Check tabs saves as static pages
		openTest("/optView.html");
		sourceContains("resources/sheet.css", false);
		openTest("/Players.html");
		sourceContains("resources/sheet.css", false);

		// do basic web page checks
		openTest("/");
		links = driver.findElements(By.cssSelector("a"));
		List<String> names = new ArrayList<String>();
		for (WebElement we : links) {
			String txt = we.getText();
			if (!"Login".equals(txt) && !"/api/".equals(txt)) {
				names.add(txt);
			}
		}

		boolean login = true;
		for (String name : names) {
			checkEditLinks(name, login);
			if (login)
				login = false;
		}

		openTest("/api/");
		links = driver.findElements(By.cssSelector("a"));
		List<String> refs = new ArrayList<String>();
		for (WebElement we : links) {
			String txt = getAttribute(we, "href");
			if (txt.contains("/api/")) {
				refs.add(txt);
			}
		}

		for (String ref : refs) {
			open(ref);
			sourceContains("<id>1</id>", false);
		}

	}
}
