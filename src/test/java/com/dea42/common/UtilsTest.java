/**
 * 
 */
package com.dea42.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.ResourceBundle;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import com.dea42.build.CommonMethods;
import com.dea42.build.Sheets2DBTest;

/**
 * @author avata
 *
 */
@RunWith(BlockJUnit4ClassRunner.class)
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
	 * Test method for
	 * {@link com.dea42.common.Utils#getPath(java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testGetPath() {
		String cwd = System.getProperty("user.dir").toString().replace('\\', '/');
		if (cwd.endsWith("target"))
			cwd = cwd.substring(0, cwd.length() - 7);

		Path p = Utils.getPath("static");
		assertEquals(cwd + "/static", p.toString().replace('\\', '/'));
		// no way to cd in Java. Must run manually in other folder to test in target
		// folder logic
		// TODO: ? % java -cp .:"/Applications/IntelliJ IDEA 13 CE.app/Contents/lib/*"
		// org.junit.runner.JUnitCore UtilsTest
		p = Utils.getPath("../genSpring/static");
		assertEquals(cwd + "/static", p.toString().replace('\\', '/'));

	}

}
