/**
 * 
 */
package com.dea42.build;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import lombok.extern.slf4j.Slf4j;

/**
 * Note since this test converts files from generated apps and and compares
 * generated files to generated apps it will fail if those regressions have not
 * been run yet. If fails run after regression tests to confirm true error.
 * 
 * @author avata
 *
 */
@Slf4j
public class Java2VMTest {
	private static final String TEST_WITH_LONG_IDS = "genSpringMSSQLTest";
	private static final String TEST_WITH_INT_IDS = "genSpringTest";
	private static final String TEST_WITH_ALT_OPTIONS = "genSpringTest2";

	private String rel = "";

	public Java2VMTest() {
		// adjust paths if run from maven vs eclipse
		Path cwd = Paths.get(".").toAbsolutePath();
		if (cwd.getParent().endsWith("target")) {
			rel = "../";
		}
	}

	/**
	 * Convert file to Velocity template and compare with current template. Note
	 * useful for making templates from modified regression test generated apps to
	 * pull mods back in.
	 * 
	 * @param bundleName
	 * @param file
	 * @throws IOException
	 */
	public void doJava2vm(String bundleName, String file) throws IOException {
		Java2VM j = new Java2VM(bundleName);
		String srcFile = j.getBaseDir() + file.replace("com/dea42/genspring",j.getBasePath());
		String outFile = "target/base" + file + ".vm";
		j.java2vm(srcFile, outFile);

		String expected = new String(Files.readAllBytes(Paths.get(Java2VM.TEMPLATE_FOLDER + file + ".vm")));
		String actual = new String(Files.readAllBytes(Paths.get(outFile)));

		log.debug("compare:" + expected.compareTo(actual));
		assertEquals("Compare of " + file + ".vm to ref", expected, actual);
	}

	/**
	 * 
	 * @param bundleName
	 * @param fileStr
	 * @throws IOException
	 */
	public void dovm2java(String bundleName, String fileStr) throws IOException {

		File f = new File(fileStr);
		assertTrue("Check exists:" + fileStr, f.exists());

		Java2VM j = new Java2VM(bundleName);
		String rfile = j.getResourcePathString(fileStr);
		InputStream stream = getClass().getResourceAsStream("/" + rfile);
		ClassLoader cl = getClass().getClassLoader();

		URL[] urls = ((URLClassLoader) cl).getURLs();

		for (URL url : urls) {
			File fcp = new File(url.getFile() + rfile);
			if (fcp.exists()) {
				log.debug("FOUND:" + fcp.getAbsolutePath());
			}
		}

		assertNotNull("Checking classpath for:" + rfile, stream);

		String relOutFile = rfile.substring(5, rfile.length() - 3);
		String outFile = rel + "target/" + relOutFile;
		j.vm2java(fileStr, outFile);

		String expected = new String(Files.readAllBytes(Paths.get(rel + "../" + bundleName + "/" + relOutFile)));
		String actual = new String(Files.readAllBytes(Paths.get(outFile)));

		log.debug("compare:" + expected.compareTo(actual));
		assertEquals("Compare of " + outFile + " to ref " + rel + "../" + bundleName + "/" + relOutFile, expected,
				actual);
	}

	@Test
	public void testgetResourcePathString() throws IOException {
		Java2VM j = new Java2VM(TEST_WITH_INT_IDS);

		String expected = "base/src/test/java/com/dea42/genspring/UnitBase.java.vm";
		String actual = j.getResourcePathString(
				rel + "src/main/resources/base/src/test/java/com/dea42/genspring/UnitBase.java.vm");
		assertEquals("Checking relative:", expected, actual);

		actual = j.getResourcePathString(
				"D:\\SpringTools4.6.1\\workspace\\genSpring\\src\\main\\resources\\base\\src\\test\\java\\com\\dea42\\genspring\\UnitBase.java.vm");
		assertEquals("Checking relative:", expected, actual);

		actual = j.getResourcePathString(
				rel + "../genSpring/src/main/resources/base/src/test/java/com/dea42/genspring/UnitBase.java.vm");
		assertEquals("Checking relative:", expected, actual);
	}

	/**
	 * Test method for
	 * {@link com.dea42.build.Java2VM#java2vm(java.lang.String, java.lang.String)}.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testVm2JavaUnitBase() throws IOException {
		dovm2java(TEST_WITH_INT_IDS, Java2VM.TEMPLATE_FOLDER + "/src/test/java/com/dea42/genspring/UnitBase.java.vm");
	}

	/**
	 * Test method for
	 * {@link com.dea42.build.Java2VM#java2vm(java.lang.String, java.lang.String)}.
	 * Quick test
	 * 
	 * @throws IOException
	 */
	@Test
	public void testJava2vmUnitBase() throws IOException {
		doJava2vm(TEST_WITH_INT_IDS, "/src/test/java/com/dea42/genspring/UnitBase.java");
	}

	/**
	 * Test method for
	 * {@link com.dea42.build.Java2VM#java2vm(java.lang.String, java.lang.String)}.
	 * with int IDs
	 * 
	 * @throws IOException
	 */
	@Test
	public void testJava2vmAppControllerTest() throws IOException {
		doJava2vm(TEST_WITH_INT_IDS, "/src/test/java/com/dea42/genspring/controller/AppControllerTest.java");
	}

