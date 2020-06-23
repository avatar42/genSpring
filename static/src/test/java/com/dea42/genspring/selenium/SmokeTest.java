/**
 * 
 */
package com.dea42.genspring.selenium;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Basic smoke test to check app works.
 * 
 * @author GenSpring
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SmokeTest extends SeleniumBase {

	/**
	 * Test test framework is working
	 */
	@Test
	public void getSearchPage() {
		open("https://www.google.com");
		WebElement element = driver.findElement(By.name("q"));
		assertNotNull("Testing driver works", element);
	}

	/**
	 * Do basic login and check pages are reachable in Spring Boot standalone mode
	 * 
	 * @throws Exception
	 */
	@Test
	public void smokeTest() throws Exception {
		checkSite();
	}
}