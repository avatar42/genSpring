package com.dea42.genSpring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dea42.build.GenSpring;
import com.dea42.build.Sheets2DB;
import com.dea42.common.Db;
import com.dea42.common.Utils;

public class Sheet2AppTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(Sheet2AppTest.class.getName());
	private static final boolean clearDBFirst = true;
	private static final boolean clearSrcFirst = true;

	/**
	 * Run full end to end regression tests with genSpringMySQLTest.properties file
	 * and validate the results. Note will skip if enable=false in properties file.
	 */
	@Test
	public void testEndToEndMySQLTest() {
		Assume.assumeTrue(Utils.getProp("genSpringMySQLTest", "enabled", false));
		doEndToEnd("genSpringMySQLTest", true);

	}

	/**
	 * Do full tests of sheet to app that uses package and module that match the
	 * static folder. This project and then be used to prototype changes as well.
	 */
	@Test
	public void testEndToEnd() {
		doEndToEnd("genSpringTest", true);
	}

	/**
	 * Do full tests of sheet to app package and module that does not match the
	 * static folder and uses diff sheet than testEndToEnd() to ensure no hard codes
	 * are left in, no gened files are in the static folder and a couple diff
	 * generation options.
	 */
	@Test
	public void testEndToEnd2() {
		doEndToEnd("genSpringTest2", true);
	}

	/**
	 * Same as testEndToEnd2 except with out purge and checking no genSpring created
	 * files were changed.
	 */
	@Test
	public void testEndToEnd3() {
		String bundleName = "genSpringTest2";

		Map<String, Long> modTimes = new HashMap<String, Long>();
		ResourceBundle bundle = ResourceBundle.getBundle(bundleName);
		Path outdir = Utils.getPath(Utils.getProp(bundle, Sheets2DB.PROPKEY + ".outdir", "."), "src");
		Path pom = Utils.getPath(Utils.getProp(bundle, Sheets2DB.PROPKEY + ".outdir", "."), "pom.xml");

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
			LOGGER.error("failed getting mod times", e);
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
			LOGGER.error("failed getting mod times", e);
			fail("failed getting mod times");
		}

	}

	private void deletePath(Path path) {
		if (path.toFile().exists()) {
			if (path.toFile().isFile()) {
				try {
					Files.delete(path);
				} catch (IOException e) {
					LOGGER.error("Failed deleting file:" + path, e);
					fail(e.getMessage());
				}
			} else if (path.toFile().isDirectory()) {

				try {
					Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
						@Override
						public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
							if (!file.endsWith(".sqlite")) {
								LOGGER.debug("Deleting file:" + file);
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
								LOGGER.debug("Deleting file again:" + file);
								Files.delete(file);
							}
							return FileVisitResult.CONTINUE;
						}

						@Override
						public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
							if (exc == null) {
								if (!path.equals(dir)) {
									LOGGER.debug("Deleting dir:" + dir);
									Files.delete(dir);
								}
								return FileVisitResult.CONTINUE;
							} else {
								// directory iteration failed; propagate exception
								// throw exc;
								// Windows seems to be slow noticing we've removed everything from the folder
								LOGGER.error("Ignoring failed to delete folder", exc);
								return FileVisitResult.CONTINUE;
							}
						}
					});
				} catch (

				IOException e) {
					LOGGER.error("Failed deleting folder:" + path, e);
					fail(e.getMessage());
				}
			}
		}
	}

	/**
	 * removes the src and target folders plus pom.xml
	 * 
	 * @param bundleName
	 * @param clearDB
	 * @param clearSrc
	 */
	private void purgeProject(String bundleName, boolean clearDB, boolean clearSrc) {
		ResourceBundle bundle = ResourceBundle.getBundle(bundleName);
		if (clearSrc) {
			String outdir = Utils.getProp(bundle, GenSpring.PROPKEY + ".outdir", ".");
			deletePath(Utils.getPath(outdir, "src"));
			deletePath(Utils.getPath(outdir, "Scripts"));
			deletePath(Utils.getPath(outdir, "target"));
			deletePath(Utils.getPath(outdir, "bin"));
			deletePath(Utils.getPath(outdir, "pom.xml"));
		}
		if (clearSrc) {
			String outdir = Utils.getProp(bundle, Sheets2DB.PROPKEY + ".outdir", ".");
			Path dbFile = Utils.getPath(outdir.replace('\\', '/'), bundleName + "DB.sqlite");
			if (dbFile.toFile().exists()) {
				dbFile.toFile().delete();
			}
		}
	}

	public void doEndToEnd(String bundleName, boolean purgeFirst) {
		// remove all files form projects
		if (purgeFirst)
			purgeProject(bundleName, clearDBFirst, clearSrcFirst);

		Sheets2DB s = new Sheets2DB(bundleName, true);
		s.getSheet();
		ResourceBundle bundle = ResourceBundle.getBundle(bundleName);
		String outdir = Utils.getProp(bundle, Sheets2DB.PROPKEY + ".outdir", ".");
		Db db = new Db("Sheet2AppTest", bundleName, outdir);
		String schema = db.getPrefix();

		Connection conn = db.getConnection("Sheet2AppTest");
		List<String> tables = Utils.getPropList(bundle, Sheets2DB.PROPKEY + ".tabs");
		for (String tableName : tables) {
			int columns = Utils.getProp(bundle, tableName + ".testCols", 0);
			int rows = Utils.getProp(bundle, tableName + ".testRows", 0);
			try {
				String query = "SELECT * FROM " + schema + tableName;
				Statement stmt = conn.createStatement();
				stmt.setMaxRows(1);
				LOGGER.debug("query=" + query);
				ResultSet rs = stmt.executeQuery(query);
				assertNotNull("Check ResultSet", rs);
				ResultSetMetaData rm = rs.getMetaData();
				int size = rm.getColumnCount();
				assertEquals("Checking expected columns in " + schema + tableName, columns, size);

				query = "SELECT COUNT(*) FROM " + schema + tableName;
				stmt = conn.createStatement();
				rs = stmt.executeQuery(query);
				assertNotNull("Check ResultSet", rs);
				if (db.isMySQL())
					rs.next();
				assertEquals("Checking expected rows in " + schema + tableName, rows, rs.getInt(1));
				List<Integer> userColNums = s.strToCols(Utils.getProp(bundle, tableName + ".user"));
				if (!userColNums.isEmpty()) {
					tableName = tableName + "User";
					columns = Utils.getProp(bundle, tableName + ".testCols", 0);
					rows = Utils.getProp(bundle, tableName + ".testRows", 0);
					query = "SELECT * FROM " + schema + tableName;
					stmt = conn.createStatement();
					LOGGER.debug("query=" + query);
					rs = stmt.executeQuery(query);
					assertNotNull("Check ResultSet", rs);
					rm = rs.getMetaData();
					size = rm.getColumnCount();
					assertEquals("Checking expected columns in " + schema + tableName, columns, size);

					query = "SELECT COUNT(*) FROM " + schema + tableName;
					stmt = conn.createStatement();
					rs = stmt.executeQuery(query);
					assertNotNull("Check ResultSet", rs);
					if (db.isMySQL())
						rs.next();
					assertEquals("Checking expected rows in " + schema + tableName, rows, rs.getInt(1));
				}
			} catch (SQLException e) {
				LOGGER.error("Exception creating DB", e);
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

			int rtn = Utils.runCmd(cmd, outdir);
			assertEquals("Return from:" + cmd, expected, rtn);

			String baseModule = Utils.getProp(bundle, GenSpring.PROPKEY + ".module");
			String baseArtifactId = Utils.getProp(bundle, GenSpring.PROPKEY + ".artifactId", baseModule);
			String appVersion = Utils.getProp(bundle, GenSpring.PROPKEY + ".version", "1.0.0");

			Path p = Utils.getPath(outdir, "target", baseArtifactId + "-" + appVersion + "-SNAPSHOT.war").normalize();
			assertTrue("check war file was created and properly named:" + p.toString(), p.toFile().exists());
		} catch (Exception e) {
			LOGGER.error("Failed generating app", e);
			fail(e.getMessage());
		}
	}

}