	/**
	 * Test method for
	 * {@link com.dea42.build.Java2VM#java2vm(java.lang.String, java.lang.String)}.
	 * with long IDs
	 * 
	 * @throws IOException
	 */
	@Test
	public void testJava2vmAppControllerTest4() throws IOException {
		doJava2vm(TEST_WITH_LONG_IDS, "/src/test/java/com/dea42/genspring/controller/AppControllerTest.java");
	}

	/**
	 * Test method for
	 * {@link com.dea42.build.Java2VM#java2vm(java.lang.String, java.lang.String)}.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testJava2vmSeleniumBase() throws IOException {
		doJava2vm(TEST_WITH_INT_IDS, "/src/test/java/com/dea42/genspring/selenium/SeleniumBase.java");
	}

	/**
	 * Test method for
	 * {@link com.dea42.build.Java2VM#java2vm(java.lang.String, java.lang.String)}.
	 * with pom.xml file and alt options
	 * 
	 * @throws IOException
	 */
	@Test
	public void testJava2vmPom2() throws IOException {
		doJava2vm(TEST_WITH_INT_IDS, "/pom.xml");
	}

	/**
	 * Test method for
	 * {@link com.dea42.build.Java2VM#java2vm(java.lang.String, java.lang.String)}.
	 * with pom.xml file
	 * 
	 * @throws IOException
	 */
	@Test
	public void testJava2vmPom() throws IOException {
		doJava2vm(TEST_WITH_INT_IDS, "/pom.xml");
	}

	/**
	 * Test method for
	 * {@link com.dea42.build.Java2VM#java2vm(java.lang.String, java.lang.String)}.
	 * with alt options
	 * 
	 * @throws IOException
	 */
	@Test
	public void testJava2vmUnitBase2() throws IOException {
		doJava2vm(TEST_WITH_ALT_OPTIONS, "/src/test/java/com/dea42/genspring/UnitBase.java");
	}

	/**
	 * Test method for
	 * {@link com.dea42.build.Java2VM#java2vm(java.lang.String, java.lang.String)}.
	 * with longs
	 * 
	 * @throws IOException
	 */
	@Test
	public void testJava2vmUnitBase4() throws IOException {
		doJava2vm(TEST_WITH_LONG_IDS, "/src/test/java/com/dea42/genspring/UnitBase.java");
	}

	/**
	 * Mainly for testing out regexs to be added to java2vm()
	 * 
	 * @throws IOException
	 */
	@Test
	public void testp() throws IOException {
		String idPrim = "int";
		String data = "	protected static int TEST_USER_ID;\r\n" + "	protected static String TEST_USER;\r\n"
				+ "	protected static String TEST_PASS;\r\n" + "	protected static String TEST_ROLE;\r\n" + "\r\n"
				+ "	protected static int ADMIN_USER_ID;\r\n" + "	protected static String ADMIN_USER;\r\n"
				+ "	protected static String ADMIN_PASS;\r\n" + "	protected static String ADMIN_ROLE;\r\n" + "";
		Pattern p = Pattern.compile("(" + idPrim + ") ([A-Z,_]*)_ID;");
		Matcher m = p.matcher(data);
		log.debug("matcher:" + m.toString());
//		assertEquals("groupCount:", 2, m.groupCount());
		log.debug("find:" + m.find());
//		assertTrue("find:", m.find());
		data = m.replaceAll("\\${idPrim} $2_ID;");
		log.debug(data);
		assertTrue("data", data.contains("${idPrim}"));
	}

	/**
	 * Used to get new templates and other files from modified genSpringTest files.
	 * Lists tests where files changed
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(final String[] args) throws Exception {
		try {
			Java2VM j = new Java2VM(TEST_WITH_INT_IDS);
			j.genTemplates();
		} catch (IOException e) {
			e.printStackTrace();
		}
//		Utils.deletePath(Paths.get("target", "base"));
//		JUnitCore junit = new JUnitCore();
//		String[] tests = { "testJava2vmPom", "testJava2vmUnitBase", "testJava2vmSeleniumBase" };
//		try {
//			int cnt = 0;
//			List<String> chged = new ArrayList<String>();
//			for (String test : tests) {
//				cnt++;
//				Request req = Request.method(Java2VMTest.class, test);
//				final Result result = junit.run(req);
//				// Check result.getFailures etc.
//				if (!result.wasSuccessful()) {
//					String error = result.getFailures().toString();
//					log.warn(test + ":" + error);
//					if (error.contains("No tests found matching Method")) {
//						throw new Exception(error);
//					} else {
//						chged.add(test);
//					}
//				}
//			}
//			log.debug("Converted " + cnt + " files, of them these appear to have changed or failed:" + chged);
//		} catch (Exception e) {
//			log.error("bad test", e);
//			for (final Method method : Java2VMTest.class.getDeclaredMethods()) {
//				Class<? extends Annotation> annotation = Test.class;
//				if (method.isAnnotationPresent(annotation) && method.getName().startsWith("testJava2vm")) {
//					log.error("Test:" + method.getName());
//				}
//			}
//		}
	}

}
