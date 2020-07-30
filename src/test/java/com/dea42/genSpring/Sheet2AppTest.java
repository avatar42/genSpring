package com.dea42.genSpring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Scanner;

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
	 * Do full tests of sheet to app that uses package and module that match the
	 * static folder. This project and then be used to prototype changes as well.
	 */
	@Test
	public void testEndToEnd() {
		doEndToEnd("genSpringTest", 5);
	}

	/**
	 * Do full tests of sheet to app that does not package and module that match the
	 * static folder and diff sheet than testEndToEnd() to ensure no hard codes are
	 * left in, no gened files are in the static folder and a couple diff sheet
	 * options.
	 */
	@Test
	public void testEndToEnd2() {
		doEndToEnd("genSpringTest2", 6);
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

	public void doEndToEnd(String bundleName, int columns) {
		// remove all files form projects
		purgeProject(bundleName, clearDBFirst, clearSrcFirst);

		Sheets2DB s = new Sheets2DB(bundleName, true);
		s.getSheet();
		ResourceBundle bundle = ResourceBundle.getBundle(bundleName);
		String outdir = Utils.getProp(bundle, Sheets2DB.PROPKEY + ".outdir", ".");
		Db db = new Db("Sheet2AppTest", bundleName, outdir);
		Connection conn = db.getConnection("Sheet2AppTest");
		List<String> tables = Utils.getPropList(bundle, Sheets2DB.PROPKEY + ".tabs");
		for (String tableName : tables) {
			try {
				String query = "SELECT * FROM " + tableName;
				Statement stmt = conn.createStatement();
				stmt.setMaxRows(1);
				LOGGER.debug("query=" + query);
				ResultSet rs = stmt.executeQuery(query);
				ResultSetMetaData rm = rs.getMetaData();
				int size = rm.getColumnCount();
				assertEquals("Checking expected columns in " + tableName, columns, size);
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

		} catch (Exception e) {
			LOGGER.error("Failed generating app", e);
			fail(e.getMessage());
		}

		String cmd = "mvn";
		String osName = System.getProperty("os.name");
		if (osName.startsWith("Windows"))
			cmd = "mvn.cmd";
		cmd = cmd + " clean install";

		Utils.runCmd(cmd, outdir);

		Path p = Utils
				.getPath(outdir, "target",
						Utils.getProp(bundle, GenSpring.PROPKEY + ".module", "1.0.0") + "-"
								+ Utils.getProp(bundle, GenSpring.PROPKEY + ".version", "1.0.0") + "-SNAPSHOT.war")
				.normalize();
		assertTrue("check war file was created and properly named:" + p.toString(), p.toFile().exists());
	}

}
