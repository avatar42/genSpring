/**
 * 
 */
package com.dea42.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;
import java.util.ResourceBundle;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.dea42.build.CommonMethods;
import com.dea42.build.Sheets2DBTest;

import lombok.extern.slf4j.Slf4j;

/**
 * @author avata
 *
 */
@ExtendWith(SpringExtension.class)
@Slf4j
class UtilsTest {

	/**
	 * Test method for
	 * {@link com.dea42.common.Utils#getProp(java.util.ResourceBundle, java.lang.String)}.
	 */
	@Test
	void testGetPropResourceBundleString() {
		ResourceBundle bundle = ResourceBundle.getBundle(Sheets2DBTest.bundleName);
		String val = Utils.getProp(bundle, "db.driver");
		assertEquals("org.sqlite.JDBC", val, "db.driver");
		val = Utils.getProp(bundle, "db.drive");
		assertNull("db.drive", val);
	}

	/**
	 * Test method for
	 * {@link com.dea42.common.Utils#getPropCls(java.util.ResourceBundle, java.lang.String, java.lang.Class)}
	 * 
	 */
	@Test
	void testGetPropClsGetPropResourceBundleStringClass() {
		ResourceBundle bundle = ResourceBundle.getBundle("Watchlist");
		Class<?> val = Utils.getPropCls(bundle, "RoamioNpl.L.type", null);
		assertNotNull(val, "Checking RoamioNpl.L.type is not null");
		assertTrue(val.isAssignableFrom(String.class), "Checking RoamioNpl.L.type is String");

		// shows.24.type=java.util.Date
		val = Utils.getPropCls(bundle, "Shows.BG.type", null);
		assertNotNull(val, "Checking Shows.BG.type is not null");
		assertTrue(val.isAssignableFrom(java.util.Date.class), "Checking Shows.BG.type is Date");

		val = Utils.getPropCls(bundle, "shows.B.type", null);
		assertNull(val, "Checking shows.B.type is null");

	}

	/**
	 * Test method for
	 * {@link com.dea42.common.Utils#getProp(java.util.ResourceBundle, java.lang.String, java.lang.String)}.
	 */
	@Test
	void testGetPropResourceBundleStringString() {
		ResourceBundle bundle = ResourceBundle.getBundle(Sheets2DBTest.bundleName);
		String val = Utils.getProp(bundle, "db.driver", "bob");
		assertEquals("org.sqlite.JDBC", val, "db.driver");
		val = Utils.getProp(bundle, "db.drive", "bob");
		assertEquals("bob", val, "db.driver");
	}

	/**
	 * Test method for
	 * {@link com.dea42.common.Utils#getPropList(java.util.ResourceBundle, java.lang.String)}.
	 */
	@Test
	void testGetPropListResourceBundleString() {
		ResourceBundle bundle = ResourceBundle.getBundle(Sheets2DBTest.bundleName);
		List<String> list = Utils.getPropList(bundle, CommonMethods.PROPKEY + ".tabs");
		assertTrue(list.contains("Sheet 1"), CommonMethods.PROPKEY + ".tabs");

		list = Utils.getPropList(bundle, CommonMethods.PROPKEY + ".tab");
		assertTrue(list.isEmpty(), CommonMethods.PROPKEY + ".tab");
	}

	/**
	 * Test method for
	 * {@link com.dea42.common.Utils#tabToStr(java.util.ResourceBundle, java.lang.String)}.
	 */
	@Test
	void testTabToStr() {
		String tableName = Utils.tabToStr(null, "ab_cd ef/gh.ij(kl)mn?op!qr");
		assertEquals("AbCdEfGhIjKlMnOpQr", tableName, "ab_cd ef/gh.ij(kl)mn?op!qr");
	}

	/**
	 * Test method for
	 * {@link com.dea42.common.Utils#tabToStr(java.util.ResourceBundle, java.lang.String)}.
	 */
	@Test
	void testTabToStr2() {
		String tableName = Utils.tabToStr(null, "_cd ef/gh.ij(kl)mn?op!");
		assertEquals("CdEfGhIjKlMnOp", tableName, "_cd ef/gh.ij(kl)mn?op!");
	}

	/**
	 * Test method for
	 * {@link com.dea42.common.Utils#tabToStr(java.util.ResourceBundle, java.lang.String)}.
	 */
	@Test
	void testTabToStr3() {
		ResourceBundle renames = ResourceBundle.getBundle("renametest");
		String tableName = Utils.tabToStr(renames, "In Last Show / showRSS link");
		assertEquals("InShowRssAs", tableName, "In Last Show / showRSS link");
	}

	/**
	 * Test method for
	 * {@link com.dea42.common.Utils#tabToStr(java.util.ResourceBundle, java.lang.String)}.
	 */
	@Test
	void testTabToStr4() {
		String tableName = Utils.tabToStr(null, "_short");
		assertEquals(tableName, "ShortField");
	}

	/**
	 * Tests method for
	 * {@link com.dea42.common.Utils#getPathAsString(java.lang.String, java.lang.String)}.
	 * and
	 * {@link com.dea42.common.Utils#getPath(java.lang.String, java.lang.String)}.
	 * since getPathAsString just converts getPath return to standardized String
	 */
	@Test
	void testGetPathAsString() {
		String prjRoot = System.getProperty("user.dir").toString().replace('\\', '/');
		if (prjRoot.endsWith("target"))
			prjRoot = prjRoot.substring(0, prjRoot.length() - 7);

		String p = Utils.getPathAsString("src/main/resources/base");
		assertEquals(p, prjRoot + "/src/main/resources/base");
		p = Utils.getPathAsString("../genSpring/src/main/resources/base");
		assertEquals(p, prjRoot + "/src/main/resources/base");
		p = Utils.getPathAsString("../genSpring", "src/main/resources/base");
		assertEquals(p, prjRoot + "/src/main/resources/base");

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
		assertEquals(
				prjRoot.replace("/genSpring", "/genSpringTest")
						+ "/src/main/resources/base/src/test/java/com/dea42/genspring/selenium/SeleniumBase.java.vm",
				p);
		log.debug("passed:" + p);
	}

	/**
	 * Test method for
	 * {@link com.dea42.common.Utils#getRelPath(java.lang.String, java.lang.String)}.
	 */
	@Test
	void testGetRelPath() {
		// Windows
		String rootPath = "D:\\SpringTools4.6.1\\workspace\\genSpring";
		String fullPath = "D:\\SpringTools4.6.1\\workspace\\genSpringTest\\target\\v2j\\src\\test\\java\\com\\dea42\\genspring\\UnitBase.java";
		String expectedPath = "..\\genSpringTest\\target\\v2j\\src\\test\\java\\com\\dea42\\genspring\\UnitBase.java";

		// Linux
		if (File.separatorChar == '/') {
			rootPath = "/home/deabigt/SpringTools4.6.1/workspace/genSpring";
			fullPath = "/home/deabigt/SpringTools4.6.1/workspace/genSpringTest/target/v2j/src/test/java/com/dea42/genspring/UnitBase.java";
			expectedPath = "../genSpringTest/target/v2j/src/test/java/com/dea42/genspring/UnitBase.java";
		}
		String relPathStr = Utils.getRelPath(rootPath, fullPath);
		assertEquals(expectedPath, relPathStr);
	}

}
