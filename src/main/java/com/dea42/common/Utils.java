package com.dea42.common;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Utils {

	private static final List<String> reservedWords = new ArrayList<String>();

	/**
	 * A list of Java and SQL key words that might cause issues if used as field
	 * names
	 */
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
			reservedWords.add("constraint");
			reservedWords.add("continue");
			reservedWords.add("date");
			reservedWords.add("datetime");
			reservedWords.add("decimal");
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
			reservedWords.add("integer");
			reservedWords.add("interface");
			reservedWords.add("long");
			reservedWords.add("native");
			reservedWords.add("new");
			reservedWords.add("not");
			reservedWords.add("number");
			reservedWords.add("null");
			reservedWords.add("package");
			reservedWords.add("primary");
			reservedWords.add("private");
			reservedWords.add("protected");
			reservedWords.add("public");
			reservedWords.add("real");
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
			reservedWords.add("unique");
			reservedWords.add("varchar");
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

		return new ArrayList<String>(Arrays.asList(s.split("\\s*,\\s*")));
	}

	public static Map<String, String> getPropMap(final ResourceBundle bundle, final String key) {
		Map<String, String> rtn = new HashMap<String, String>();
		String s = getProp(bundle, key, null);
		if (!StringUtils.isBlank(s)) {
			List<String> tmp = Arrays.asList(s.split("\\s*,\\s*"));
			for (String pair : tmp) {
				String[] set = pair.split("\\s*:\\s*");
				if (set.length == 2)
					rtn.put(set[0], set[1]);
				else
					throw new PatternSyntaxException(tmp + " is invalid syntax", "\\s*:\\s*", 0);
			}
		}

		return rtn;

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
			log.warn(key + " undefined in " + bundle.getBaseBundleName() + " using " + defaultVal);
		}

		return defaultVal;
	}

	/**
	 * 
	 * @param bundle
	 * @param key
	 * @param defaultVal
	 * @return returns bundle value as Class or defaultVal if not found
	 */
	public static Class<?> getPropCls(final ResourceBundle bundle, final String key, final Class<?> defaultVal) {
		try {
			return Class.forName(bundle.getString(key));
		} catch (MissingResourceException e) {
			log.warn(key + " undefined in " + bundle.getBaseBundleName() + " using " + defaultVal);
		} catch (ClassNotFoundException e) {
			log.warn(key + " has invalid value in " + bundle.getBaseBundleName() + " using " + defaultVal);
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
			log.warn(key + " undefined in " + bundle.getBaseBundleName() + " using " + defaultVal);
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
			log.warn(key + " undefined in " + bundle.getBaseBundleName() + " using " + defaultVal);
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
			log.warn(key + " undefined in " + bundle.getBaseBundleName() + " using " + defaultVal);
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
		if (StringUtils.isBlank(objName)) {
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
		// upper case the U in User tables.
		if (rtn.endsWith("user")) {
			rtn = rtn.substring(0, rtn.length() - 4) + "User";
		}
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
	 * checks to see if Java or SQL reserved word so we do not end up with class or
	 * var names that will not compile or SQL that will not run.
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
	 * Returns a path that is this path with redundant name elements eliminated. If
	 * System.getProperty("user.dir") ends in target folder uses parent to deal with
	 * diffs in running from inside Eclipse and as embedded maven install test
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
		Path cwd = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
		// check for in target folder. If so make rel to parent
		if (cwd.endsWith("target")) {
			cwd = cwd.getParent().toAbsolutePath();
		}
		Path p;
		if (more.length == 0) {
			p = Paths.get(path, more);
		} else {
			// try to deal with an absolute path passed in more
			List<String> m = new ArrayList<String>(more.length);
			for (int i = 0; i < more.length; i++) {
				String fs = more[i];
				if (fs.startsWith("/") || (fs.length() > 1 && fs.charAt(1) == ':')) {
					fs = getRelPath(cwd.toString(), fs);
				}
				m.add(i, fs);
			}
			p = Paths.get(path, m.toArray(more));
		}

		Path rtn = cwd.resolve(p).toAbsolutePath().normalize();
		return rtn;
//		return Paths.get(tpath, more).toAbsolutePath().normalize();
	}

	/**
	 * Calls getPath(final String path, final String... more) then
	 * .toString().replace('\\', '/') to convert to String
	 * 
	 * @see getPath(String, String...)
	 * 
	 * @param path
	 * @param more
	 * @return
	 */
	public static String getPathAsString(final String path, final String... more) {
		return getPath(path, more).toString().replace('\\', '/');
	}

	public static String getRelPath(final String base, final String rel) {
		Path basePath = Paths.get(base).toAbsolutePath();
		Path childPath = Paths.get(rel).toAbsolutePath();
		Path relPath = basePath.relativize(childPath);
		return relPath.toString();
	}

	/**
	 * Creates all the parent folders as needed and an empty file. It returns null
	 * if the old file exists.
	 * 
	 * @param baseDir path of folder to use as root for relPath. If ends in target
	 *                then target is removed.
	 * @param relPath paths of file to create
	 * @return
	 */
	public static Path createFile(String baseDir, String... relPath) {
		Path p = getPath(baseDir, relPath);
//		if (p.toFile().exists()) {
//			p.toFile().delete();
//		}
		try {
			Files.createDirectories(p.getParent());
			p = Files.createFile(p);
		} catch (IOException e) {
			log.warn(e.getMessage() + " exists will skip");
			return null;
		}

		return p;
	}

	public static int runCmd(final String cmd, final String outdir) {
		int exitValue = -1;
		log.error("Now doing a maven install of project in " + outdir);

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
				log.error("Process was interrupted");
			}

		} catch (IOException e) {
			log.error("Error running:" + cmd, e);
		}

		log.info("Exit status:" + exitValue + " for:" + cmd);
		return exitValue;
	}

	private static void logIO(final InputStream src, final boolean isError) {
		new Thread(new Runnable() {
			public void run() {
				Scanner sc = new Scanner(src);
				while (sc.hasNextLine()) {
					if (isError)
						log.error(sc.nextLine());
					else
						log.info(sc.nextLine());

				}
				sc.close();
			}
		}).start();
	}

	/**
	 * Delete the path
	 * 
	 * @param path
	 * @throws IOException is still exists after delete attempt
	 */
	public static void deletePath(Path path) throws IOException {
		if (path.toFile().exists()) {
			if (path.toFile().isDirectory()) {
				Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						if (!file.endsWith(".sqlite")) {
							log.debug("Deleting file:" + file);
							Files.delete(file);
						}
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
						// try to delete the file anyway, even if its attributes
						// could not be read, since delete-only access is
						// theoretically possible
						if (file.toFile().exists()) {
							log.debug("Deleting file again:" + file);
							Files.delete(file);
						}
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
						if (exc == null) {
							if (!path.equals(dir)) {
								log.debug("Deleting dir:" + dir);
								try {
									Files.delete(dir);
								} catch (IOException e) {
									// see an occasional race condition so give second try after a sec
									try {
										Thread.sleep(100);
									} catch (InterruptedException e1) {
										// ignored
									}
									Files.delete(dir);
								}
							}
							return FileVisitResult.CONTINUE;
						} else {
							// directory iteration failed; propagate exception
							// throw exc;
							// Windows seems to be slow noticing we've removed everything from the folder
							log.error("Ignoring failed to delete folder", exc);
							return FileVisitResult.CONTINUE;
						}
					}
				});
			}
			Files.delete(path);
		}
		if (path.toFile().exists()) {
			throw new IOException(path + " still exists");
		} else {
			log.debug("Deleted:" + path.toAbsolutePath());
		}
	}

}
