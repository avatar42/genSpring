/**
 * 
 */
package com.dea42.build;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

import org.junit.Assume;
import org.junit.Test;

import com.dea42.common.Utils;

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
	 * Use the java2vm to convert file pointed to be fileStr to a template then
	 * check the expectedText(s) are in the generated template.
	 * 
	 * @param java2vm
	 * @param file
	 * @param bundleName
	 * @throws IOException
	 */
	public void doJava2vm(Java2VM java2vm, String file, String... expectedText) throws IOException {
		assertFalse("Found absolute path:" + file, file.charAt(0) == '/');
		Path srcFile = Utils.getPath(java2vm.getBaseDir(), file.replace("com/dea42/genspring", java2vm.getBasePath()));
		Assume.assumeTrue("Checking " + srcFile + " does not exists", srcFile.toFile().exists());
		Path outPath = Utils.getPath(java2vm.getBaseDir(), "target/base", file + ".vm");
		Utils.deletePath(outPath);
		java2vm.java2vm(srcFile.toString(), outPath.toString());

		String actual = new String(Files.readAllBytes(outPath));
		for (String expected : expectedText) {
			assertTrue("Looking for " + expected + " in " + outPath.toAbsolutePath(), actual.contains(expected));
		}
		log.debug("Created:" + outPath);
	}

	/**
	 * Use the java2vm to convert file pointed to be templatefilePathStr to a
	 * template then check the expectedText(s) are in the generated template.
	 * 
	 * @param java2vm
	 * @param templatefilePathStr
	 * @param expectedText
	 * 
	 * @throws IOException
	 */
	public void dovm2java(Java2VM java2vm, String templatefilePathStr, String... expectedText) throws IOException {

		Path srcPath = Utils.getPath(templatefilePathStr);
		assertTrue("Check exists:" + templatefilePathStr, srcPath.toFile().exists());

		String rfile = java2vm.getResourcePathString(srcPath.toString());
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

		Path outPath = Utils.getPath(java2vm.getBaseDir(), "target/v2j/", rfile.substring(5, rfile.length() - 3));
		Utils.deletePath(outPath);

		java2vm.vm2java(srcPath.toString(), outPath.toString());

		String actual = new String(Files.readAllBytes(outPath));
		for (String expected : expectedText) {
			assertTrue("Looking for " + expected + " in " + outPath.toAbsolutePath(), actual.contains(expected));
		}
	}

	@Test
	public void testgetResourcePathString() throws Exception {
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
	 * @throws Exception
	 */
	@Test
	public void testVm2JavaUnitBase() throws Exception {
		Java2VM j = new Java2VM(TEST_WITH_INT_IDS);
		dovm2java(j, Java2VM.TEMPLATE_FOLDER + "/src/test/java/com/dea42/genspring/UnitBase.java.vm",
				"package com.dea42.genspring;", "protected static int ADMIN_USER_ID",
				"ADMIN_USER_ID = Utils.getProp(appBundle, \"default.adminid\", 2);",
				"public static String asJsonString(final Object obj)");
	}

	/**
	 * Test method for
	 * {@link com.dea42.build.Java2VM#java2vm(java.lang.String, java.lang.String)}.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testVm2JavaUnitBase2() throws Exception {
		Java2VM j = new Java2VM(TEST_WITH_ALT_OPTIONS);
		dovm2java(j, Java2VM.TEMPLATE_FOLDER + "/src/test/java/com/dea42/genspring/UnitBase.java.vm",
				"package dea.example.regression;", "protected static int ADMIN_USER_ID",
				"ADMIN_USER_ID = Utils.getProp(appBundle, \"default.adminid\", 2);",
				"public static String asJsonString(final Object obj)");
	}

	/**
	 * Test method for
	 * {@link com.dea42.build.Java2VM#java2vm(java.lang.String, java.lang.String)}.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testVm2JavaUnitBase4() throws Exception {
		Java2VM j = new Java2VM(TEST_WITH_LONG_IDS);
		dovm2java(j, Java2VM.TEMPLATE_FOLDER + "/src/test/java/com/dea42/genspring/UnitBase.java.vm",
				"package com.dea42.genspring;", "protected static long ADMIN_USER_ID",
				"ADMIN_USER_ID = Utils.getProp(appBundle, \"default.adminid\", 2l);",
				"public static String asJsonString(final Object obj)");
	}

	/**
	 * Test method for
	 * {@link com.dea42.build.Java2VM#java2vm(java.lang.String, java.lang.String)}.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testVm2JavaPom() throws Exception {
		Java2VM j = new Java2VM(TEST_WITH_INT_IDS);

		dovm2java(j, Java2VM.TEMPLATE_FOLDER + "/pom.xml.vm", "<groupId>" + j.getBaseGroupId() + "</groupId>",
				"<artifactId>" + j.getBaseArtifactId() + "</artifactId>",
				"<version>" + j.getGenSpringVersion() + "-SNAPSHOT</version>",
				"<description>" + j.getAppDescription() + "</description>");
	}

	/**
	 * Test method for
	 * {@link com.dea42.build.Java2VM#java2vm(java.lang.String, java.lang.String)}.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testVm2JavaPom2() throws Exception {
		Java2VM j = new Java2VM(TEST_WITH_LONG_IDS);

		dovm2java(j, Java2VM.TEMPLATE_FOLDER + "/pom.xml.vm", "<groupId>" + j.getBaseGroupId() + "</groupId>",
				"<artifactId>" + j.getBaseArtifactId() + "</artifactId>",
				"<version>" + j.getAppVersion() + "-SNAPSHOT</version>",
				"<description>" + j.getAppDescription() + "</description>");
	}

	/**
	 * Test method for
	 * {@link com.dea42.build.Java2VM#java2vm(java.lang.String, java.lang.String)}.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testVm2JavaREADME() throws Exception {
		Java2VM j = new Java2VM(TEST_WITH_INT_IDS);
		dovm2java(j, Java2VM.TEMPLATE_FOLDER + "/README.md.vm", "## Screen shots",
				"![French home screen](screenshots/home.fr.png)", "# " + j.getAppName(), j.getAppDescription());

	}

	/**
	 * Test method for
	 * {@link com.dea42.build.Java2VM#java2vm(java.lang.String, java.lang.String)}.
	 * Quick test
	 * 
	 * @throws Exception
	 */
	@Test
	public void testJava2vmUnitBase() throws Exception {
		Java2VM j = new Java2VM(TEST_WITH_INT_IDS);

		doJava2vm(j, "src/test/java/com/dea42/genspring/UnitBase.java", "package ${basePkg};",
				"protected static ${idPrim} TEST_USER_ID;", "protected static ${idPrim} ADMIN_USER_ID;",
				"TEST_USER_ID = Utils.getProp(appBundle, \"default.userid\", 1${idMod});",
				"ADMIN_USER_ID = Utils.getProp(appBundle, \"default.adminid\", 2${idMod});");
	}

	/**
	 * Not really a test as much as quick convert of new POC files into templates.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testJava2vmNewStuff() throws Exception {
		Java2VM j = new Java2VM(TEST_WITH_INT_IDS);

//		doJava2vm(j, "src/main/java/com/dea42/genspring/search/SearchCriteria.java", "package ${basePkg}.search;");
//		doJava2vm(j, "src/main/java/com/dea42/genspring/search/SearchOperation.java", "package ${basePkg}.search;");
//		doJava2vm(j, "src/main/java/com/dea42/genspring/search/SearchSpecification.java", "package ${basePkg}.search;");
		doJava2vm(j, "src/test/java/com/dea42/genspring/MockBase.java", "package ${basePkg};");
	}

	/**
	 * Test method for
	 * {@link com.dea42.build.Java2VM#java2vm(java.lang.String, java.lang.String)}.
	 * with int IDs
	 * 
	 * @throws Exception
	 */
	@Test
	public void testJava2vmAppControllerTest() throws Exception {
		Java2VM j = new Java2VM(TEST_WITH_INT_IDS);

		doJava2vm(j, "src/test/java/com/dea42/genspring/controller/AppControllerTest.java",
				"package ${basePkg}.controller;", "account.setId(0${idMod});");
	}

	/**
	 * Test method for
	 * {@link com.dea42.build.Java2VM#java2vm(java.lang.String, java.lang.String)}.
	 * with long IDs
	 * 
	 * @throws Exception
	 */
	@Test
	public void testJava2vmAppControllerTest4() throws Exception {
		Java2VM j = new Java2VM(TEST_WITH_LONG_IDS);

		doJava2vm(j, "src/test/java/com/dea42/genspring/controller/AppControllerTest.java",
				"package ${basePkg}.controller;", "account.setId(0${idMod});");
	}

	/**
	 * Test method for
	 * {@link com.dea42.build.Java2VM#java2vm(java.lang.String, java.lang.String)}.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testJava2vmSeleniumBase() throws Exception {
		Java2VM j = new Java2VM(TEST_WITH_INT_IDS);

		doJava2vm(j, "src/test/java/com/dea42/genspring/selenium/SeleniumBase.java", "package ${basePkg}.selenium;",
				"protected String context = \"${baseModule}\";");
	}

	/**
	 * Test method for
	 * {@link com.dea42.build.Java2VM#java2vm(java.lang.String, java.lang.String)}.
	 * with pom.xml file and alt options
	 * 
	 * @throws Exception
	 */
	@Test
	public void testJava2vmPom2() throws Exception {
		Java2VM j = new Java2VM(TEST_WITH_ALT_OPTIONS);

		doJava2vm(j, "pom.xml", "<groupId>${baseGroupId}</groupId>", "<artifactId>${baseArtifactId}</artifactId>",
				"<version>${appVersion}-SNAPSHOT</version>", "<packaging>war</packaging>",
				"<description>${appDescription}</description>", "<mainClass>${basePkg}.WebAppApplication</mainClass>");
	}

	/**
	 * Test method for
	 * {@link com.dea42.build.Java2VM#java2vm(java.lang.String, java.lang.String)}.
	 * with pom.xml file
	 * 
	 * @throws Exception
	 */
	@Test
	public void testJava2vmPom() throws Exception {
		Java2VM j = new Java2VM(TEST_WITH_INT_IDS);

		doJava2vm(j, "pom.xml", "<groupId>${baseGroupId}</groupId>", "<artifactId>${baseArtifactId}</artifactId>",
				"<version>${appVersion}-SNAPSHOT</version>", "<packaging>war</packaging>",
				"<description>${appDescription}</description>", "<mainClass>${basePkg}.WebAppApplication</mainClass>");
	}

	/**
	 * Test method for
	 * {@link com.dea42.build.Java2VM#java2vm(java.lang.String, java.lang.String)}.
	 * with alt options
	 * 
	 * @throws Exception
	 */
	@Test
	public void testJava2vmUnitBase2() throws Exception {
		Java2VM j = new Java2VM(TEST_WITH_ALT_OPTIONS);

		doJava2vm(j, "src/test/java/com/dea42/genspring/UnitBase.java", "package ${basePkg};",
				"protected static ${idPrim} TEST_USER_ID;", "protected static ${idPrim} ADMIN_USER_ID;",
				"TEST_USER_ID = Utils.getProp(appBundle, \"default.userid\", 1${idMod});",
				"ADMIN_USER_ID = Utils.getProp(appBundle, \"default.adminid\", 2${idMod});");
	}

	/**
	 * Test method for
	 * {@link com.dea42.build.Java2VM#java2vm(java.lang.String, java.lang.String)}.
	 * with longs
	 * 
	 * @throws Exception
	 */
	@Test
	public void testJava2vmUnitBase4() throws Exception {
		Java2VM j = new Java2VM(TEST_WITH_LONG_IDS);

		doJava2vm(j, "src/test/java/com/dea42/genspring/UnitBase.java", "package ${basePkg};",
				"protected static ${idPrim} TEST_USER_ID;", "protected static ${idPrim} ADMIN_USER_ID;",
				"TEST_USER_ID = Utils.getProp(appBundle, \"default.userid\", 1${idMod});",
				"ADMIN_USER_ID = Utils.getProp(appBundle, \"default.adminid\", 2${idMod});");
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
	}

}
