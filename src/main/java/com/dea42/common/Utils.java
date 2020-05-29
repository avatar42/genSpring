package com.dea42.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class Utils {
	private static final Logger LOGGER = Logger.getLogger(Utils.class);

	private static final List<String> reservedWords = new ArrayList<String>();

	public static synchronized void initReservedWords() {
		if (reservedWords.isEmpty()) {
			reservedWords.add("abstract");
			reservedWords.add("assert");
			reservedWords.add("boolean");
			reservedWords.add("break");
			reservedWords.add("byte");
			reservedWords.add("case");
			reservedWords.add("catch");
			reservedWords.add("char");
			reservedWords.add("class");
			reservedWords.add("const");
			reservedWords.add("continue");
			reservedWords.add("default");
			reservedWords.add("double");
			reservedWords.add("do");
			reservedWords.add("else");
			reservedWords.add("enum");
			reservedWords.add("extends");
			reservedWords.add("false");
			reservedWords.add("final");
			reservedWords.add("finally");
			reservedWords.add("float");
			reservedWords.add("for");
			reservedWords.add("goto");
			reservedWords.add("if");
			reservedWords.add("implements");
			reservedWords.add("import");
			reservedWords.add("instanceof");
			reservedWords.add("int");
			reservedWords.add("interface");
			reservedWords.add("long");
			reservedWords.add("native");
			reservedWords.add("new");
			reservedWords.add("null");
			reservedWords.add("package");
			reservedWords.add("private");
			reservedWords.add("protected");
			reservedWords.add("public");
			reservedWords.add("return");
			reservedWords.add("short");
			reservedWords.add("static");
			reservedWords.add("strictfp");
			reservedWords.add("super");
			reservedWords.add("switch");
			reservedWords.add("synchronized");
			reservedWords.add("this");
			reservedWords.add("throw");
			reservedWords.add("throws");
			reservedWords.add("transient");
			reservedWords.add("true");
			reservedWords.add("try");
			reservedWords.add("void");
			reservedWords.add("volatile");
			reservedWords.add("while");
		}
	}

	/**
	 * 
	 * @param bundle
	 * @param key
	 * @return returns bundle value or null if not found
	 */
	public static String getProp(ResourceBundle bundle, String key) {
		return getProp(bundle, key, null);
	}

	/**
	 * Get a comma separated property as a List<String>
	 * 
	 * @param bundle
	 * @param key
	 * @return
	 */
	public static List<String> getPropList(ResourceBundle bundle, String key) {
		String s = getProp(bundle, key, null);
		if (StringUtils.isBlank(s))
			return null;

		return Arrays.asList(s.split("\\s*,\\s*"));
	}

	/**
	 * 
	 * @param bundle
	 * @param key
	 * @param defaultVal
	 * @return returns bundle value or defaultVal if not found
	 */
	public static String getProp(ResourceBundle bundle, String key, String defaultVal) {
		try {
			return bundle.getString(key);
		} catch (MissingResourceException e) {
			LOGGER.warn(key + " undefined in " + bundle.getBaseBundleName() + " using " + defaultVal);
		}

		return defaultVal;
	}

	public static int getProp(ResourceBundle bundle, String key, int defaultVal) {
		try {
			return Integer.parseInt(bundle.getString(key));
		} catch (MissingResourceException e) {
			LOGGER.warn(key + " undefined in " + bundle.getBaseBundleName() + " using " + defaultVal);
		}

		return defaultVal;
	}

	/**
	 * Lower case objName then remove each non alphanumeric then upper case the char
	 * right after. Note if resulting name is Java keyword then Field is appended.
	 * 
	 * @param renames bundle with alternative names to use or null
	 * @param objName
	 * 
	 * @return String in camel case
	 */
	public static String tabToStr(ResourceBundle renames, String objName) {
		if (objName == null) {
			return null;
		}
		StringBuffer sb = new StringBuffer(objName.length());
		boolean capNext = true;
		for (int i = 0; i < objName.length(); i++) {
			char c = objName.charAt(i);
			if (Character.isDigit(c)) {
				if (capNext)
					capNext = false;
				sb.append(c);
			} else if (Character.isAlphabetic(c)) {
				String s = "" + c;
				if (capNext) {
					sb.append(s.toUpperCase());
					capNext = false;
				} else {
					sb.append(s.toLowerCase());
				}
			} else {
				capNext = true;
			}

		}

		String rtn = sb.toString();
		try {
			if (renames != null && renames.containsKey(rtn))
				return renames.getString(rtn);
		} catch (MissingResourceException e) {

		}

		if (isReserved(rtn)) {
			rtn = rtn + "Field";
		}
		return rtn;
	}

	/**
	 * checks to see if Java reserved word so we do not end up with class or var
	 * names that will not compile
	 * 
	 * @param s
	 * @return
	 */
	public static boolean isReserved(String s) {
		if (s == null)
			return false;

		initReservedWords();
		return reservedWords.contains(s.toLowerCase());
	}
}
