/**
 * 
 */
package com.dea42.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.ResourceBundle;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import com.dea42.build.CommonMethods;
import com.dea42.build.Sheets2DBTest;

import lombok.extern.slf4j.Slf4j;

/**
 * @author avata
 *
 */
@RunWith(BlockJUnit4ClassRunner.class)
@Slf4j
public class UtilsTest {

	/**
	 * Test method for
	 * {@link com.dea42.common.Utils#getProp(java.util.ResourceBundle, java.lang.String)}.
	 */
	@Test
	public void testGetPropResourceBundleString() {
		ResourceBundle bundle = ResourceBundle.getBundle(Sheets2DBTest.bundleName);
		String val = Utils.getProp(bundle, "db.driver");
		assertEquals("db.driver", "org.sqlite.JDBC", val);
		val = Utils.getProp(bundle, "db.drive");
		assertNull("db.drive", val);
	}

	/**
	 * Test method for
	 * {@link com.dea42.common.Utils#getProp(java.util.ResourceBundle, java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testGetPropResourceBundleStringString() {
		ResourceBundle bundle = ResourceBundle.getBundle(Sheets2DBTest.bundleName);
		String val = Utils.getProp(bundle, "db.driver", "bob");
		assertEquals("db.driver", "org.sqlite.JDBC", val);
		val = Utils.getProp(bundle, "db.drive", "bob");
		assertEquals("db.driver", "bob", val);
	}

	/**
	 * Test method for
	 * {@link com.dea42.common.Utils#getPropList(java.util.ResourceBundle, java.lang.String)}.
	 */
	@Test
	public void testGetPropListResourceBundleString() {
		ResourceBundle bundle = ResourceBundle.getBundle(Sheets2DBTest.bundleName);
		List<String> list = Utils.getPropList(bundle, CommonMethods.PROPKEY + ".tabs");
		assertTrue(CommonMethods.PROPKEY + ".tabs", list.contains("Roamio_Todo"));

		list = Utils.getPropList(bundle, CommonMethods.PROPKEY + ".tab");
		assertTrue(CommonMethods.PROPKEY + ".tab", list.isEmpty());
	}

	/**
	 * Test method for
	 * {@link com.dea42.common.Utils#tabToStr(java.util.ResourceBundle, java.lang.String)}.
	 */
	@Test
	public void testTabToStr() {
		String tableName = Utils.tabToStr(null, "ab_cd ef/gh.ij(kl)mn?op!qr");
		assertEquals("ab_cd ef/gh.ij(kl)mn?op!qr", "AbCdEfGhIjKlMnOpQr", tableName);
	}

	/**
	 * Test method for
	 * {@link com.dea42.common.Utils#tabToStr(java.util.ResourceBundle, java.lang.String)}.
	 */
	@Test
	public void testTabToStr2() {
		String tableName = Utils.tabToStr(null, "_cd ef/gh.ij(kl)mn?op!");
		assertEquals("_cd ef/gh.ij(kl)mn?op!", "CdEfGhIjKlMnOp", tableName);
	}

	/**
	 * Test method for
	 * {@link com.dea42.common.Utils#tabToStr(java.util.ResourceBundle, java.lang.String)}.
	 */
	@Test
	public void testTabToStr3() {
		ResourceBundle renames = ResourceBundle.getBundle("renametest");
		String tableName = Utils.tabToStr(renames, "In Last Show / showRSS link");
		assertEquals("In Last Show / showRSS link", "InShowRssAs", tableName);
	}

	/**
	 * Test method for
	 * {@link com.dea42.common.Utils#tabToStr(java.util.ResourceBundle, java.lang.String)}.
	 */
	@Test
	public void testTabToStr4() {
		String tableName = Utils.tabToStr(null, "_short");
		assertEquals("ShortField", tableName);
	}

	/**
	 * Tests method for
	 * {@link com.dea42.common.Utils#getPathAsString(java.lang.String, java.lang.String)}.
	 * and
	 * {@link com.dea42.common.Utils#getPath(java.lang.String, java.lang.String)}.
	 * since getPathAsString just converts getPath return to standardized String
	 */
	@Test
	public void testGetPathAsString() {
		String prjRoot = System.getProperty("user.dir").toString().replace('\\', '/');
		if (prjRoot.endsWith("target"))
			prjRoot = prjRoot.substring(0, prjRoot.length() - 7);

		String p = Utils.getPathAsString("src/main/resources/base");
		assertEquals(prjRoot + "/src/main/resources/base", p);
		p = Utils.getPathAsString("../genSpring/src/main/resources/base");
		assertEquals(prjRoot + "/src/main/resources/base", p);
		p = Utils.getPathAsString("../genSpring", "src/main/resources/base");
		assertEquals(prjRoot + "/src/main/resources/base", p);

		// no way to cd in Java. Must run manually in other folder to test in target
		// folder logic
		// TODO: ? % java -cp .:"/Applications/IntelliJ IDEA 13 CE.app/Contents/lib/*"
		// org.junit.runner.JUnitCore UtilsTest
		System.setProperty("user.dir", prjRoot + "/target");
		log.info("Changed to:" + System.getProperty("user.dir"));
		p = Utils.getPathAsString("src/main/resources/base");
		assertEquals(prjRoot + "/src/main/resources/base", p);
		p = Utils.getPathAsString("../genSpring/src/main/resources/base");
		assertEquals(prjRoot + "/src/main/resources/base", p);
		p = Utils.getPathAsString("../genSpringTest", "target/v2j/",
				"src/main/resources/base/src/test/java/com/dea42/genspring/selenium/SeleniumBase.java.vm");
		assertEquals(prjRoot.replace("/genSpring", "/genSpringTest")
				+ "/target/v2j/src/main/resources/base/src/test/java/com/dea42/genspring/selenium/SeleniumBase.java.vm",
				p);
		p = Utils.getPathAsString("../genSpringTest",
				prjRoot + "/src/main/resources/base/src/test/java/com/dea42/genspring/selenium/SeleniumBase.java.vm");
		assertEquals(prjRoot.replace("/genSpring", "/genSpringTest")
				+ "/src/main/resources/base/src/test/java/com/dea42/genspring/selenium/SeleniumBase.java.vm",
				p);
		log.debug("passed:" + p);
	}

	/**
	 * Test method for
	 * {@link com.dea42.common.Utils#getRelPath(java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testGetRelPath() {
		String relPathStr = Utils.getRelPath("D:\\SpringTools4.6.1\\workspace\\genSpring", "D:\\SpringTools4.6.1\\workspace\\genSpringTest\\target\\v2j\\src\\test\\java\\com\\dea42\\genspring\\UnitBase.java");
		assertEquals("..\\genSpringTest\\target\\v2j\\src\\test\\java\\com\\dea42\\genspring\\UnitBase.java", relPathStr);
	}

	
}
