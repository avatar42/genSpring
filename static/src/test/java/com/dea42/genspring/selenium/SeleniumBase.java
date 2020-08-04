/**
 * 
 */
package com.dea42.genspring.selenium;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.web.servlet.ResultActions;

import com.dea42.genspring.UnitBase;
import com.dea42.genspring.utils.Utils;

import io.github.bonigarcia.wdm.WebDriverManager;

/**
 * Base class for Selenium tests
 * 
 * @author GenSpring
 *
 */
public class SeleniumBase extends UnitBase {

	@LocalServerPort
	protected int port;
	protected String base;
	protected String context = "genspring";
	protected WebDriver driver;
	protected int timeOutInSeconds = 30;
	protected boolean useLocal = false;
	// time to pause at the start of each command.
	protected long speedDelay = 1;

	@Rule
	public ScreenShotRule screenshotRule = new ScreenShotRule();

	/**
	 * called at the start of each "command". Set speedDelay above to the
	 * milliseconds pause you want between commands or 0 for none.
	 */
	protected void speedControl() {
		if (speedDelay > 0) {
			try {
				Thread.sleep(speedDelay);
			} catch (InterruptedException e) {
				// ignored
			}
		}
	}

	protected WebElement getBy(By selector, WebElement parent) {
		speedControl();
		WebElement element = null;
		try {
			if (parent == null)
				element = driver.findElement(selector);
			else
				element = parent.findElement(selector);
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
		speedControl();
		driver.get(url);
		driver.manage().window().maximize();
		waitForPageLoaded();
	}

	protected String getSrc() {
		speedControl();
		String rtn = null;
		try {
			rtn = driver.getPageSource();
		} catch (Exception e) {
			LOGGER.warn("failed getting page source", e);
		}

		return rtn;
	}

	/**
	 * Checks the nav system
	 */
	public void checkHeader(ResultActions result, String user) throws Exception {
		speedControl();
		List<String> names = getMenuLinks("guiMenu", "header.gui", false);
		assertTrue("Check for more than 0 GUI menu items", names.size() > 0);
		boolean login = true;
		for (String name : names) {
			checkEditLinks(name, login);
			if (login)
				login = false;
		}

		List<String> refs = getMenuLinks("restMenu", "header.restApi", true);
		assertTrue("Check for more than 0 REST API menu items", names.size() > 0);
		for (String ref : refs) {
			open(ref);
			sourceContains("<id>1</id>", false);
		}

		// check lang changes
		openTest("/");
		refs = getMenuLinks("langMenu", "lang.change", true);
		List<String> langs = new ArrayList<String>();
		for (String ref : refs) {
			langs.add(ref.substring(ref.length() - 2, ref.length()));
		}
		for (String ref : refs) {
			lang = ref.substring(ref.length() - 2, ref.length());
			open(ref);
			sourceContains("<span>" + getMsg("lang.change") + "</span>", false);
			for (String lang : langs) {
				sourceContains(">" + getMsg("lang." + lang) + "</a>", false);
			}
		}

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
	 * Open a url on Spring Boot app and wait for page to load
	 * 
	 * @param url without this.base bit as in /login
	 */
	protected void openTest(String url) {
		open(base + url);
	}

	/**
	 * Wait for the element to appear then click it. Same as waitThenClick(By, null)
	 * 
	 * @param selector
	 * @return
	 */
	protected WebElement waitThenClick(By selector) {
		return waitThenClick(selector, null);
	}

	/**
	 * wait for the element to appear then click it.
	 * 
	 * @param selector
	 * @param parent   element to search from or null to search page
	 * @return
	 */
	protected WebElement waitThenClick(By selector, WebElement parent) {
		waitForElement(selector);
		LOGGER.debug("clicking:" + selector);
		return clickBy(selector, parent);

	}

	/**
	 * Click a link based on its text. wrapper for clickBy(By.linkText(text),
	 * parent)
	 * 
	 * @param text
	 * @param parent
	 * @return
	 */
	protected WebElement clickLinkByText(String text, WebElement parent) {
		return clickBy(By.linkText(text), parent);
	}

	/**
	 * 
	 * @param selector
	 * @param parent
	 * @return
	 */
	protected WebElement clickBy(By selector, WebElement parent) {
		speedControl();
		WebElement element = getBy(selector, parent);
		assertNotNull("Getting element" + selector, element);
		element.click();
		LOGGER.debug("Clicked:" + selector);
		return element;
	}

	protected String getAttribute(WebElement element, String attrName) {
		speedControl();
		String rtn = null;
		try {
			rtn = element.getAttribute(attrName);
		} catch (Exception e) {
			LOGGER.warn("Failed to get attribue " + attrName + " from " + element, e);
		}

		return rtn;
	}

	protected WebElement type(By selector, String text, boolean clearFirst, WebElement parent) {
		speedControl();
		WebElement element = getBy(selector, parent);
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
		speedControl();
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

	public WebElement waitForElement(By selector) {
		speedControl();
		WebElement element = null;
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
			element = driver.findElement(selector);
		} catch (Throwable error) {
			Assert.fail("Timeout waiting for Page Load after click of " + selector + " to complete.");
		}

		return element;
	}

	/**
	 * load web driver and set base part of URL to local app
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

//	@After
//	public void teardown() {
//		if (driver != null)
//			driver.quit();
//	}

	public void loginAdmin() {
		speedControl();
		WebElement form = waitForElement(By.id("signinForm"));
		ResourceBundle bundle = ResourceBundle.getBundle("app");
		String user = Utils.getProp(bundle, "default.admin", null);
		String userpass = Utils.getProp(bundle, "default.adminpass", null);
		sourceContains("<legend>" + getMsg("signin.title") + "</legend>", false);
		type(By.id("email"), user, true, form);
		type(By.id("password"), userpass, true, form);
		clickBy(By.id("signinBtn"), form);

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
		speedControl();
		openTest("/");
		WebElement menu = waitThenClick(By.id("guiMenu"), null);
		waitThenClick(By.linkText(item), menu);
		if (expectLogin) {
			loginAdmin();
			menu = waitThenClick(By.id("guiMenu"), null);
			waitThenClick(By.linkText(item), menu);
		}

		waitThenClick(By.linkText(getMsg("edit.new") + " " + item), null);
		sourceContains(getMsg("edit.new") + " " + item, false);
		assertNotNull("Checking for cancel button", getBy(By.xpath("//button[@value='cancel']"), null));
		waitThenClick(By.xpath("//button[@value='cancel']"), null);

		waitThenClick(By.linkText("Edit"), null);
		sourceContains("Edit " + item, false);
		assertNotNull("Checking for save button", getBy(By.xpath("//button[@value='save']"), null));
		waitThenClick(By.xpath("//button[@value='save']"), null);

	}

	/**
	 * Get all the menu item links for a menu. TODO: add sub menu support
	 * 
	 * @param menuId
	 * @param menuKey     TODO
	 * @param returnHrefs
	 * @return
	 */
	protected List<String> getMenuLinks(String menuId, String menuKey, boolean returnHrefs) {
		openTest("/");
		WebElement menu = waitThenClick(By.id(menuId), null);
		List<WebElement> links = menu.findElements(By.cssSelector("a"));
		List<String> refs = new ArrayList<String>();
		for (WebElement we : links) {
			// skip menu link we clicked to display menu
			String txt = we.getText();
			if (!getMsg(menuKey).equals(txt)) {
				if (returnHrefs) {
					refs.add(getAttribute(we, "href"));
				} else {
					refs.add(txt);
				}
			}
		}

		LOGGER.debug(refs.toString());
		return refs;
	}

	protected void checkSite() throws Exception {
		// check statics
		// check css links work
		openTest("/resources/css/site.css");
		sourceContains("background-color:", false);
		openTest("/resources/css/bootstrap.min.css");
		sourceContains("Bootstrap v3.0.0", false);
		openTest("/resources/sheet.css");
		sourceContains("fonts.googleapis.com", false);

		// check js links work
		openTest("/resources/js/jquery.min.js");
		sourceContains("jQuery v1.11.1", false);
		openTest("/resources/js/bootstrap.min.js");
		sourceContains("bootstrap.js v3.0.0", false);

		// Check tabs saves as static pages
		openTest("/optView.html");
		sourceContains("resources/sheet.css", false);
		openTest("/Players.html");
		sourceContains("resources/sheet.css", false);

		// do basic web page checks
		checkHeader(null, null);
	}

	public class ScreenShotRule extends TestWatcher {
		String testName = "";
		private long startTime = 0;
		private long endTime = 0;

		public ScreenShotRule() {
		}

		@Override
		protected void starting(Description description) {
			startTime = System.currentTimeMillis();
			if (description.getDisplayName() != null)
				testName = description.getDisplayName();
			else if (description.getMethodName() != null)
				testName = description.getMethodName();
			else if (description.getClassName() != null)
				testName = description.getClassName();
			else
				testName = "unknown";
		}

		@Override
		protected void failed(Throwable e, Description description) {
			endTime = System.currentTimeMillis();
			try {
				TakesScreenshot takesScreenshot = (TakesScreenshot) driver;

				File scrFile = takesScreenshot.getScreenshotAs(OutputType.FILE);
				File destFile = new File(testName + ".screenShot.png");
				FileUtils.copyFile(scrFile, destFile);
				LOGGER.error("saved screenshot to:" + destFile.getAbsolutePath(), e);
			} catch (IOException ioe) {
				throw new RuntimeException(ioe);
			}
		}

		@Override
		protected void succeeded(Description description) {
			endTime = System.currentTimeMillis();
		}

		@Override
		protected void finished(Description description) {
			LOGGER.info("Runtime:" + (endTime - startTime) + " milliseconds");
			if (driver != null)
				driver.quit();
		}

	}
}
