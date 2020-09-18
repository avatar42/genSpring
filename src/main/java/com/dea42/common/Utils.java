package com.dea42.common;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Scanner;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {
	private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class.getName());

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
	public static String getProp(final ResourceBundle bundle, final String key) {
		return getProp(bundle, key, null);
	}

	/**
	 * Get a comma separated property as a List<String>
	 * 
	 * @param bundle
	 * @param key
	 * @return
	 */
	public static List<String> getPropList(final ResourceBundle bundle, final String key) {
		String s = getProp(bundle, key, null);
		if (StringUtils.isBlank(s))
			return new ArrayList<String>();

		return Arrays.asList(s.split("\\s*,\\s*"));
	}

	/**
	 * 
	 * @param bundle
	 * @param key
	 * @param defaultVal
	 * @return returns bundle value or defaultVal if not found
	 */
	public static String getProp(final ResourceBundle bundle, final String key, final String defaultVal) {
		try {
			return bundle.getString(key);
		} catch (MissingResourceException e) {
			LOGGER.warn(key + " undefined in " + bundle.getBaseBundleName() + " using " + defaultVal);
		}

		return defaultVal;
	}

	/**
	 * 
	 * @param bundle
	 * @param key
	 * @param defaultVal
	 * @return returns bundle value or defaultVal if not found
	 */
	public static int getProp(final ResourceBundle bundle, final String key, final int defaultVal) {
		try {
			return Integer.parseInt(bundle.getString(key));
		} catch (MissingResourceException e) {
			LOGGER.warn(key + " undefined in " + bundle.getBaseBundleName() + " using " + defaultVal);
		}

		return defaultVal;
	}

	/**
	 * 
	 * @param bundle
	 * @param key
	 * @param defaultVal
	 * @return returns bundle value or defaultVal if not found
	 */
	public static boolean getProp(final ResourceBundle bundle, final String key, final boolean defaultVal) {
		try {
			return Boolean.parseBoolean(bundle.getString(key));
		} catch (MissingResourceException e) {
			LOGGER.warn(key + " undefined in " + bundle.getBaseBundleName() + " using " + defaultVal);
		}

		return defaultVal;
	}

	/**
	 * 
	 * @param bundle
	 * @param key
	 * @param defaultVal
	 * @return returns bundle value or defaultVal if not found
	 */
	public static long getProp(ResourceBundle bundle, String key, long defaultVal) {
		try {
			return Long.parseLong(bundle.getString(key));
		} catch (MissingResourceException e) {
			LOGGER.warn(key + " undefined in " + bundle.getBaseBundleName() + " using " + defaultVal);
		}

		return defaultVal;
	}

	/**
	 * 
	 * @param bundleName
	 * @param key
	 * @param defaultVal
	 * @return returns bundle (named bundleName) value or defaultVal if not found
	 */
	public static boolean getProp(final String bundleName, final String key, final boolean defaultVal) {
		ResourceBundle bundle = ResourceBundle.getBundle(bundleName);

		return getProp(bundle, key, defaultVal);
	}

	/**
	 * 
	 * @param bundleName
	 * @param key
	 * @param defaultVal
	 * @return returns bundle (named bundleName) value or defaultVal if not found
	 */
	public static String getProp(final String bundleName, final String key, final String defaultVal) {
		ResourceBundle bundle = ResourceBundle.getBundle(bundleName);

		return getProp(bundle, key, defaultVal);
	}

	/**
	 * Lower case objName then remove each non alphanumeric then upper case the char
	 * right after. Note if resulting name is Java keyword then Field is appended.
	 * If starts with a digit an N is prepended
	 * 
	 * @param renames bundle with alternative names to use or null
	 * @param objName
	 * 
	 * @return String in camel case
	 */
	public static String tabToStr(final ResourceBundle renames, final String objName) {
		if (objName == null) {
			return null;
		}
		StringBuffer sb = new StringBuffer(objName.length());
		// Names can not start with number
		if (Character.isDigit(objName.charAt(0))) {
			sb.append('N');
		}
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
	public static boolean isReserved(final String s) {
		if (s == null)
			return false;

		initReservedWords();
		return reservedWords.contains(s.toLowerCase());
	}

	/**
	 * Convert ResourceBundle into a Properties object.
	 *
	 * @param resource a resource bundle to convert.
	 * @return Properties a properties version of the resource bundle.
	 */
	public static Properties convertResourceBundleToProperties(final ResourceBundle resource) {
		Properties properties = new Properties();
		Enumeration<String> keys = resource.getKeys();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			properties.put(key, resource.getString(key));
		}
		return properties;
	}

	/**
	 * Returns a path that is this path with redundant name elements eliminated.
	 * Adds "../" to path if currently in target folder to deal with diffs in
	 * running from direct and as embedded maven install test
	 * 
	 * @param path the path string or initial part of the path string
	 * @param more additional strings to be joined to form the path string
	 * @return the resulting path or this path if it does not contain redundant name
	 *         elements; an empty path is returned if this path does have a root
	 *         component and all name elements are redundant
	 * 
	 */
	public static Path getPath(final String path, final String... more) {
		// toAbsolutePath() required for getParent() to work
		Path cwd = Paths.get(".").toAbsolutePath();
		String tpath = path;
		// check for in target folder. If so make rel to parent
		if (cwd.getParent().endsWith("target")) {
			tpath = cwd.getParent().getParent() + "/" + path;
		}

		return Paths.get(tpath, more).toAbsolutePath().normalize();
	}

	public static int runCmd(final String cmd, final String outdir) {
		int exitValue = -1;
		LOGGER.error("Now doing a maven install of project in " + outdir);

		File prjFold = Utils.getPath(outdir).toFile();
		// Execute a command and get its process handle
		try {
			Process proc = Runtime.getRuntime().exec(cmd, null, prjFold);
			logIO(proc.getInputStream(), false);
			logIO(proc.getErrorStream(), true);
			// Wait for process to terminate and catch any Exceptions.
			try {
				exitValue = proc.waitFor();
			} catch (InterruptedException e) {
				LOGGER.error("Process was interrupted");
			}

		} catch (IOException e) {
			LOGGER.error("Error running:" + cmd, e);
		}

		LOGGER.info("Exit status:" + exitValue + " for:" + cmd);
		return exitValue;
	}

	private static void logIO(final InputStream src, final boolean isError) {
		new Thread(new Runnable() {
			public void run() {
				Scanner sc = new Scanner(src);
				while (sc.hasNextLine()) {
					if (isError)
						LOGGER.error(sc.nextLine());
					else
						LOGGER.info(sc.nextLine());

				}
				sc.close();
			}
		}).start();
	}

}
