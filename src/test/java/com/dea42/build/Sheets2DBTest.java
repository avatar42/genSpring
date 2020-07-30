/**
 * 
 */
package com.dea42.build;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.ResourceBundle;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dea42.common.Utils;

/**
 * @author avata
 *
 */
public class Sheets2DBTest {
	public static final String bundleName = "sheettest";
	private static final Logger LOGGER = LoggerFactory.getLogger(Sheets2DBTest.class.getName());

	/**
	 * Test method for {@link com.dea42.build.Sheets2DB#columnNumberToLetter(int)}.
	 */
	@Test
	public void testColumnNumberToLetter() {
		Sheets2DB s = new Sheets2DB();
		String col = s.columnNumberToLetter(104);
		assertEquals("columnNumberToLetter", "CZ", col);
	}

	/**
	 * Test method for
	 * {@link com.dea42.build.Sheets2DB#columnLetterToNumber(java.lang.String)}.
	 */
	@Test
	public void testColumnLetterToNumber() {
		Sheets2DB s = new Sheets2DB();
		Integer col = s.columnLetterToNumber("CZ");
		assertEquals("testColumnLetterToNumber", (Integer) 104, col);
	}

	/**
	 * Test method for
	 * {@link com.dea42.build.Sheets2DB#strToCols(java.lang.String)}.
	 */
	@Test
	public void testStrToCols() {
		Sheets2DB s = new Sheets2DB(bundleName, true);
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
		Sheets2DB s = new Sheets2DB(bundleName, true);

		long ms = s.parseDateStr(str);
		Date d = new Date(ms);
		LOGGER.debug(str + " -> " + d.toString());
		assertEquals(str, expected, ms);

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

}
