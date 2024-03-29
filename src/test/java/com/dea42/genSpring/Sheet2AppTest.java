package com.dea42.genSpring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Test;

import com.dea42.build.CommonMethods;
import com.dea42.build.GenSpring;
import com.dea42.build.Sheets2DB;
import com.dea42.build.Sheets2DBTest;
import com.dea42.common.Db;
import com.dea42.common.Utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Sheet2AppTest {

	private static final boolean clearDBFirst = true;
	private static final boolean clearSrcFirst = true;
	private static final boolean stopOnError = false;
	// to speed up GenSpring testing set this to false
	private static boolean doDbCreate = true;

	/**
	 * Generate the POC app and run the regression tests
	 * 
	 * @throws IOException
	 */
	@Test
	void testEndToEndWatchlist() throws Exception {
		assumeTrue(Utils.getProp("Watchlist", "enabled", false));
		backupProject("Watchlist");
		doEndToEnd("Watchlist", true);

	}

	/**
	 * Run full end to end regression tests with genSpringMySQLTest.properties file
	 * and validate the results. Note will skip if enable=false in properties file.
	 */
	@Test
	void testEndToEndMySQLTest() throws Exception {
		assumeTrue(Utils.getProp("genSpringMySQLTest", "enabled", false));
		doEndToEnd("genSpringMySQLTest", true);

	}

	/**
	 * Run full end to end regression tests with genSpringMSSQLTest.properties file
	 * and validate the results. Note will skip if enable=false in properties file.
	 */
	@Test
	void testEndToEndMSSQLTest() throws Exception {
		assumeTrue(Utils.getProp("genSpringMSSQLTest", "enabled", false));
		doEndToEnd("genSpringMSSQLTest", true);

	}

	/**
	 * Do full tests of sheet to app that uses package and module that match the
	 * static folder. This project and then be used to prototype changes as well.
	 */
	@Test
	void testEndToEnd() throws Exception {
		doEndToEnd("genSpringTest", true);
	}

	/**
	 * Do full tests of sheet to app package and module that does not match the
	 * static folder and uses diff sheet than testEndToEnd() to ensure no hard codes
	 * are left in, no gened files are in the static folder and a couple diff
	 * generation options.
	 */
	@Test
	void testEndToEnd2() throws Exception {
		doEndToEnd("genSpringTest2", true);
	}

	/**
	 * Same as testEndToEnd2 except with out purge and checking no genSpring created
	 * files were changed.
	 */
	@Test
	void testEndToEnd3() throws Exception {
		String bundleName = "genSpringTest2";

		Map<String, Long> modTimes = new HashMap<String, Long>();
		ResourceBundle bundle = ResourceBundle.getBundle(bundleName);
		Path outdir = Utils.getPath(Utils.getProp(bundle, CommonMethods.PROPKEY + ".outdir", "."), "src");
		Path pom = Utils.getPath(Utils.getProp(bundle, CommonMethods.PROPKEY + ".outdir", "."), "pom.xml");

		modTimes.put(pom.toString(), pom.toFile().lastModified());
		try {
			Files.walkFileTree(outdir, new FileVisitor<Path>() {
				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					return FileVisitResult.CONTINUE;
				}

				/**
				 * store file modtimes
				 */
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					modTimes.put(file.toString(), file.toFile().lastModified());
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
		} catch (IOException e) {
			log.error("failed getting mod times", e);
			fail("failed getting mod times");
		}

		assertFalse(modTimes.isEmpty(), "check if modTimes empty");
		doEndToEnd("genSpringTest2", false);

		assertEquals(modTimes.get(pom.toString()).longValue(), pom.toFile().lastModified(), pom.toString());
		try {
			Files.walkFileTree(outdir, new FileVisitor<Path>() {
				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					return FileVisitResult.CONTINUE;
				}

				/**
				 * check file modtimes unchanged
				 */
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					assertNotNull(modTimes.get(file.toString()), "File added:" + file.toString());
					assertEquals(modTimes.get(file.toString()).longValue(), file.toFile().lastModified(),
							file.toString());
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
		} catch (IOException e) {
			log.error("failed getting mod times", e);
			fail("failed getting mod times");
		}

	}

	/**
	 * removes the src and target folders plus pom.xml
	 * 
	 * @param bundleName
	 * @param clearDB
	 * @param clearSrc
	 * @throws IOException
	 */
	private void purgeProject(String bundleName, boolean clearDB, boolean clearSrc) throws IOException {
		ResourceBundle bundle = ResourceBundle.getBundle(bundleName);
		if (clearSrc) {
			String outdir = Utils.getProp(bundle, GenSpring.PROPKEY + ".outdir", ".");
			Utils.deletePath(Utils.getPath(outdir, "src"));
			Utils.deletePath(Utils.getPath(outdir, "target"));
			Utils.deletePath(Utils.getPath(outdir, "bin"));
			Utils.deletePath(Utils.getPath(outdir, "pom.xml"));
			Utils.deletePath(Utils.getPath(outdir, "README.md"));
			Utils.deletePath(Utils.getPath(outdir, "files.md"));
			Utils.deletePath(Utils.getPath(outdir, "listFiles.bat"));
			Utils.deletePath(Utils.getPath(outdir, "screenshots"));
		}
		if (clearDB) {
			String outdir = Utils.getProp(bundle, CommonMethods.PROPKEY + ".outdir", ".");
			Path dbFile = Utils.getPath(outdir.replace('\\', '/'), bundleName + "DB.sqlite");
			if (dbFile.toFile().exists()) {
				dbFile.toFile().delete();
			}
		}
	}

	private void backupFileOrFolder(ResourceBundle bundle, String outdir, long ts, String filePathName)
			throws IOException {
		Path srcPath = Utils.getPath(outdir, filePathName);
		if (srcPath.toFile().exists()) {
			Path target = Utils.getPath(outdir, "hold", "" + ts, filePathName);
			if (srcPath.toFile().isDirectory())
				target.toFile().mkdirs();
			else
				target.toFile().getParentFile().mkdirs();

			Files.move(srcPath, target, StandardCopyOption.REPLACE_EXISTING);
		} else {
			log.warn("No file to back up:" + srcPath.toAbsolutePath());
		}
	}

	private void backupProject(String bundleName) throws IOException {
		ResourceBundle bundle = ResourceBundle.getBundle(bundleName);
		String outdir = Utils.getProp(bundle, GenSpring.PROPKEY + ".outdir", ".");
		Path srcPath = Utils.getPath(outdir, "pom.xml");
		File pom = srcPath.toFile();
		if (pom.exists()) {
			long ts = pom.lastModified();
			backupFileOrFolder(bundle, outdir, ts, "pom.xml");
			backupFileOrFolder(bundle, outdir, ts, "README.md");
			backupFileOrFolder(bundle, outdir, ts, bundleName + "DB.sqlite");
			backupFileOrFolder(bundle, outdir, ts, "src");
		}
	}

	/**
	 * Run both Sheets2DB and GenSpring on bundleName then run some basic tests.
	 * 
	 * @param bundleName
	 * @param purgeFirst if true clears the DB and generated files to ensure all
	 *                   possible files are recreated.
	 * @throws Exception
	 */
	public void doEndToEnd(String bundleName, boolean purgeFirst) throws Exception {
		// remove all files form projects
		if (purgeFirst)
			purgeProject(bundleName, doDbCreate && clearDBFirst, clearSrcFirst);

		ResourceBundle bundle = ResourceBundle.getBundle(bundleName);
		String outdir = Utils.getProp(bundle, CommonMethods.PROPKEY + ".outdir", ".");
		Db db = new Db("Sheet2AppTest", bundleName);
		if (doDbCreate) {
			// Note set to fail if any errors encountered.
			Sheets2DB s = new Sheets2DB(bundleName, true);
			s.getSheet();
			Sheets2DBTest s2dt = new Sheets2DBTest();
			s2dt.setStopOnError(stopOnError);
			List<String> tabs = Utils.getPropList(bundle, CommonMethods.PROPKEY + ".tabs");
			for (String tabName : tabs) {
				// no point in dup chk code so just call
				String err = s2dt.quickChkTable(db, bundle, tabName);
				if (StringUtils.isNotBlank(err))
					log.error(err);
			}
		}

		outdir = Utils.getProp(bundle, GenSpring.PROPKEY + ".outdir", ".");
		try {
			GenSpring gs = new GenSpring(bundleName);
			List<String> tableNames = gs.getTablesNames(db);

			gs.writeProject(tableNames);

			String cmd = "mvn";
			String osName = System.getProperty("os.name");
			int expected = 0;
			if (osName.startsWith("Windows"))
				cmd = "mvn.cmd";
//			cmd = cmd + " clean integration-test -Pintegration";
			cmd = cmd + " clean install";

			// if we did not purge nothing should have changed so no need to rerun tests.
			if (purgeFirst) {
				int rtn = Utils.runCmd(cmd, outdir);
				assertEquals(expected, rtn, "Return from:" + cmd);

				String baseModule = Utils.getProp(bundle, GenSpring.PROPKEY + ".module");
				String baseArtifactId = Utils.getProp(bundle, GenSpring.PROPKEY + ".artifactId", baseModule);
				String appVersion = Utils.getProp(bundle, GenSpring.PROPKEY + ".version", gs.getGenSpringVersion());

				Path p = Utils.getPath(outdir, "target", baseArtifactId + "-" + appVersion + "-SNAPSHOT.war")
						.normalize();
				assertTrue(p.toFile().exists(), "check war file was created and properly named:" + p.toString());
			}
		} catch (Exception e) {
			log.error("Failed generating app", e);
			fail(e.getMessage());
		}
	}

	public static void main(String... args) {
		try {
			if (args.length > 0) {
				Sheet2AppTest s = new Sheet2AppTest();
				s.doEndToEnd(args[0], true);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
