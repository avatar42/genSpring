/**
 * 
 */
package com.dea42.build;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.ResourceBundle;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assume;
import org.junit.ComparisonFailure;
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
	// set false to check all the expected values in one run
	private static final boolean stopOnError = false;
	public static final String bundleName = "sheettest";
	public static final String RESOURCE_FOLDER = "src/test/resources";

	/**
	 * Test method for {@link com.dea42.build.Sheets2DB#columnNumberToLetter(int)}.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testColumnNumberToLetter() throws Exception {
		Sheets2DB s = new Sheets2DB();
		String col = s.columnNumberToLetter(104);
		assertEquals("columnNumberToLetter", "CZ", col);
	}

	/**
	 * Test method for
	 * {@link com.dea42.build.Sheets2DB#columnLetterToNumber(java.lang.String)}.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testColumnLetterToNumber() throws Exception {
		Sheets2DB s = new Sheets2DB();
		Integer col = s.columnLetterToNumber("CZ");
		assertEquals("testColumnLetterToNumber", (Integer) 104, col);
	}

	/**
	 * Test method for
	 * {@link com.dea42.build.Sheets2DB#strToCols(java.lang.String)}.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testStrToCols() throws Exception {
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
		} catch (Exception e) {
			log.error("parseDateStr test failed", e);
			fail("parseDateStr test failed");
		}

	}

	private Object chkgetTypedVal(Sheets2DB s, Object val, Class<?> fieldCls, Class<?> expectedCls) throws IOException {
		Object rtn = s.getTypedVal(val, fieldCls);
		assertNotNull("checking getTypedVal(" + val + ", " + fieldCls + ") not null", rtn);
		assertTrue("checking getTypedVal(" + val + ", " + fieldCls + ") instanceof " + expectedCls + " was:"
				+ val.getClass(), rtn.getClass().isAssignableFrom(expectedCls));
		return rtn;
	}

	@Test
	public void testgetTypedVal() throws Exception {
		Sheets2DB s = new Sheets2DB(bundleName, true, true);

		// no type assumed
		Object rtn = s.getTypedVal(null, null);
		assertNull("checking getTypedVal(null, null) returns null", rtn);

		rtn = chkgetTypedVal(s, "Sat 05/03/20 01:03 PM", null, Date.class);
		rtn = chkgetTypedVal(s, "Sat 05/03/20 13:03", null, Date.class);
		rtn = chkgetTypedVal(s, "Sat 5/3/20 1:3 PM", null, Date.class);
		rtn = chkgetTypedVal(s, "5/3/20 1:3 PM", null, Date.class);
		rtn = chkgetTypedVal(s, "202003131000", null, Date.class);
		rtn = chkgetTypedVal(s, "202005232200", null, Date.class);

		rtn = chkgetTypedVal(s, "1:30:00", null, Time.class);
		rtn = chkgetTypedVal(s, "13:30:00", null, Time.class);
		rtn = chkgetTypedVal(s, "1:30:00 PM", null, Time.class);

		rtn = chkgetTypedVal(s, "" + System.currentTimeMillis(), null, Long.class);
		rtn = chkgetTypedVal(s, "123456", null, Integer.class);
		rtn = chkgetTypedVal(s, "1", null, Integer.class);

		rtn = chkgetTypedVal(s, "123456.1", null, BigDecimal.class);

		rtn = chkgetTypedVal(s, new BigDecimal("1"), null, Integer.class);
		rtn = chkgetTypedVal(s, new BigDecimal("" + System.currentTimeMillis()), null, Long.class);
		rtn = chkgetTypedVal(s, new BigDecimal("1.1"), null, BigDecimal.class);

		rtn = chkgetTypedVal(s, "123456-1", null, String.class);

		rtn = chkgetTypedVal(s, Long.parseLong("1"), null, Long.class);

		// type previously set
		rtn = chkgetTypedVal(s, Long.parseLong("1"), String.class, String.class);
		rtn = chkgetTypedVal(s, "202005232200", Date.class, Date.class);

		rtn = chkgetTypedVal(s, new BigDecimal("1"), Integer.class, Integer.class);
		rtn = chkgetTypedVal(s, new BigDecimal("1"), Long.class, Long.class);
		rtn = chkgetTypedVal(s, new BigDecimal("" + System.currentTimeMillis()), Long.class, Long.class);
		rtn = chkgetTypedVal(s, new BigDecimal("" + System.currentTimeMillis()), Integer.class, Long.class);
		rtn = chkgetTypedVal(s, new BigDecimal("1.1"), Long.class, BigDecimal.class);
		rtn = chkgetTypedVal(s, new BigDecimal("1.1"), Integer.class, BigDecimal.class);

		rtn = chkgetTypedVal(s, 1, BigDecimal.class, BigDecimal.class);
		rtn = chkgetTypedVal(s, 1, BigDecimal.class, BigDecimal.class);
		rtn = chkgetTypedVal(s, System.currentTimeMillis(), BigDecimal.class, BigDecimal.class);
		rtn = chkgetTypedVal(s, System.currentTimeMillis(), BigDecimal.class, BigDecimal.class);
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
	 * Run Sheets2DB with Watchlist.properties file and validate the results
	 */
	@Test
	public void testWithWatchlist() throws Exception {
// Note dynamic DB so row count checks may fail
		genDB("Watchlist");
	}

	/**
	 * Check the columns and rows are what we expected.
	 * 
	 * @param db
	 * @param bundle
	 * @param tabName
	 * @throws Exception
	 */
	private String quickChkTable(Db db, ResourceBundle bundle, String tabName) throws Exception {
		StringBuilder rtn = new StringBuilder();
		Sheets2DB s = new Sheets2DB(bundleName, true, true);
		String schema = db.getPrefix();
		ResourceBundle renames = ResourceBundle.getBundle("rename");
		String tableName = Utils.tabToStr(renames, tabName);
		int expectedNumCols = Utils.getProp(bundle, tableName + ".testCols", -1);
		int rows = Utils.getProp(bundle, tableName + ".testRows", -1);
		List<Integer> wantedColNums = s.strToCols(Utils.getProp(bundle, tableName + ".columns"));
		List<Integer> userColNums = s.strToCols(Utils.getProp(bundle, tableName + ".user"));

		try {
			Connection conn = db.getConnection("Sheet2AppTest");
			String query = "SELECT * FROM " + schema + tableName;
			Statement stmt = conn.createStatement();
			stmt.setMaxRows(1);
			log.debug("query=" + query);
			ResultSet rs = stmt.executeQuery(query);
			assertNotNull("Check ResultSet", rs);
			ResultSetMetaData rm = rs.getMetaData();
			int columnCount = rm.getColumnCount();
			int calcCols = 0;
			if (expectedNumCols > -1 && expectedNumCols != columnCount) {
				rtn.append("Checking expected columns in " + schema + tableName);
				if (wantedColNums.size() > 0) {
					calcCols = wantedColNums.size() + 1;
					if (userColNums.size() == 0)
						calcCols++;
					else
						calcCols -= userColNums.size();

					rtn.append(" wantedColNums:" + wantedColNums.size() + " userColNums:" + userColNums.size()
							+ " so might be " + calcCols);
				}
				if (stopOnError)
					assertEquals(rtn.toString(), expectedNumCols, columnCount);
				else if (expectedNumCols != columnCount)
					rtn.append(" expected:" + expectedNumCols + " found:" + columnCount);
			}

			query = "SELECT COUNT(*) FROM " + schema + tableName;
			stmt = conn.createStatement();
			rs = stmt.executeQuery(query);
			assertNotNull("Check ResultSet", rs);
			if (!db.isSQLite())
				rs.next();
			int cnt = rs.getInt(1);
			if (rows > -1 && rows != cnt) {
				rtn.append("Checking expected rows in " + schema + tableName);
				if (stopOnError)
					assertEquals(rtn.toString(), rows, cnt);
				else
					rtn.append(" expected rows:" + rows + " got:" + cnt);
			}
		} catch (SQLException e) {
			log.error("Exception creating DB", e);
			fail("Exception creating DB");
		} finally {
			db.close("Sheet2AppTest");
		}

		return rtn.toString();
	}

	private void genDB(String bundleName) throws Exception {
		Sheets2DB s = new Sheets2DB(bundleName, true, true);
		ResourceBundle bundle = ResourceBundle.getBundle(bundleName);
		assertNotNull("Check DB:" + Utils.getProp(bundle, "db.url", null), s);
		s.getSheet();

		// Validate DB
		chkSQL(bundleName, bundle);
		Db db = new Db("Sheet2AppTest", bundleName);
		StringBuilder sb = new StringBuilder();
		sb.append(quickChkTable(db, bundle, "Account")).append('\n');
		List<String> tables = Utils.getPropList(bundle, CommonMethods.PROPKEY + ".tabs");
		for (String tableName : tables) {
			sb.append(quickChkTable(db, bundle, tableName)).append('\n');
			List<Integer> userColNums = s.strToCols(Utils.getProp(bundle, tableName + ".user"));
			if (!userColNums.isEmpty()) {
				sb.append(quickChkTable(db, bundle, tableName + "User")).append('\n');
			}
		}
		String errors = sb.toString().trim();
		assertTrue(errors, StringUtils.isBlank(errors));
	}

	/**
	 * quick and dirty convert so text compares work on Windows and Linux
	 * 
	 * @param str
	 * @return
	 */
	public String dos2Unix(String str) {
		if (str == null)
			return str;

		return str.replace("\r\n", "\n");
	}

	public void chkSQL(String bundleName, ResourceBundle bundle) throws IOException {
		Path staticPath = Utils.getPath(RESOURCE_FOLDER, bundleName);
		Files.walkFileTree(staticPath, new FileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				return FileVisitResult.CONTINUE;
			}

			/**
			 * Copy file into new tree converting package / paths as needed TODO: change to
			 * use velocityGenerator()
			 */
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (file.toString().endsWith(".sql")) {
					String expected = new String(Files.readAllBytes(file));
					String baseDir = Utils.getProp(bundle, CommonMethods.PROPKEY + ".outdir", "target");
					Path p = Utils.getPath(baseDir, Sheets2DB.SCRIPTS_FOLDER, file.getFileName().toString());
					String actual = new String(Files.readAllBytes(p));
					try {
						assertEquals("Comparing generated and stored " + file.getFileName().toString(), dos2Unix(expected),
								dos2Unix(actual));
					} catch (Throwable e) {
						// debug break point.
						throw e;
					}
				}

				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				return FileVisitResult.CONTINUE;
			}
		});
	}

}
