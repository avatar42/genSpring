package com.dea42.genSpring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.junit.Assume;
import org.junit.Test;

import com.dea42.build.CommonMethods;
import com.dea42.build.GenSpring;
import com.dea42.build.Sheets2DB;
import com.dea42.common.Db;
import com.dea42.common.Utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Sheet2AppTest {

	private static final boolean clearDBFirst = true;
	private static final boolean clearSrcFirst = true;
	private static final boolean stopOnError = false;
	private ResourceBundle renames = ResourceBundle.getBundle("rename");

	/**
	 * Generate the POC app and run the regression tests
	 * 
	 * @throws IOException
	 */
	@Test
	public void testEndToEndWatchlist() throws Exception {
		Assume.assumeTrue(Utils.getProp("Watchlist", "enabled", false));
		backupProject("Watchlist");
		doEndToEnd("Watchlist", true);

	}

	/**
	 * Run full end to end regression tests with genSpringMySQLTest.properties file
	 * and validate the results. Note will skip if enable=false in properties file.
	 */
	@Test
	public void testEndToEndMySQLTest() throws Exception {
		Assume.assumeTrue(Utils.getProp("genSpringMySQLTest", "enabled", false));
		doEndToEnd("genSpringMySQLTest", true);

	}

	/**
	 * Run full end to end regression tests with genSpringMSSQLTest.properties file
	 * and validate the results. Note will skip if enable=false in properties file.
	 */
	@Test
	public void testEndToEndMSSQLTest() throws Exception {
		Assume.assumeTrue(Utils.getProp("genSpringMSSQLTest", "enabled", false));
		doEndToEnd("genSpringMSSQLTest", true);

	}

	/**
	 * Do full tests of sheet to app that uses package and module that match the
	 * static folder. This project and then be used to prototype changes as well.
	 */
	@Test
	public void testEndToEnd() throws Exception {
		doEndToEnd("genSpringTest", true);
	}

	/**
	 * Do full tests of sheet to app package and module that does not match the
	 * static folder and uses diff sheet than testEndToEnd() to ensure no hard codes
	 * are left in, no gened files are in the static folder and a couple diff
	 * generation options.
	 */
	@Test
	public void testEndToEnd2() throws Exception {
		doEndToEnd("genSpringTest2", true);
	}

	/**
	 * Same as testEndToEnd2 except with out purge and checking no genSpring created
	 * files were changed.
	 */
	@Test
	public void testEndToEnd3() throws Exception {
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

		assertFalse("check if modTimes empty", modTimes.isEmpty());
		doEndToEnd("genSpringTest2", false);

		assertEquals(pom.toString(), modTimes.get(pom.toString()).longValue(), pom.toFile().lastModified());
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
					assertNotNull("File added:" + file.toString(), modTimes.get(file.toString()) != null);
					assertEquals(file.toString(), modTimes.get(file.toString()).longValue(),
							file.toFile().lastModified());
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
			Utils.deletePath(Utils.getPath(outdir, "Scripts"));
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
			backupFileOrFolder(bundle, outdir, ts, bundleName + "DB.sqlite");
			backupFileOrFolder(bundle, outdir, ts, "src");
		}
	}

	private void chkErr(String lable, int expected, int found) {
		if (stopOnError)
			assertEquals(lable, expected, found);
		else
			log.error(lable + " expected:" + expected + " :" + found);

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
			purgeProject(bundleName, clearDBFirst, clearSrcFirst);

		// Note set to fail if any errors encountered.
		Sheets2DB s = new Sheets2DB(bundleName, true, true);
		s.getSheet();

		ResourceBundle bundle = ResourceBundle.getBundle(bundleName);
		String outdir = Utils.getProp(bundle, CommonMethods.PROPKEY + ".outdir", ".");
		Db db = new Db("Sheet2AppTest", bundleName);
		String schema = db.getPrefix();

		Connection conn = db.getConnection("Sheet2AppTest");
		List<String> tabs = Utils.getPropList(bundle, CommonMethods.PROPKEY + ".tabs");
		for (String tabName : tabs) {
			String tableName = Utils.tabToStr(renames, tabName);
			int columns = Utils.getProp(bundle, tableName + ".testCols", 0);
			int rows = Utils.getProp(bundle, tableName + ".testRows", 0);
			try {
				String query = "SELECT * FROM " + schema + tableName;
				Statement stmt = conn.createStatement();
				stmt.setMaxRows(1);
				log.debug("query=" + query);
				ResultSet rs = stmt.executeQuery(query);
				assertNotNull("Check ResultSet", rs);
				ResultSetMetaData rm = rs.getMetaData();
				int size = rm.getColumnCount();
				chkErr("Checking expected columns in " + schema + tableName, columns, size);

				query = "SELECT COUNT(*) FROM " + schema + tableName;
				stmt = conn.createStatement();
				rs = stmt.executeQuery(query);
				assertNotNull("Check ResultSet", rs);
				if (!db.isSQLite())
					rs.next();
				chkErr("Checking expected rows in " + schema + tableName, rows, rs.getInt(1));
				List<Integer> userColNums = s.strToCols(Utils.getProp(bundle, tableName + ".user"));
				if (!userColNums.isEmpty()) {
					tableName = tableName + "User";
					columns = Utils.getProp(bundle, tableName + ".testCols", 0);
					rows = Utils.getProp(bundle, tableName + ".testRows", 0);
					query = "SELECT * FROM " + schema + tableName;
					stmt = conn.createStatement();
					log.debug("query=" + query);
					rs = stmt.executeQuery(query);
					assertNotNull("Check ResultSet", rs);
					rm = rs.getMetaData();
					size = rm.getColumnCount();
					chkErr("Checking expected columns in " + schema + tableName, columns, size);

					query = "SELECT COUNT(*) FROM " + schema + tableName;
					stmt = conn.createStatement();
					rs = stmt.executeQuery(query);
					assertNotNull("Check ResultSet", rs);
					if (db.isMySQL())
						rs.next();
					chkErr("Checking expected rows in " + schema + tableName, rows, rs.getInt(1));
				}
			} catch (SQLException e) {
				log.error("Exception creating DB", e);
				fail("Exception creating DB");
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
			cmd = cmd + " clean integration-test -Pintegration";

			// if we did not purge nothing should have changed so no need to rerun tests.
			if (purgeFirst) {
				int rtn = Utils.runCmd(cmd, outdir);
				assertEquals("Return from:" + cmd, expected, rtn);

				String baseModule = Utils.getProp(bundle, GenSpring.PROPKEY + ".module");
				String baseArtifactId = Utils.getProp(bundle, GenSpring.PROPKEY + ".artifactId", baseModule);
				String appVersion = Utils.getProp(bundle, GenSpring.PROPKEY + ".version", gs.getGenSpringVersion());

				Path p = Utils.getPath(outdir, "target", baseArtifactId + "-" + appVersion + "-SNAPSHOT.war")
						.normalize();
				assertTrue("check war file was created and properly named:" + p.toString(), p.toFile().exists());
			}
		} catch (Exception e) {
			log.error("Failed generating app", e);
			fail(e.getMessage());
		}
	}

}
