/**
 * 
 */
package com.dea42.build;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.ResourceBundle;

import org.junit.Assume;
import org.junit.Test;

import com.dea42.common.Db;
import com.dea42.common.Utils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author avata
 *
 */
@Slf4j
public class Sheets2DBTest {
	public static final String bundleName = "sheettest";

	/**
	 * Test method for {@link com.dea42.build.Sheets2DB#columnNumberToLetter(int)}.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testColumnNumberToLetter() throws IOException {
		Sheets2DB s = new Sheets2DB();
		String col = s.columnNumberToLetter(104);
		assertEquals("columnNumberToLetter", "CZ", col);
	}

	/**
	 * Test method for
	 * {@link com.dea42.build.Sheets2DB#columnLetterToNumber(java.lang.String)}.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testColumnLetterToNumber() throws IOException {
		Sheets2DB s = new Sheets2DB();
		Integer col = s.columnLetterToNumber("CZ");
		assertEquals("testColumnLetterToNumber", (Integer) 104, col);
	}

	/**
	 * Test method for
	 * {@link com.dea42.build.Sheets2DB#strToCols(java.lang.String)}.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testStrToCols() throws IOException {
		Sheets2DB s = new Sheets2DB(bundleName, true, true);
		ResourceBundle bundle = ResourceBundle.getBundle(bundleName);
		String cols = Utils.getProp(bundle, "shows.columns", "A-I,Q-T,BC-BF");
		List<Integer> list = s.strToCols(cols);
		Integer[] expecteds = new Integer[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 16, 17, 18, 19, 54, 55, 56, 57 };
		for (Integer expected : expecteds) {
			assertTrue("Looking for " + expected + " in results", list.contains(expected));
		}

		list = s.strToCols("A-C,E,CW-CZ");
		expecteds = new Integer[] { 0, 1, 2, 4, 100, 101, 102, 103, 103 };
		for (Integer expected : expecteds) {
			assertTrue("Looking for " + expected + " in results", list.contains(expected));
		}

		list = s.strToCols("");
		assertNotNull("passing empty string", list);
		assertTrue("passing empty string", list.isEmpty());
	}

	private void parseDateStr(String str, long expected) {
		try {
			Sheets2DB s = new Sheets2DB(bundleName, true, true);

			long ms = s.parseDateStr(str);
			Date d = new Date(ms);
			log.debug(str + " -> " + d.toString());
			assertEquals(str, expected, ms);
		} catch (IOException e) {
			log.error("parseDateStr test failed", e);
			fail("parseDateStr test failed");
		}

	}

	/**
	 * Test method for
	 * {@link com.dea42.build.Sheets2DB#parseDateStr(java.lang.String)}.
	 */
	@Test
	public void testParseDateStr() {
		/*
		 * Sat 5/23/20 10:00 PM 1:30:00 05-23-2020 202005232200
		 */
		GregorianCalendar gc = new GregorianCalendar(2020, 4, 3, 13, 3);

		parseDateStr("Sat 05/03/20 01:03 PM", gc.getTimeInMillis());
		parseDateStr("Sat 05/03/20 13:03", gc.getTimeInMillis());
		parseDateStr("Sat 5/3/20 1:3 PM", gc.getTimeInMillis());
		parseDateStr("5/3/20 1:3 PM", gc.getTimeInMillis());
		parseDateStr("202005031303", gc.getTimeInMillis());

		gc.set(Calendar.HOUR_OF_DAY, 0);
		gc.set(Calendar.MINUTE, 0);
		parseDateStr("05-03-2020", gc.getTimeInMillis());

		gc = new GregorianCalendar(2020, 4, 23, 22, 00);
		parseDateStr("202005232200", gc.getTimeInMillis());

		gc = new GregorianCalendar(1970, 0, 1, 1, 30);
		parseDateStr("1:30:00", gc.getTimeInMillis());

		gc = new GregorianCalendar(1970, 0, 1, 13, 30);
		parseDateStr("13:30:00", gc.getTimeInMillis());
		parseDateStr("1:30:00 PM", gc.getTimeInMillis());
	}

	/**
	 * Run Sheets2DB with genSpringTest.properties file and validate the results
	 */
	@Test
	public void testWithgenSpringTest() throws Exception {
		genDB("genSpringTest");

	}

	/**
	 * Run Sheets2DB with genSpringMySQLTest.properties file and validate the
	 * results. Note will skip if enable=false in properties file.
	 */
	@Test
	public void testWithgenSpringMySQLTest() throws Exception {
		Assume.assumeTrue(Utils.getProp("genSpringMySQLTest", "enabled", false));
		genDB("genSpringMySQLTest");

	}

	/**
	 * Run Sheets2DB with genSpringMSSQLTest.properties file and validate the
	 * results. Note will skip if enable=false in properties file.
	 */
	@Test
	public void testWithgenSpringMSSQLTest() throws Exception {
		Assume.assumeTrue(Utils.getProp("genSpringMSSQLTest", "enabled", false));
		// Drivers check
//		com.microsoft.sqlserver.jdbc.SQLServerDriver sQLServerDriver;

		genDB("genSpringMSSQLTest");

	}

	/**
	 * Run Sheets2DB with genSpringTest2.properties file and validate the results
	 */
	@Test
	public void testWithgenSpringTest2() throws Exception {

		genDB("genSpringTest2");
	}

	/**
	 * Check the columns and rows are what we expected.
	 * 
	 * @param db
	 * @param bundle
	 * @param tableName
	 */
	private void quickChkTable(Db db, ResourceBundle bundle, String tableName) {
		String schema = db.getPrefix();

		int columns = Utils.getProp(bundle, tableName + ".testCols", 0);
		int rows = Utils.getProp(bundle, tableName + ".testRows", 0);
		try {
			Connection conn = db.getConnection("Sheet2AppTest");
			String query = "SELECT * FROM " + schema + tableName;
			Statement stmt = conn.createStatement();
			stmt.setMaxRows(1);
			log.debug("query=" + query);
			ResultSet rs = stmt.executeQuery(query);
			assertNotNull("Check ResultSet", rs);
			ResultSetMetaData rm = rs.getMetaData();
			int size = rm.getColumnCount();
			assertEquals("Checking expected columns in " + schema + tableName, columns, size);

			query = "SELECT COUNT(*) FROM " + schema + tableName;
			stmt = conn.createStatement();
			rs = stmt.executeQuery(query);
			assertNotNull("Check ResultSet", rs);
			if (!db.isSQLite())
				rs.next();
			assertEquals("Checking expected rows in " + schema + tableName, rows, rs.getInt(1));
		} catch (SQLException e) {
			log.error("Exception creating DB", e);
			fail("Exception creating DB");
		} finally {
			db.close("Sheet2AppTest");
		}

	}

	private void genDB(String bundleName) throws Exception {
		Sheets2DB s = new Sheets2DB(bundleName, true, true);
		s.getSheet();

		// Validate DB
		ResourceBundle bundle = ResourceBundle.getBundle(bundleName);
		String outdir = Utils.getProp(bundle, CommonMethods.PROPKEY + ".outdir", ".");
		Db db = new Db("Sheet2AppTest", bundleName);

		quickChkTable(db, bundle, "Account");
		List<String> tables = Utils.getPropList(bundle, CommonMethods.PROPKEY + ".tabs");
		for (String tableName : tables) {
			quickChkTable(db, bundle, tableName);
			List<Integer> userColNums = s.strToCols(Utils.getProp(bundle, tableName + ".user"));
			if (!userColNums.isEmpty()) {
				quickChkTable(db, bundle, tableName + "User");
			}
		}
	}
}
