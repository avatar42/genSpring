package com.dea42.build;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

import com.dea42.common.Db;
import com.dea42.common.Utils;

import lombok.extern.slf4j.Slf4j;

/**
 * Title: GenSpring <br>
 * Description: Class for generating SIMPLE CRUD web app from table info<br>
 * Copyright: Copyright (c) 2020<br>
 * Company: RMRR<br>
 * <br>
 * 
 * @author David Abigt<br>
 *         <br>
 *         See http://jakarta.apache.org/ojb/jdbc-types.html for data type info
 */
@Slf4j
public class GenSpring extends CommonMethods {
	public static int IMPORT_TYPE_SERVICE = 0;
	public static int IMPORT_TYPE_FORM = 1;
	public static int IMPORT_TYPE_BEAN = 2;

	// see
	// https://docs.microsoft.com/en-us/sql/odbc/reference/syntax/sqlcolumns-function?view=sql-server-ver15
	public static int TABLE_CAT = 1;// 'null'
	public static int TABLE_SCHEM = 2;// 'null'
	public static int TABLE_NAME = 3;// 'account'
	public static int COLUMN_NAME = 4;// 'id'
	public static int DATA_TYPE = 5;// '4' // Types.*
	public static int TYPE_NAME = 6;// 'INTEGER'
	// SQLite ignores field sizes and precisioN. nEED TO USE TYPE_NAME TO GET
	// varchar LEN
	public static int COLUMN_SIZE = 7;// '2000000000'
	public static int BUFFER_LENGTH = 8;// '2000000000'
	public static int DECIMAL_DIGITS = 9;// '10'
	public static int NUM_PREC_RADIX = 10;// '10'
	public static int NULLABLE = 11;// '0'
	public static int REMARKS = 12;// 'null'
	public static int COLUMN_DEF = 13;// 'null'
	public static int SQL_DATA_TYPE = 14;// '0'
	public static int SQL_DATETIME_SUB = 15;// '0'
	public static int CHAR_OCTET_LENGTH = 16;// '2000000000'
	public static int ORDINAL_POSITION = 17;// '0'
	public static int IS_NULLABLE = 18;// 'NO'
	// below may not be supported
	public static int SCOPE_CATLOG = 19;// 'null'
	public static int SCOPE_SCHEMA = 20;// 'null'
	public static int SCOPE_TABLE = 21;// 'null'
	public static int SOURCE_DATA_TYPE = 22;// 'null'

	// SQL server and My SQL
	public static int IS_AUTOINCREMENT = 23;// 'YES'
	public static int IS_GENERATEDCOLUMN = 24;// 'NO'
	// just MS SQL
	public static int SS_IS_SPARSE = 25;// '0'
	public static int SS_IS_COLUMN_SET = 26;// '0'
	public static int SS_UDT_CATALOG_NAME = 27;// 'null'
	public static int SS_UDT_SCHEMA_NAME = 28;// 'null'
	public static int SS_UDT_ASSEMBLY_TYPE_NAME = 29;// 'null'
	public static int SS_XML_SCHEMACOLLECTION_CATALOG_NAME = 30;// 'null'
	public static int SS_XML_SCHEMACOLLECTION_SCHEMA_NAME = 31;// 'null'
	public static int SS_XML_SCHEMACOLLECTION_NAME = 32;// 'null'

	public static final String PKEY_INFO = "PRIMARY_KEY_INFO";

	public GenSpring(String bundleName) throws IOException {
		initVars(bundleName);
	}

	/**
	 * Simplify test generation by just passing props use to gen to the app's tests
	 */
	private void writeTestProps() {
		Path p = Utils.createFile(baseDir, "src/test/resources/test.properties");
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
				for (String key : bundle.keySet()) {
					String val = bundle.getString(key);
					if (StringUtils.isBlank(val)) {
						if ("db.url".equals(key)) {
							ps.println(key + "=" + db.getDbUrl());
						} else {
							ps.println(key + "=");
						}
					} else {
						ps.println(key + "=" + val);
					}
					log.debug(key + "=" + val);
				}
				log.warn("Wrote:" + p.toString());
			} catch (Exception e) {
				log.error("failed to create " + p, e);
				p.toFile().delete();
			}
		}
		p = Utils.createFile(baseDir, "src/test/resources/rename.properties");
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
				for (String key : renames.keySet()) {
					String val = renames.getString(key);
					if (StringUtils.isBlank(val))
						ps.println(key + "=");
					else
						ps.println(key + "=" + val);
				}
				log.warn("Wrote:" + p.toString());
			} catch (Exception e) {
				log.error("failed to create " + p, e);
				p.toFile().delete();
			}
		}
	}

	/**
	 * Creates a top level page to ref the other pages.
	 * 
	 * @param list
	 */
	private void writeIndex(Set<String> set) {
		Path p = Utils.createFile(baseDir, "src/main/resources/templates/index.html");
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
				ps.println(htmlHeader("header.home", null));
				for (String className : set) {
					String fieldName = className.substring(0, 1).toLowerCase() + className.substring(1);
					ps.println("    	<a th:href=\"@{/" + fieldName + "s}\">" + className + "</a><br>");
				}
				ps.println("    	<a th:href=\"@{/api/}\">/api/</a><br>");
				ps.println("    	<a th:href=\"@{/login}\">Login</a><br>");
				ps.println(htmlFooter());
				log.warn("Wrote:" + p.toString());
			} catch (Exception e) {
				log.error("failed to create " + p, e);
				p.toFile().delete();
			}
		}
	}

	private void writeApiIndex(Set<String> set) {
		Path p = Utils.createFile(baseDir, "src/main/resources/templates/api_index.html");
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
				ps.println(htmlHeader("header.restApi", null));
				for (String className : set) {
					String fieldName = className.substring(0, 1).toLowerCase() + className.substring(1);
					ps.println("    	<a th:href=\"@{/api/" + fieldName + "s}\">" + className + "</a><br>");
				}
				ps.println("    	<a th:href=\"@{/}\">Home</a><br>");
				ps.println("    	<a th:href=\"@{/login}\">Login</a><br>");
				ps.println(htmlFooter());
				log.warn("Wrote:" + p.toString());
			} catch (Exception e) {
				log.error("failed to create " + p, e);
				p.toFile().delete();
			}
		}
	}

	/**
	 * return true if class has user fields moved separate class/table or is
	 * ACCOUNT_CLASS. TODO: currently keys off className + ".user". Should probably
	 * get from DB.
	 * 
	 * @param className
	 * @return
	 */
	private boolean classHasUserFields(String className) {
		String userColNums = Utils.getProp(bundle, className + ".user");

		return ACCOUNT_CLASS.equals(className) || !StringUtils.isBlank(userColNums);
	}

	private void writeNav(Set<String> set, Map<String, String> statics) {
		Path p = Utils.createFile(baseDir, "src/main/resources/templates/fragments/nav.html");
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
				ps.println("<!DOCTYPE html>");
				ps.println("<html xmlns:th=\"http://www.thymeleaf.org\">");
				ps.println("<body>");
				ps.println("	<ul class=\"navbar-nav mr-auto\">");
				ps.println("		<li th:fragment=\"guiMenu\" id=\"guiMenu\" class=\"nav-item dropdown\"><a");
				ps.println("			href=\"#\" class=\"nav-link dropdown-toggle\" data-toggle=\"dropdown\"");
				ps.println("			aria-haspopup=\"true\" aria-expanded=\"false\"><span");
				ps.println("				th:text=\"#{header.gui}\"></span> <b class=\"caret\"></b> </a>");
				ps.println("			<ul class=\"dropdown-menu bg-dark\">");
				for (String className : set) {
					String li = makeAdminOnly(ps, classHasUserFields(className), "li");
					String fieldName = className.substring(0, 1).toLowerCase() + className.substring(1);
					ps.println("								<" + li + " th:classappend=\"${module == '" + fieldName
							+ "' ? 'active' : ''}\">");
					ps.println("    								<a id=\"guiItem" + className + "\" th:href=\"@{/"
							+ fieldName + "s}\" th:text=\"#{class." + className + "}\"></a></li>");
				}
				ps.println("			</ul></li>");
				ps.println("		<li th:fragment=\"restMenu\" id=\"restMenu\" class=\"nav-item dropdown\"><a");
				ps.println("			href=\"#\" class=\"nav-link dropdown-toggle\" data-toggle=\"dropdown\"");
				ps.println("			aria-haspopup=\"true\" aria-expanded=\"false\"><span");
				ps.println("				th:text=\"#{header.restApi}\"></span> <b class=\"caret\"></b> </a>");
				ps.println("			<ul class=\"dropdown-menu bg-dark\">");
				for (String className : set) {
					String li = makeAdminOnly(ps, classHasUserFields(className), "li");
					String fieldName = className.substring(0, 1).toLowerCase() + className.substring(1);
					ps.println("								<" + li + " th:classappend=\"${module == '" + fieldName
							+ "' ? 'active' : ''}\">");
					ps.println(
							"    								<a id=\"apiItem" + className + "\" th:href=\"@{/api/"
									+ fieldName + "s}\" th:text=\"#{class." + className + "}\"></a></li>");
				}
				ps.println("			</ul></li>");
				ps.println("		<li th:fragment=\"staticMenu\" id=\"staticMenu\" class=\"nav-item dropdown\">");
				if (statics != null && !statics.isEmpty()) {
					ps.println("			<a href=\"#\" class=\"nav-link dropdown-toggle\" data-toggle=\"dropdown\"");
					ps.println("			aria-haspopup=\"true\" aria-expanded=\"false\"><span");
					ps.println("				th:text=\"#{header.static}\"></span> <b class=\"caret\"></b> </a>");
					ps.println("			<ul class=\"dropdown-menu bg-dark\">");
					for (String name : statics.keySet()) {
						String url = statics.get(name);
						ps.println("								<li>");
						ps.println("    								<a id=\"" + name + "\" th:href=\"@{" + url
								+ "}\" th:text=\"#{static." + name + "}\"></a></li>");
					}
					ps.println("							</ul>");
				}
				ps.println("							</li>");
				ps.println("					</ul>");
				ps.println("</body>");
				ps.println("</html>");
				log.warn("Wrote:" + p.toString());
			} catch (Exception e) {
				log.error("failed to create " + p, e);
				p.toFile().delete();
			}
		}
	}

	/**
	 * Copy the files in static/[bundelName] to the new project (if they do not
	 * exist) creating any needed folders along the way.
	 * 
	 * @throws IOException
	 */
	public void copyCustom() throws IOException {
		Path staticPath = Utils.getPath("static/" + getBundelName());
		Files.walkFileTree(staticPath, new FileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				String relDir = staticPath.relativize(dir).toString().replace('\\', '/').replace(srcPath, basePath);
				Path target = Utils.getPath(baseDir, relDir);
				Files.createDirectories(target);

				log.debug("preVisitDirectory: " + dir + "->" + target);
				return FileVisitResult.CONTINUE;
			}

			/**
			 * Copy the custom file into new tree
			 */
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				String relPath = staticPath.relativize(file).toString().replace('\\', '/').replace(srcPath, basePath);

				Path p = Utils.createFile(baseDir, relPath);
				if (p != null) {
					try {
						Files.copy(file, p, StandardCopyOption.REPLACE_EXISTING);
						log.warn("Wrote:" + p.toString());
					} catch (Exception e) {
						log.error("failed to create " + p, e);
					}
				} else {
					log.warn("Exists. skipping:" + relPath);
				}
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				log.debug("visitFileFailed: " + file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				log.debug("postVisitDirectory: " + dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	/**
	 * Convert or copy files in Java2VM.TEMPLATE_FOLDER to new folder as needed
	 * creating any needed folders as well.
	 * 
	 * @throws IOException
	 */
	public void copyCommon(Java2VM j2m) throws IOException {
		Path staticPath = Utils.getPath(Java2VM.TEMPLATE_FOLDER);
		Files.walkFileTree(staticPath, new FileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				String relDir = staticPath.relativize(dir).toString().replace('\\', '/').replace(srcPath, basePath);
				Path target = Utils.getPath(baseDir, relDir);
				Files.createDirectories(target);

				log.debug("preVisitDirectory: " + dir + "->" + target);
				return FileVisitResult.CONTINUE;
			}

			/**
			 * Copy file into new tree converting package / paths as needed
			 */
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				String relPath = staticPath.relativize(file).toString().replace('\\', '/').replace(srcPath, basePath);

				if (file.toString().endsWith(".vm")) {
					log.info("Converting:" + file);
					relPath = relPath.substring(0, relPath.length() - 3);
					j2m.vm2java(file.toString(), relPath);
				} else {
					Path p = Utils.createFile(baseDir, relPath);
					if (p != null) {
						try {
							Files.copy(file, p, StandardCopyOption.REPLACE_EXISTING);
							log.warn("Wrote:" + p.toString());
						} catch (Exception e) {
							log.error("failed to create " + p, e);
						}
					} else {
						log.warn("Exists. skipping:" + relPath);
					}
				}
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				log.debug("visitFileFailed: " + file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				log.debug("postVisitDirectory: " + dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	/**
	 * Generate folder structure and project level files
	 * 
	 * @param tableNames
	 * @throws Exception
	 */
	public void writeProject(List<String> tableNames) throws Exception {

		Map<String, Map<String, ColInfo>> colsInfo = new HashMap<String, Map<String, ColInfo>>();
		for (String tableName : tableNames) {
			String className = Utils.tabToStr(renames, tableName);
			colsInfo.put(className, gatherTableInfo(tableName));
		}
		Java2VM j2m = new Java2VM(getBundelName());
		addMockBaseVars(colsInfo, j2m);

		copyCustom();

		copyCommon(j2m);

		writeAppProps();
		writeTestProps();

		genTableMaintFiles(tableNames, colsInfo);

		writeApiController(colsInfo.keySet());
		writeApiControllerTest(colsInfo);
		writeNav(colsInfo.keySet(), null);
		writeIndex(colsInfo.keySet());
		writeApiIndex(colsInfo.keySet());
		updateMsgProps(colsInfo);
		updateREADME(colsInfo);
	}

	private StringBuilder commentAdd(StringBuilder sb, String comment) {
		if (sb.length() > 0)
			sb.append(System.lineSeparator());
		return sb.append(" * ").append(comment).append("<br>");
	}

	/**
	 * looks for columnName in the list in a case insensitive way and return if
	 * found
	 * 
	 * @param list
	 * @param columnName
	 * @param ifEmpty    what to return if list is empty or null
	 * @return
	 */
	private boolean caseIgnoreListContains(List<String> list, String columnName, boolean ifEmpty) {
		if (list == null || list.isEmpty())
			return ifEmpty;

		return list.stream().anyMatch(columnName::equalsIgnoreCase);
	}

	/**
	 * Entry point for generating all the files you need for Spring maint screens to
	 * Add/Edit/Delete/Search record for a table.
	 * 
	 * @param tableName
	 * @throws Exception
	 */
	public Map<String, ColInfo> gatherTableInfo(String tableName) throws Exception {

		String firstColumnName = null;
		String className = Utils.tabToStr(renames, tableName);

		Connection conn = db.getConnection(PROPKEY + ".genFiles()");

		log.debug("tableName:" + tableName);
		// Map indexed by column name
		List<String> jsonIgnoreCols = Utils.getPropList(bundle, className + ".JsonIgnore");
		List<String> uniqueCols = Utils.getPropList(bundle, className + ".unique");
		List<String> passwordCols = Utils.getPropList(bundle, className + ".passwords");
		List<String> emailCols = Utils.getPropList(bundle, className + ".email");
		List<String> listCols = Utils.getPropList(bundle, className + ".list");

		DatabaseMetaData meta = conn.getMetaData();
		ResultSet rs = meta.getColumns(null, null, tableName, null);
		ResultSetMetaData rm = rs.getMetaData();
		Map<String, ColInfo> colNameToInfoMap = new TreeMap<String, ColInfo>();
		String catalog = "";
		while (rs.next()) {
			if (log.isDebugEnabled()) {
				for (int i = 1; i <= rm.getColumnCount(); i++) {
					String columnName = rm.getColumnName(i);
					log.debug(columnName + "='" + rs.getString(columnName) + "'");
				}
			}
			catalog = rs.getString(TABLE_CAT);
			ColInfo colInfo = new ColInfo();
			colInfo.setFNum(rs.getInt(ORDINAL_POSITION) + 1);
			String columnName = rs.getString(COLUMN_NAME);
			colInfo.setColName(columnName);
			if (firstColumnName == null) {
				firstColumnName = columnName;
			}
			// process mod lists
			colInfo.setPassword(caseIgnoreListContains(passwordCols, columnName, false));
			colInfo.setEmail(caseIgnoreListContains(emailCols, columnName, false));
			colInfo.setJsonIgnore(caseIgnoreListContains(jsonIgnoreCols, columnName, false));
			colInfo.setUnique(caseIgnoreListContains(uniqueCols, columnName, false));
			if (colInfo.isPk())
				colInfo.setList(false);
			else
				colInfo.setList(caseIgnoreListContains(listCols, columnName, true));
			if (db.isSqlserver()) {
				try {
					String autoinc = rs.getString(IS_AUTOINCREMENT);
					if ("YES".equals(autoinc))
						colInfo.setPk(true);
				} catch (SQLException e) {
					log.error("Failed checking IS_AUTOINCREMENT for:" + columnName, e);
				}
			}
			colInfo.setRequired(rs.getInt(NULLABLE) == 0);
			int stype = rs.getInt(DATA_TYPE);
			if (isSQLite()) {
				String typ = rs.getString(TYPE_NAME);
				if ("DATETIME".equalsIgnoreCase(typ)) {
					stype = Types.TIMESTAMP;
				}
			}
			colInfo.setStype(stype);
			switch (stype) {
			case Types.VARCHAR:
			case Types.NVARCHAR:
			case Types.LONGNVARCHAR:
			case Types.LONGVARCHAR:
			case Types.BLOB:
			case Types.CHAR:
			case Types.SQLXML:
				colInfo.setType("String");
				String tmp = null;
				if (isSQLite()) {
					tmp = rs.getString(TYPE_NAME);
					int s = tmp.indexOf('(');
					if (s > -1) {
						tmp = tmp.substring(s + 1, tmp.length() - 1);
					} else {
						tmp = null;
						log.warn(columnName + " def has not len: " + tmp);
					}
					// MySQL seems to have issue the reporting varchar lens>100
				} else if (db.isMySQL()) {
					tmp = rs.getString(COLUMN_SIZE);
					if ("100".equals(tmp)) {
						tmp = rs.getString(REMARKS);
						if (tmp != null && tmp.startsWith("len=")) {
							tmp = tmp.substring(4);
							log.warn(columnName + " Using REMARKS: " + tmp);
						} else {
							tmp = null;
						}
					} else {
						log.warn(columnName + " Using COLUMN_SIZE: " + tmp);
					}
				} else { // most others
					tmp = rs.getString(COLUMN_SIZE);
					log.warn(columnName + " Using COLUMN_SIZE: " + tmp);
				}
				if (colInfo.isPassword()) {
					// See PasswordConstraintValidator
					colInfo.setLength(maxPassLen);
				} else if (!StringUtils.isBlank(tmp)) {
					try {
						colInfo.setLength(Integer.parseInt(tmp));
					} catch (NumberFormatException e) {
						NumberFormatException e2 = new NumberFormatException(
								"Failed to parse tmp:" + tmp + " from TYPE_NAME:" + rs.getString(TYPE_NAME)
										+ " COLUMN_SIZE:" + rs.getString(COLUMN_SIZE));
						e2.initCause(e);
						throw e2;
					}
				} else {
					// SQLite
					colInfo.setLength(255);
				}

				break;

			case Types.REAL:
				colInfo.setType("Float");
				colInfo.setColPrecision(rs.getInt(COLUMN_SIZE));
				colInfo.setColScale(rs.getInt(DECIMAL_DIGITS));
				break;

			case Types.FLOAT:
			case Types.DECIMAL:
			case Types.DOUBLE:
			case Types.NUMERIC:
				if (useDouble) {
					colInfo.setType("Double");
				} else {
					colInfo.setType("BigDecimal");
				}
				colInfo.setColPrecision(rs.getInt(COLUMN_SIZE));
				colInfo.setColScale(rs.getInt(DECIMAL_DIGITS));
				break;

			case Types.INTEGER:
			case Types.SMALLINT:
			case Types.TINYINT:
				colInfo.setType("Integer");
				break;
			case Types.BIGINT:
				colInfo.setType("Long");
				break;
			case Types.TIMESTAMP:
			case Types.DATE:
				colInfo.setType("Date");
				break;
			case Types.VARBINARY:
			case Types.CLOB:
				colInfo.setType("byte[]");
				break;
			default:
				// if its something else treat it like a String for now
				System.err.println("Type " + stype + " Unknown treating " + columnName + " like String");
				colInfo.setType("String");
			}

//			colInfo.setJtype(rm.getColumnClassName(i));

			if (columnName.equalsIgnoreCase(colCreated)) {
				colInfo.setCreated(true);
			}

			if (columnName.equalsIgnoreCase(colLastMod)) {
				colInfo.setLastMod(true);
			}

			columnName = Utils.tabToStr(renames, columnName);
			colInfo.setVName(columnName.substring(0, 1).toLowerCase() + columnName.substring(1));
			colInfo.setMsgKey(className + "." + colInfo.getVName());
			colInfo.setGsName(columnName);

			if (colInfo.getColName() != null && colInfo.getType() != null && colInfo.getType().length() > 0) {
				log.info("storing:" + colInfo);
				colNameToInfoMap.put(colInfo.getColName().toLowerCase(), colInfo);
				if (colInfo.isPk())
					colNameToInfoMap.put(PKEY_INFO, colInfo);
			}
		}

		// write bean with helpers

		// MySQL
		// SHOW KEYS FROM table WHERE Key_name = 'PRIMARY'
//		SELECT 
//		  TABLE_NAME,COLUMN_NAME,CONSTRAINT_NAME, REFERENCED_TABLE_NAME,REFERENCED_COLUMN_NAME
//		FROM
//		  INFORMATION_SCHEMA.KEY_COLUMN_USAGE
//		WHERE
//		  REFERENCED_TABLE_SCHEMA = '<database>' AND
//		  REFERENCED_TABLE_NAME = '<table>';

		// SQLite
		// SELECT * FROM pragma_table_info('my_table');
		// SELECT * FROM pragma_foreign_key_list('my_table');
		// TODO: add support for composite keys issue #17
		String pkCol = null;
		DatabaseMetaData metaData = conn.getMetaData();
		rs = metaData.getPrimaryKeys(catalog, schema, tableName);
		if (rs.next()) {
			pkCol = rs.getString("COLUMN_NAME");
			ColInfo pkinfo = colNameToInfoMap.get(pkCol.toLowerCase());
			if (pkinfo == null) {
				throw new Exception("PK returned by getPrimaryKeys() not found in column list!");
			}
			// validate we know it is PK
			if (!pkinfo.isPk()) {
				pkinfo.setPk(true);
				colNameToInfoMap.put(PKEY_INFO, pkinfo);
			}
			StringBuilder comment = new StringBuilder();
			comment = commentAdd(comment, "Table name: " + rs.getString("TABLE_NAME"));
			comment = commentAdd(comment, "Column name: " + pkCol);
			comment = commentAdd(comment, "Catalog name: " + rs.getString("TABLE_CAT"));
			comment = commentAdd(comment, "Primary key sequence: " + rs.getString("KEY_SEQ"));
			comment = commentAdd(comment, "Primary key name: " + rs.getString("PK_NAME"));
			comment = commentAdd(comment, " ");
			pkinfo.setComment(comment.toString());
		}
		if (colNameToInfoMap.get(PKEY_INFO) == null) {
			pkCol = firstColumnName;
			log.error(tableName + " does not have a primary key. Using " + pkCol);
			ColInfo pkinfo = colNameToInfoMap.get(pkCol.toLowerCase());
			if (pkinfo == null) {
				throw new Exception("PK returned by getPrimaryKeys() not found in column list!");
			}
			// validate we know it is PK
			if (!pkinfo.isPk()) {
				pkinfo.setPk(true);
				colNameToInfoMap.put(PKEY_INFO, pkinfo);
			}
		}

		rs = metaData.getImportedKeys(catalog, schema, tableName);
		while (rs.next()) {
			ColInfo ci = colNameToInfoMap.get(rs.getString("FKCOLUMN_NAME").toLowerCase());
			String tnam = rs.getString("PKTABLE_NAME");
			ci.setForeignTable(tnam);
			String fkClsName = Utils.tabToStr(renames, tnam);

			ci.setForeignCol(rs.getString("PKCOLUMN_NAME"));
			ci.setVName(tnam.toLowerCase());
			ci.setType(fkClsName);
			ci.setGsName(tnam.substring(0, 1).toUpperCase() + tnam.substring(1).toLowerCase());

			StringBuilder comment = new StringBuilder();
			comment = commentAdd(comment, rs.getString("FKCOLUMN_NAME") + " => foreign key column name");
			comment = commentAdd(comment,
					rs.getString("PKTABLE_CAT") + " => primary key table catalog being imported (may be null)");
			comment = commentAdd(comment,
					rs.getString("PKTABLE_SCHEM") + " => primary key table schema being imported (may be null) ");
			comment = commentAdd(comment, rs.getString("PKTABLE_NAME") + " => primary key table name being imported ");
			comment = commentAdd(comment, rs.getString("PKCOLUMN_NAME") + " => primary key column name being imported");
			comment = commentAdd(comment, rs.getString("FKTABLE_CAT") + " => foreign key table catalog (may be null)");
			comment = commentAdd(comment, rs.getString("FKTABLE_SCHEM") + " => foreign key table schema (may be null)");
			comment = commentAdd(comment, rs.getString("FKTABLE_NAME") + " => foreign key table name ");
			comment = commentAdd(comment, rs.getString("KEY_SEQ")
					+ " => sequence number within a foreign key( a valueof 1 represents the first column of the foreign key, a value of 2 would represent the second column within the foreign key).");
			comment = commentAdd(comment,
					rs.getString("UPDATE_RULE") + " => What happens to a foreign key when the primary key is updated:");

			comment = commentAdd(comment,
					rs.getString("DELETE_RULE") + " => What happens to the foreign key when primary is deleted.");
			comment = commentAdd(comment, rs.getString("FK_NAME") + " => foreign key name (may be null) ");
			comment = commentAdd(comment, rs.getString("PK_NAME") + " => primary key name (may be null) ");
			comment = commentAdd(comment, rs.getString("DEFERRABILITY") + " DEFERRABILITY");
			comment = commentAdd(comment, " ");
			ci.setComment(comment.toString());
		}
		db.close(PROPKEY + ".genFiles()");

		return colNameToInfoMap;
	}

	private void genTableMaintFiles(List<String> tableNames, Map<String, Map<String, ColInfo>> colsInfo)
			throws Exception {
		for (String tableName : tableNames) {
			writeBean(tableName, colsInfo);
			writeForm(tableName, colsInfo);
			writeSearchForm(tableName, colsInfo);
			writeRepo(tableName, colsInfo);
			writeService(tableName, colsInfo);
			writeListPage(tableName, colsInfo);
			writeObjController(tableName, colsInfo);
			writeObjControllerTest(tableName, colsInfo);
			writeEditPage(tableName, colsInfo);
			writeSearchPage(tableName, colsInfo);
		}
	}

	/**
	 * Generate comment header for a class
	 * 
	 * @param className   Used for Title:
	 * @param description What to put after Description:
	 * @param comment     Any additional comment to add the header
	 * @return
	 */
	private String getClassHeader(String className, String description, String comment) {
		StringBuilder sb = new StringBuilder();
		sb.append("/**").append(System.lineSeparator());
		sb.append(" * Title: " + className + " <br>").append(System.lineSeparator());
		sb.append(" * Description: " + description + " <br>").append(System.lineSeparator());
		String tmp = Utils.getProp(bundle, PROPKEY + ".Copyright", "");
		if (!StringUtils.isBlank(tmp)) {
			sb.append(" * Copyright: " + tmp + year + "<br>").append(System.lineSeparator());
		}
		tmp = Utils.getProp(bundle, PROPKEY + ".Company", "");
		if (!StringUtils.isBlank(tmp)) {
			sb.append(" * Company: " + tmp + "<br>").append(System.lineSeparator());
		}
		sb.append(" * @author Gened by " + this.getClass().getCanonicalName() + " version " + genSpringVersion + "<br>")
				.append(System.lineSeparator());
		sb.append(" * @version " + appVersion + "<br>").append(System.lineSeparator());
		if (!StringUtils.isBlank(comment))
			sb.append(comment);
		sb.append(" */");

		return sb.toString();
	}

	private void writeApiController(Set<String> set) {
		String pkgNam = basePkg + ".controller";
		String relPath = pkgNam.replace('.', '/');
		String outFile = "src/main/java/" + relPath + "/ApiController.java";
		Path p = Utils.createFile(baseDir, outFile);
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
				ps.println("package " + pkgNam + ';');
				ps.println("");
				ps.println("import org.springframework.beans.factory.annotation.Autowired;");
				ps.println("import org.springframework.data.domain.Page;");
				ps.println("import org.springframework.web.bind.annotation.GetMapping;");
				ps.println("import org.springframework.web.bind.annotation.RequestMapping;");
				ps.println("import org.springframework.web.bind.annotation.RestController;");
				for (String className : set) {
					ps.println("import " + basePkg + ".entity." + className + ";");
					ps.println("import " + basePkg + ".service." + className + "Services;");
				}
				ps.println("");
				ps.println("import java.util.List;");
				ps.println(getClassHeader("ApiController", "Api REST Controller.", null));
				ps.println("@RestController");
				ps.println("@RequestMapping(\"/api\")");
				ps.println("public class ApiController {");
				ps.println("");
				for (String className : set) {
					String fieldName = className.substring(0, 1).toLowerCase() + className.substring(1);
					ps.println("    @Autowired");
					ps.println("    private " + className + "Services " + fieldName + "Services;");
				}
				ps.println("");
				ps.println("    public ApiController(){");
				ps.println("    }");
				for (String className : set) {
					String fieldName = className.substring(0, 1).toLowerCase() + className.substring(1);
					ps.println("");
					ps.println("    @GetMapping(\"/" + fieldName + "s\")");
					ps.println("    public List<" + className + "> getAll" + className + "s(){");
					ps.println("        return this." + fieldName + "Services.listAll(null).toList();");
					ps.println("    }");
				}
				ps.println("}");
				log.warn("Wrote:" + p.toString());
			} catch (Exception e) {
				log.error("failed to create " + p, e);
				p.toFile().delete();
			}
		}
	}

	/**
	 * Add and the class and field names to src/main/resources/messages.properties
	 * 
	 * @param colsInfo
	 */
	private void updateMsgProps(Map<String, Map<String, ColInfo>> colsInfo) {
		Path p = Utils.getPath(baseDir, "src/main/resources/messages.properties");
		String data = "";
		boolean dataChged = false;
		try {
			data = new String(Files.readAllBytes(p));
			data = data + "app.version=" + appVersion + System.lineSeparator();
			data = data + "app.name=" + appName + System.lineSeparator();
			data = data + "app.description=" + appDescription + System.lineSeparator();
			data = data + "index.greeting=Welcome to the " + appName + " application!" + System.lineSeparator();
			Set<String> set = colsInfo.keySet();
			for (String className : set) {
				// If table already in there then skip
				if (!data.contains("## for " + className + System.lineSeparator())) {
					dataChged = true;
					data = data + "## for " + className + System.lineSeparator();
					data = data + "class." + className + "=" + className + System.lineSeparator();
					Map<String, ColInfo> colNameToInfoMap = colsInfo.get(className);
					for (String key : colNameToInfoMap.keySet()) {
						if (PKEY_INFO.equals(key))
							continue;
						ColInfo info = (ColInfo) colNameToInfoMap.get(key);
						data = data + info.getMsgKey() + "=" + info.getGsName() + System.lineSeparator();
						if (info.isPassword()) {
							data = data + info.getMsgKey() + "Confirm=" + info.getGsName() + " Confirm"
									+ System.lineSeparator();
						}
					}
					data = data + System.lineSeparator();
				}
			}
			if (dataChged) {
				Files.write(p, data.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
				log.warn("Wrote:" + p.toString());
			} else {
				log.warn(p.toString() + " up to date so will skip.");
			}
		} catch (Exception e) {
			log.error("failed to update " + p, e);
		}
	}

	/**
	 * Add screen shot links to the README.md page
	 * 
	 * @param colsInfo
	 */
	private void updateREADME(Map<String, Map<String, ColInfo>> colsInfo) {
		Path p = Utils.getPath(baseDir, "README.md");
		String data = "";
		boolean dataChged = false;
		try {
			data = new String(Files.readAllBytes(p));
			Set<String> set = colsInfo.keySet();
			// If already in there then skip
			if (!data.contains("### Admin screens" + System.lineSeparator())) {
				data = data + System.lineSeparator() + "### Admin screens" + System.lineSeparator();
				for (String className : set) {
					if (classHasUserFields(className)) {
						dataChged = true;
						data = data + "#### " + className + " screens" + System.lineSeparator();
						data = data + "![" + className + " list screen](screenshots/" + className + ".list.png)"
								+ System.lineSeparator();
						data = data + "![" + className + " new screen](screenshots/" + className + ".new.png)"
								+ System.lineSeparator();
						data = data + "![" + className + " edit screen](screenshots/" + className + ".edit.png)"
								+ System.lineSeparator();
						data = data + System.lineSeparator();
					}
				}
			}
			if (!data.contains("### User screens" + System.lineSeparator())) {
				data = data + System.lineSeparator() + "### User screens" + System.lineSeparator();
				for (String className : set) {
					if (!classHasUserFields(className)) {
						dataChged = true;
						data = data + "#### " + className + " screens" + System.lineSeparator();
						data = data + "![" + className + " list screen](screenshots/" + className + ".list.png)"
								+ System.lineSeparator();
						data = data + "![" + className + " new screen](screenshots/" + className + ".new.png)"
								+ System.lineSeparator();
						data = data + "![" + className + " edit screen](screenshots/" + className + ".edit.png)"
								+ System.lineSeparator();
						data = data + System.lineSeparator();
					}
				}
			}
			if (dataChged) {
				Files.write(p, data.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
				log.warn("Wrote:" + p.toString());
			} else {
				log.warn(p.toString() + " up to date so will skip.");
			}
		} catch (Exception e) {
			log.error("failed to update " + p, e);
		}
	}

	/**
	 * Created mock unit test of ApiController.
	 * 
	 * @param colsInfo
	 */
	private void writeApiControllerTest(Map<String, Map<String, ColInfo>> colsInfo) {
		Set<String> set = colsInfo.keySet();
		String pkgNam = basePkg + ".controller";
		String relPath = pkgNam.replace('.', '/');
		Path p = Utils.createFile(baseDir, "src/test/java", relPath, "ApiControllerTest.java");
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
				ps.println("package " + pkgNam + ';');
				ps.println("");
				ps.println("import static org.hamcrest.CoreMatchers.containsString;");
				ps.println("import static org.mockito.BDDMockito.given;");
				ps.println(
						"import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;");
				ps.println("import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;");
				ps.println("import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;");
				ps.println("import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;");
				ps.println("");
				ps.println("import java.util.ArrayList;");
				ps.println("import java.util.Iterator;");
				ps.println("import java.util.List;");
				ps.println("import java.util.function.Function;");
				ps.println("");
				ps.println("import org.junit.Test;");
				ps.println("import org.junit.runner.RunWith;");
				ps.println("import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;");
				ps.println("import org.springframework.data.domain.Page;");
				ps.println("import org.springframework.data.domain.Pageable;");
				ps.println("import org.springframework.data.domain.Sort;");
				ps.println("import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;");
				ps.println("");
				ps.println("import " + basePkg + ".MockBase;");
				for (String className : set) {
					ps.println("import " + basePkg + ".entity." + className + ";");
				}
				ps.println("");
				ps.println(getClassHeader("ApiControllerTest", "REST Api Controller Test.", null));
				ps.println("@RunWith(SpringJUnit4ClassRunner.class)");
				ps.println("@WebMvcTest(ApiController.class)");
				ps.println("public class ApiControllerTest extends MockBase {");
				ps.println("");
				for (String className : set) {
					Map<String, ColInfo> colNameToInfoMap = colsInfo.get(className);
					ColInfo pkinfo = colNameToInfoMap.get(PKEY_INFO);
					if (pkinfo == null) {
						log.error("No PK found for " + className);
						System.exit(2);
					}
					String fieldName = className.substring(0, 1).toLowerCase() + className.substring(1);
					ps.println("");
					ps.println("	/**");
					ps.println("	 * Test method for");
					ps.println("	 * {@link " + basePkg + ".controller." + className + "Controller#getAll" + className
							+ "s(org.springframework.ui.Model)}.");
					ps.println("	 */");
					ps.println("	@Test");
					ps.println("	public void testGetAll" + className + "s() throws Exception {");
					ps.println("		List<" + className + "> list = new ArrayList<>();");
					ps.println("		" + className + " o = new " + className + "();");
					for (String key : colNameToInfoMap.keySet()) {
						if (PKEY_INFO.equals(key))
							continue;
						ColInfo info = (ColInfo) colNameToInfoMap.get(key);
						if (info.isString()) {
							int endIndex = info.getLength();
							if (endIndex > 0) {
								if (info.isPassword())
									ps.println("        o.set" + info.getGsName() + "(getTestPasswordString(" + endIndex
											+ "));");
								else if (info.isEmail())
									ps.println("        o.set" + info.getGsName() + "(getTestEmailString(" + endIndex
											+ "));");
								else
									ps.println(
											"        o.set" + info.getGsName() + "(getTestString(" + endIndex + "));");
							}
						} else if (info.isPk()) {
							ps.println("		o.set" + info.getGsName() + "(1" + pkinfo.getMod() + ");");
						}
					}
					ps.println("		list.add(o);");
					ps.println("		Page<" + className + "> p = getPage(list);");
					ps.println("		given(" + fieldName + "Services.listAll(null)).willReturn(p);");
					ps.println("");
					ps.println("		this.mockMvc.perform(get(\"/api/" + fieldName
							+ "s\").with(user(\"user\").roles(\"ADMIN\"))).andExpect(status().isOk())");
					for (String key : colNameToInfoMap.keySet()) {
						if (PKEY_INFO.equals(key))
							continue;
						ColInfo info = (ColInfo) colNameToInfoMap.get(key);
						if (info.isJsonIgnore())
							continue;
						if (info.isString()) {
							int endIndex = info.getLength();
							if (endIndex > 0) {
								ps.println("				.andExpect(content().string(containsString(o.get"
										+ info.getGsName() + "())))");
							}
						}
						ps.print("				.andExpect(content().string(containsString(\"" + info.getVName()
								+ "\")))");
					}
					ps.println(";");
					ps.println("	}");
					ps.println("");
				}
				ps.println("}");
				log.warn("Wrote:" + p.toString());
			} catch (Exception e) {
				log.error("failed to create " + p, e);
				p.toFile().delete();
			}
		}
	}

	/**
	 * gen HTML header for page
	 * 
	 * @param className or key to be used for the title
	 * @param pageType  it null className is assumed to be a message key. Should
	 *                  match property in messages.properties as in listView for
	 *                  edit.listView
	 * @return
	 */
	private String htmlHeader(String className, String pageType) {
		StringBuilder sb = new StringBuilder();

		sb.append("<!DOCTYPE html>" + System.lineSeparator());
		sb.append("<html xmlns:th=\"http://www.thymeleaf.org\">" + System.lineSeparator());
		sb.append(
				"<head th:replace=\"fragments/header :: common_header(~{::title},~{},~{})\">" + System.lineSeparator());
		if (pageType == null)
			sb.append("<title th:text=\"#{" + className + "}\"></title>" + System.lineSeparator());
		else
			sb.append("<title th:text=\"#{edit." + pageType + "} + ' ' + #{class." + className + "}\"></title>"
					+ System.lineSeparator());
		sb.append("</head>" + System.lineSeparator());
		sb.append("<body class=\"d-flex flex-column min-vh-100\">" + System.lineSeparator());

		sb.append("	<div th:replace=\"fragments/header :: header\">&nbsp;</div>" + System.lineSeparator());
		// closed in footer
		sb.append("	<div class=\"wrapper flex-grow-1\">" + System.lineSeparator());

		return sb.toString();
	}

	private String htmlFooter() {
		StringBuilder sb = new StringBuilder();
		sb.append("	</div>");
		sb.append("	<div class=\"mt-auto\" th:insert=\"fragments/footer :: footer\">&copy; 2020 default</div>" + System.lineSeparator());
		sb.append("</body>");
		sb.append("</html>");

		return sb.toString();
	}

	/**
	 * Write the CrudRepository interface for a bean
	 * 
	 * @param className
	 * @param colNameToInfoMap
	 * @param pkType
	 */
	private void writeRepo(String tableName, Map<String, Map<String, ColInfo>> colsInfo) throws Exception {
		String className = Utils.tabToStr(renames, tableName);
		Map<String, ColInfo> colNameToInfoMap = colsInfo.get(className);

		ColInfo pkinfo = colNameToInfoMap.get(PKEY_INFO);
		String pkgNam = basePkg + ".repo";
		String relPath = pkgNam.replace('.', '/');
		Path p = Utils.createFile(baseDir, "src/main/java", relPath, className + "Repository.java");
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
				ps.println("package " + pkgNam + ';');
				ps.println("");
				ps.println("import org.springframework.data.jpa.repository.JpaSpecificationExecutor;");
				ps.println("import org.springframework.data.repository.CrudRepository;");
				ps.println("import org.springframework.stereotype.Repository;");
				ps.println("import " + basePkg + ".entity." + className + ";");
				ps.println("");
				ps.println(
						getClassHeader(className + "Repository", "Class for the " + className + " Repository.", null));
				ps.println("@Repository");
				ps.println("public interface " + className + "Repository extends CrudRepository<" + className + ", "
						+ pkinfo.getType() + ">,");
				ps.println("JpaSpecificationExecutor<" + className + "> {");
				if ((pkinfo.getStype() != Types.INTEGER) && (pkinfo.getStype() != Types.BIGINT)) {
					ps.println(
							"//TODO: Primary key is not int or bigint which will require custom code to be added below");
				}
//				// add special methods
//				if (ACCOUNT_CLASS.equals(className)) {
//					ps.println("	" + ACCOUNT_CLASS + " findOneByEmail(String email);");
//					ps.println("	" + ACCOUNT_CLASS + " findOneByEmailAndPassword(String email, String password);");
//					ps.println("");
//				}
				ps.println("}");
				log.warn("Wrote:" + p.toString());
			} catch (Exception e) {
				log.error("failed to create " + p, e);
				p.toFile().delete();
			}
		}
	}

	/**
	 * Add line to make tag it is inserted in viewable by ADMIN_ROLE only if addit
	 * is true
	 * 
	 * @param ps    PrintStream to write to
	 * @param addit Generally an expression like ACCOUNT_CLASS.equals(className)||
	 *              info.isAdminOnly()
	 * @param tag   to preappend to return
	 * @return returns if addit is true then tag+" sec:authorize=\"hasRole('" +
	 *         ADMIN_ROLE + "')\" ". if addit false returns tag.
	 */
	private String makeAdminOnly(PrintStream ps, boolean addit, String tag) {
		if (addit)
			return tag + " sec:authorize=\"hasRole('" + ADMIN_ROLE + "')\" ";

		return tag;
	}

	/**
	 * 
	 * @param ps
	 * @param parentStr
	 * @param className
	 * @param colsInfo
	 * @param Edit      form name plus . on end
	 * @throws Exception
	 */
	private void writeEditSubObjects(PrintStream ps, String parentStr, String className,
			Map<String, Map<String, ColInfo>> colsInfo, String form) throws Exception {
		Map<String, ColInfo> colNameToInfoMap = colsInfo.get(className);

		String field = className.substring(0, 1).toLowerCase() + className.substring(1);
		String fieldName = parentStr + field;
		// guard against null objects and sub objects in list
		String div = "div th:if=\"${" + form + fieldName + " != null}\"";

		for (String key : colNameToInfoMap.keySet()) {
			if (PKEY_INFO.equals(key))
				continue;
			ColInfo info = (ColInfo) colNameToInfoMap.get(key);
			if (info.isPk()) {
				ps.println("				<input type=\"hidden\" class=\"form-control\" id=\"" + field + "."
						+ info.getVName() + "\" th:field=\"*{" + field + "." + info.getVName() + "}\" />");
			} else if (info.getForeignTable() != null) {
				writeEditSubObjects(ps, fieldName + ".", info.getForeignTable(), colsInfo, form);
			} else if (info.isList()) {
				String unless = "				<div" + " th:unless=\"${" + form + fieldName
						+ " != null}\" th:text=\"${" + form + fieldName + "}\"></div>";
				String divs = makeAdminOnly(ps, ACCOUNT_CLASS.equals(className) || info.isAdminOnly(), "div");
				ps.println("				<" + divs + " class=\"form-group\">");
				ps.println("					<label for=\"" + form + fieldName + "." + info.getVName()
						+ "\" class=\"col-lg-2 control-label\"");
				ps.println("						th:text=\"#{" + info.getMsgKey() + "} + ':'\"></label>");

				ps.println("					<" + div + " class=\"col-lg-10\" id=\"" + form + fieldName + "."
						+ info.getVName() + "\"");
				ps.println(
						"	                        th:text=\"${" + form + fieldName + "." + info.getVName() + "}\">");
				ps.println("					</div>");
				ps.println(unless);
				ps.println("				</div>");
			}

		}

	}

	private void writeEditPage(String tableName, Map<String, Map<String, ColInfo>> colsInfo) throws Exception {
		String className = Utils.tabToStr(renames, tableName);
		Map<String, ColInfo> colNameToInfoMap = colsInfo.get(className);

		ColInfo pkinfo = colNameToInfoMap.get(PKEY_INFO);
		String fieldName = className.substring(0, 1).toLowerCase() + className.substring(1);
		Path p = Utils.createFile(baseDir, "src/main/resources/templates/edit_" + fieldName + ".html");
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
				ps.println(htmlHeader(className, "edit"));
				ps.println("		<form class=\"form-narrow form-horizontal\" method=\"post\"");
				ps.println(
						"			th:action=\"@{/" + fieldName + "s/save}\" th:object=\"${" + fieldName + "Form}\"");
				ps.println("			th:fragment=\"" + fieldName + "Form\">");
				ps.println("			<!--/* Show general error messages when form contains errors */-->");
				ps.println("			<th:block th:if=\"${#fields.hasErrors('${" + fieldName + "Form.*}')}\">");
				ps.println("				<div th:each=\"fieldErrors : ${#fields.errors('${" + fieldName
						+ "Form.*}')}\">");
				ps.println("					<div th:each=\"message : ${fieldErrors.split(';')}\">");
				ps.println("						<div");
				ps.println(
						"							th:replace=\"fragments/alert :: alert (type='danger', message=${message})\">Alert</div>");
				ps.println("					</div>");
				ps.println("				</div>");
				ps.println("			</th:block>");
				ps.println("			<fieldset>");
				ps.println(
						"				<legend th:unless=\"${" + fieldName + "Form." + pkinfo.getVName() + " > 0}\"");
				ps.println("					th:text=\"#{edit.new} + ' ' + #{class." + className + "}\"></legend>");
				ps.println("				<legend th:if=\"${" + fieldName + "Form." + pkinfo.getVName() + " > 0}\"");
				ps.println("					th:text=\"#{edit.edit} + ' ' + #{class." + className + "}\"></legend>");
				ps.println("");
				for (String key : colNameToInfoMap.keySet()) {
					if (PKEY_INFO.equals(key))
						continue;
					ColInfo info = (ColInfo) colNameToInfoMap.get(key);
					if (info.isPk()) {
						ps.println("				<input type=\"hidden\" class=\"form-control\" id=\""
								+ info.getVName() + "\" th:field=\"*{" + info.getVName() + "}\" />");
					} else if (info.getForeignTable() != null) {
						writeEditSubObjects(ps, "", info.getType(), colsInfo, fieldName + "Form.");
					} else {
						ps.println("				<div class=\"form-group\"");
						ps.println("					th:classappend=\"${#fields.hasErrors('" + info.getVName()
								+ "')}? 'has-error'\">");
						ps.println("					<label for=\"" + info.getVName()
								+ "\" class=\"col-lg-2 control-label\"");
						ps.println("						th:text=\"#{" + info.getMsgKey() + "} + ':'\"></label>");
						ps.println("					<div class=\"col-lg-10\">");
						ps.println("						<input type=\"text\" class=\"form-control\" id=\""
								+ info.getVName() + "\"");
						if (info.isLastMod()) {
							ps.println("							readonly");
						}
						ps.println("							th:field=\"*{" + info.getVName() + "}\" />");
						ps.println("						<ul class=\"help-block\"");
						ps.println("							th:each=\"error: ${#fields.errors('" + info.getVName()
								+ "')}\">");
						ps.println("							<li th:each=\"message : ${error.split(';')}\">");
						ps.println(
								"								<p class=\"error-message\" th:text=\"${message}\"></p>");
						ps.println("							</li>");
						ps.println("						</ul>");
						ps.println("					</div>");
						ps.println("				</div>");
						ps.println("");
						if (info.isPassword()) {
							ps.println("				<div class=\"form-group\"");
							ps.println("					th:classappend=\"${#fields.hasErrors('" + info.getVName()
									+ "')}? 'has-error'\">");
							ps.println("					<label for=\"" + info.getVName()
									+ "\" class=\"col-lg-2 control-label\"");
							ps.println("						th:text=\"#{" + info.getMsgKey()
									+ "Confirm} + ':'\"></label>");
							ps.println("					<div class=\"col-lg-10\">");
							ps.println("						<input type=\"text\" class=\"form-control\" id=\""
									+ info.getVName() + "Confirm\"");
							ps.println("							th:field=\"*{" + info.getVName() + "Confirm}\" />");
							ps.println("						<ul class=\"help-block\"");
							ps.println("							th:each=\"error: ${#fields.errors('"
									+ info.getVName() + "Confirm')}\">");
							ps.println("							<li th:each=\"message : ${error.split(';')}\">");
							ps.println(
									"								<p class=\"error-message\" th:text=\"${message}\"></p>");
							ps.println("							</li>");
							ps.println("						</ul>");
							ps.println("					</div>");
							ps.println("				</div>");
							ps.println("");
						}
					}

				}
				ps.println("				<div class=\"form-group\">");
				ps.println("					<div class=\"col-lg-offset-2 col-lg-10\">");
				ps.println("						<button type=\"submit\" name=\"action\" value=\"save\"");
				ps.println("							class=\"btn btn-default\" th:text=\"#{edit.save}\"></button>");
				ps.println(
						"						<button type=\"submit\" name=\"action\" value=\"cancel\" class=\"btn\"");
				ps.println("							th:text=\"#{edit.cancel}\"></button>");
				ps.println("					</div>");
				ps.println("				</div>");
				ps.println("			</fieldset>");
				ps.println("		</form>");
				ps.println("");
				ps.println(htmlFooter());
				log.warn("Wrote:" + p.toString());
			} catch (Exception e) {
				log.error("failed to create " + p, e);
				p.toFile().delete();
			}
		}
	}

	/**
	 * 
	 * @param ps
	 * @param parentStr
	 * @param className
	 * @param colsInfo
	 * @param Edit      form name plus . on end
	 * @throws Exception
	 */
	private void writeSearchSubObjects(PrintStream ps, String parentStr, String className,
			Map<String, Map<String, ColInfo>> colsInfo, String form) throws Exception {
//		Map<String, ColInfo> colNameToInfoMap = colsInfo.get(className);
//
//		String field = className.substring(0, 1).toLowerCase() + className.substring(1);
//		String fieldName = parentStr + field;
//		// guard against null objects and sub objects in list
//		String div = "div th:if=\"${" + form + fieldName + " != null}\"";
//
//		for (String key : colNameToInfoMap.keySet()) {
//			if (PKEY_INFO.equals(key))
//				continue;
//			ColInfo info = (ColInfo) colNameToInfoMap.get(key);
//			if (info.isPk()) {
//				ps.println("				<input type=\"hidden\" class=\"form-control\" id=\"" + field + "."
//						+ info.getVName() + "\" th:field=\"*{" + field + "." + info.getVName() + "}\" />");
//			} else if (info.getForeignTable() != null) {
//				writeEditSubObjects(ps, fieldName + ".", info.getForeignTable(), colsInfo, form);
//			} else if (info.isList()) {
//				String unless = "				<div" + " th:unless=\"${" + form + fieldName
//						+ " != null}\" th:text=\"${" + form + fieldName + "}\"></div>";
//				String divs = makeAdminOnly(ps, ACCOUNT_CLASS.equals(className) || info.isAdminOnly(), "div");
//				ps.println("				<" + divs + " class=\"form-group\">");
//				ps.println("					<label for=\"" + form + fieldName + "." + info.getVName()
//						+ "\" class=\"col-lg-2 control-label\"");
//				ps.println("						th:text=\"#{" + info.getMsgKey() + "} + ':'\"></label>");
//
//				ps.println("					<" + div + " class=\"col-lg-10\" id=\"" + form + fieldName + "."
//						+ info.getVName() + "\"");
//				ps.println(
//						"	                        th:text=\"${" + form + fieldName + "." + info.getVName() + "}\">");
//				ps.println("					</div>");
//				ps.println(unless);
//				ps.println("				</div>");
//			}
//
//		}

	}

	private void writeSearchPage(String tableName, Map<String, Map<String, ColInfo>> colsInfo) throws Exception {
		String className = Utils.tabToStr(renames, tableName);
		Map<String, ColInfo> colNameToInfoMap = colsInfo.get(className);

//		ColInfo pkinfo = colNameToInfoMap.get(PKEY_INFO);
		String fieldName = className.substring(0, 1).toLowerCase() + className.substring(1);
		Path p = Utils.createFile(baseDir, "src/main/resources/templates/search_" + fieldName + ".html");
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
				ps.println(htmlHeader(className, "edit"));
				ps.println("		<form class=\"form-narrow form-horizontal\" method=\"post\"");
				ps.println("			th:action=\"@{/" + fieldName + "s/search}\" th:object=\"${" + fieldName
						+ "SearchForm}\"");
				ps.println("			th:fragment=\"" + fieldName + "SearchForm\">");
				ps.println("			<!--/* Show general error messages when form contains errors */-->");
				ps.println("			<th:block th:if=\"${#fields.hasErrors('${" + fieldName + "SearchForm.*}')}\">");
				ps.println("				<div th:each=\"fieldErrors : ${#fields.errors('${" + fieldName
						+ "SearchForm.*}')}\">");
				ps.println("					<div th:each=\"message : ${fieldErrors.split(';')}\">");
				ps.println("						<div");
				ps.println(
						"							th:replace=\"fragments/alert :: alert (type='danger', message=${message})\">Alert</div>");
				ps.println("					</div>");
				ps.println("				</div>");
				ps.println("			</th:block>");
				ps.println("			<fieldset>");
				ps.println("				<legend th:text=\"#{search.search} + ' ' + #{class." + className
						+ "}\"></legend>");
				ps.println("");
				for (String key : colNameToInfoMap.keySet()) {
					if (PKEY_INFO.equals(key))
						continue;
					ColInfo info = (ColInfo) colNameToInfoMap.get(key);
					if (info.isPk()) {
						// ignore for not at least
//						ps.println("				<input type=\"hidden\" class=\"form-control\" id=\""
//								+ info.getVName() + "\" th:field=\"*{" + info.getVName() + "}\" />");
					} else if (info.getForeignTable() != null) {
						writeSearchSubObjects(ps, "", info.getType(), colsInfo, fieldName + "SearchForm.");
					} else if (info.isString()) {
						ps.println("				<div class=\"form-group\"");
						ps.println("					th:classappend=\"${#fields.hasErrors('" + info.getVName()
								+ "')}? 'has-error'\">");
						ps.println("					<label for=\"" + info.getVName()
								+ "\" class=\"col-lg-2 control-label\"");
						ps.println("						th:text=\"#{" + info.getMsgKey()
								+ "} + ' ' + #{search.like}\"></label>");
						ps.println("					<div class=\"col-lg-10\">");
						ps.println("						<input type=\"text\" class=\"form-control\" id=\""
								+ info.getVName() + "\"");
						ps.println("							th:field=\"*{" + info.getVName() + "}\" />");
						ps.println("						<ul class=\"help-block\"");
						ps.println("							th:each=\"error: ${#fields.errors('" + info.getVName()
								+ "')}\">");
						ps.println("							<li th:each=\"message : ${error.split(';')}\">");
						ps.println(
								"								<p class=\"error-message\" th:text=\"${message}\"></p>");
						ps.println("							</li>");
						ps.println("						</ul>");
						ps.println("					</div>");
						ps.println("				</div>");
						ps.println("");
					} else if (info.isDate()) {
						ps.println("				<div class=\"form-group\"");
						ps.println("					th:classappend=\"${#fields.hasErrors('" + info.getVName()
								+ "Min')}? 'has-error'\">");
						ps.println("					<label for=\"" + info.getVName()
								+ "Min\" class=\"col-lg-2 control-label\"");
						ps.println("						th:text=\"#{" + info.getMsgKey()
								+ "} + ' ' + #{search.after}\"></label>");
						ps.println("					<div class=\"col-lg-10\">");
						ps.println("						<input type=\"text\" class=\"form-control\" id=\""
								+ info.getVName() + "Min\"");
						ps.println("							th:field=\"*{" + info.getVName() + "Min}\" />");
						ps.println("						<ul class=\"help-block\"");
						ps.println("							th:each=\"error: ${#fields.errors('" + info.getVName()
								+ "Min')}\">");
						ps.println("							<li th:each=\"message : ${error.split(';')}\">");
						ps.println(
								"								<p class=\"error-message\" th:text=\"${message}\"></p>");
						ps.println("							</li>");
						ps.println("						</ul>");
						ps.println("					</div>");
						ps.println("				</div>");
						ps.println("");
						ps.println("				<div class=\"form-group\"");
						ps.println("					th:classappend=\"${#fields.hasErrors('" + info.getVName()
								+ "Max')}? 'has-error'\">");
						ps.println("					<label for=\"" + info.getVName()
								+ "Max\" class=\"col-lg-2 control-label\"");
						ps.println("						th:text=\"#{" + info.getMsgKey()
								+ "} + ' ' + #{search.before}\"></label>");
						ps.println("					<div class=\"col-lg-10\">");
						ps.println("						<input type=\"text\" class=\"form-control\" id=\""
								+ info.getVName() + "Max\"");
						ps.println("							th:field=\"*{" + info.getVName() + "Max}\" />");
						ps.println("						<ul class=\"help-block\"");
						ps.println("							th:each=\"error: ${#fields.errors('" + info.getVName()
								+ "Max')}\">");
						ps.println("							<li th:each=\"message : ${error.split(';')}\">");
						ps.println(
								"								<p class=\"error-message\" th:text=\"${message}\"></p>");
						ps.println("							</li>");
						ps.println("						</ul>");
						ps.println("					</div>");
						ps.println("				</div>");
						ps.println("");
					} else { // number assumed
						ps.println("				<div class=\"form-group\"");
						ps.println("					th:classappend=\"${#fields.hasErrors('" + info.getVName()
								+ "Min')}? 'has-error'\">");
						ps.println("					<label for=\"" + info.getVName()
								+ "Min\" class=\"col-lg-2 control-label\"");
						ps.println("						th:text=\"#{" + info.getMsgKey()
								+ "} + ' ' + #{search.gte}\"></label>");
						ps.println("					<div class=\"col-lg-10\">");
						ps.println("						<input type=\"text\" class=\"form-control\" id=\""
								+ info.getVName() + "Min\"");
						ps.println("							th:field=\"*{" + info.getVName() + "Min}\" />");
						ps.println("						<ul class=\"help-block\"");
						ps.println("							th:each=\"error: ${#fields.errors('" + info.getVName()
								+ "Min')}\">");
						ps.println("							<li th:each=\"message : ${error.split(';')}\">");
						ps.println(
								"								<p class=\"error-message\" th:text=\"${message}\"></p>");
						ps.println("							</li>");
						ps.println("						</ul>");
						ps.println("					</div>");
						ps.println("				</div>");
						ps.println("");
						ps.println("				<div class=\"form-group\"");
						ps.println("					th:classappend=\"${#fields.hasErrors('" + info.getVName()
								+ "Max')}? 'has-error'\">");
						ps.println("					<label for=\"" + info.getVName()
								+ "Max\" class=\"col-lg-2 control-label\"");
						ps.println("						th:text=\"#{" + info.getMsgKey()
								+ "} + ' ' + #{search.lte}\"></label>");
						ps.println("					<div class=\"col-lg-10\">");
						ps.println("						<input type=\"text\" class=\"form-control\" id=\""
								+ info.getVName() + "Max\"");
						ps.println("							th:field=\"*{" + info.getVName() + "Max}\" />");
						ps.println("						<ul class=\"help-block\"");
						ps.println("							th:each=\"error: ${#fields.errors('" + info.getVName()
								+ "Max')}\">");
						ps.println("							<li th:each=\"message : ${error.split(';')}\">");
						ps.println(
								"								<p class=\"error-message\" th:text=\"${message}\"></p>");
						ps.println("							</li>");
						ps.println("						</ul>");
						ps.println("					</div>");
						ps.println("				</div>");
						ps.println("");

					}

				}
				ps.println("				<div class=\"form-group\">");
				ps.println("					<div class=\"col-lg-offset-2 col-lg-10\">");
				ps.println("						<button type=\"submit\" name=\"action\" value=\"search\"");
				ps.println(
						"							class=\"btn btn-default\" th:text=\"#{search.search}\"></button>");
				ps.println("					</div>");
				ps.println("				</div>");
				ps.println("			</fieldset>");
				ps.println("		</form>");
				ps.println("");
				ps.println(htmlFooter());
				log.warn("Wrote:" + p.toString());
			} catch (Exception e) {
				log.error("failed to create " + p, e);
				p.toFile().delete();
			}
		}
	}

	/**
	 * Write the row loop for the list table
	 * 
	 * @param ps
	 * @param processed
	 * @param parentStr
	 * @param tableName
	 * @param colsInfo
	 * @throws Exception
	 */
	private void writeListLoop(PrintStream ps, Set<String> processed, String parentStr, String tableName,
			Map<String, Map<String, ColInfo>> colsInfo) throws Exception {
		String className = Utils.tabToStr(renames, tableName);
		String fieldName = parentStr + className.substring(0, 1).toLowerCase() + className.substring(1);
		// guard against null objects and sub objects in list
		String td = "td th:if=\"${" + fieldName + " != null}\"";
		String unless = "				<td th:unless=\"${" + fieldName + " != null}\"></td>";
		Map<String, ColInfo> colNameToInfoMap = colsInfo.get(className);
		for (String key : colNameToInfoMap.keySet()) {
			if (PKEY_INFO.equals(key))
				continue;
			ColInfo info = (ColInfo) colNameToInfoMap.get(key);
			if (info.isPk())
				continue;
			if (!processed.contains(key) && info.isList()) {
				if (info.getForeignTable() != null) {
					writeListLoop(ps, processed, fieldName + ".", info.getForeignTable(), colsInfo);
				} else {
					String tds = makeAdminOnly(ps, ACCOUNT_CLASS.equals(className) || info.isAdminOnly(), td);
					if (colNameToInfoMap.containsKey(key + "link")) {
						ColInfo infoLnk = (ColInfo) colNameToInfoMap.get(key + "link");
						ps.println(
								"	            <" + tds + "><a th:href=\"@{${" + fieldName + "." + infoLnk.getVName()
										+ "}}\" th:text=\"${" + fieldName + "." + info.getVName() + "}\"></a></td>");
						processed.add(key);
						processed.add(key + "link");
					} else if (key.endsWith("link")
							&& colNameToInfoMap.containsKey(key.substring(0, key.length() - 4))) {
						key = key.substring(0, key.length() - 4);
						ColInfo infoLnk = (ColInfo) info;
						info = (ColInfo) colNameToInfoMap.get(key);
						ps.println(
								"	            <" + tds + "><a th:href=\"@{${" + fieldName + "." + infoLnk.getVName()
										+ "}}\" th:text=\"${" + fieldName + "." + info.getVName() + "}\"></a></td>");
						processed.add(key);
						processed.add(key + "link");
					} else {
						ps.println("	            <" + tds + " th:text=\"${" + fieldName + "." + info.getVName()
								+ "}\"></td>");
						processed.add(key);
					}
					ps.println(unless);
				}
			}
		}
	}

	/**
	 * Write the headers for the list table
	 * 
	 * @param ps
	 * @param processed
	 * @param tableName
	 * @param colsInfo
	 * @param topField  TODO
	 * @throws Exception
	 */
	private void writeListHeaders(PrintStream ps, Set<String> processed, String tableName,
			Map<String, Map<String, ColInfo>> colsInfo, String topField) throws Exception {
		String className = Utils.tabToStr(renames, tableName);
		if (topField == null)
			topField = className.substring(0, 1).toLowerCase() + className.substring(1);

		Map<String, ColInfo> colNameToInfoMap = colsInfo.get(className);
		// write table header
		for (String key : colNameToInfoMap.keySet()) {
			if (PKEY_INFO.equals(key))
				continue;
			ColInfo info = (ColInfo) colNameToInfoMap.get(key);
			if (info.isPk())
				continue;
			if (!processed.contains(key) && info.isList()) {
				if (info.getForeignTable() != null) {
					writeListHeaders(ps, processed, info.getForeignTable(), colsInfo, topField);
				} else {
					if (key.endsWith("link")) {
						key = key.substring(0, key.length() - 4);
					}

					String th = makeAdminOnly(ps, ACCOUNT_CLASS.equals(className) || info.isAdminOnly(), "th");
					ps.println("            <" + th + ">");
					ps.println(
							"				<a th:if=\"${" + "SearchForm.sortField == '" + info.getVName() + "'}\"");
					ps.println("					th:href=\"@{'/" + topField + "s/search/' + ${"
							+ "SearchForm.page} + '?sortField=" + info.getVName()
							+ "&sortDir=' + ${SearchForm.reverseSortDir}}\"");
					ps.println(
							"					th:text=\"#{" + info.getMsgKey() + "}\">" + info.getColName() + "</a>");
					ps.println("					<a th:if=\"${" + "SearchForm.sortField != '" + info.getVName()
							+ "'}\"");
					ps.println("					th:href=\"@{'/" + topField + "s/search/' + ${"
							+ "SearchForm.page} + '?sortField=" + info.getVName() + "&sortDir=asc'}\"");
					ps.println(
							"					th:text=\"#{" + info.getMsgKey() + "}\">" + info.getColName() + "</a>");
					ps.println("            </th>");
					processed.add(key);
					processed.add(key + "link");
				}
			}
		}
	}

	/**
	 * Write the list page
	 * 
	 * @param tableName
	 * @param colsInfo
	 * @throws Exception
	 */
	private void writeListPage(String tableName, Map<String, Map<String, ColInfo>> colsInfo) throws Exception {
		String className = Utils.tabToStr(renames, tableName);
		Map<String, ColInfo> colNameToInfoMap = colsInfo.get(className);

		ColInfo pkinfo = colNameToInfoMap.get(PKEY_INFO);
		String fieldName = className.substring(0, 1).toLowerCase() + className.substring(1);
		Path p = Utils.createFile(baseDir, "src/main/resources/templates/" + fieldName + "s.html");
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
				Set<String> processed = new HashSet<String>();
				ps.println(htmlHeader(className, "listView"));
				ps.println(
						"	<div class=\"container\" th:with=\"SearchForm=${session." + fieldName + "SearchForm}\">");
				ps.println("		<h1 th:text=\"#{class." + className + "} + ' ' + #{edit.list}\"></h1>");
				ps.println("		<a th:href=\"@{/" + fieldName
						+ "s/search}\" th:text=\"#{search.search} + ' ' + #{class." + className + "}\"></a> <br />");
				ps.println("		<a th:href=\"@{/" + fieldName + "s/new}\" th:text=\"#{edit.new} + ' ' + #{class."
						+ className + "}\"></a> <br /><br />");
				ps.println("");
				ps.println("	    <table>");
				ps.println("	        <tr>");
				writeListHeaders(ps, processed, tableName, colsInfo, null);
				ps.println("            <th th:text=\"#{edit.actions}\"></th>");
				ps.println("	        </tr>");
				// write data loop
				ps.println("	        <tr th:each=\"" + fieldName + ":${" + fieldName + "s}\">");
				processed = new HashSet<String>();
				writeListLoop(ps, processed, "", tableName, colsInfo);
				ps.println("				<td><a th:href=\"@{'/" + fieldName + "s/edit/' + ${" + fieldName + "."
						+ pkinfo.getVName() + "}}\" th:text=\"#{edit.edit}\"></a>");
				ps.println("					&nbsp;&nbsp;&nbsp; <a th:href=\"@{'/" + fieldName + "s/delete/' + ${"
						+ fieldName + "." + pkinfo.getVName() + "}}\" th:text=\"#{edit.delete}\"></a>");
				ps.println("				</td>");
				ps.println("");
				ps.println("	        </tr>");
				ps.println("	    </table>");
				ps.println("		<div th:if=\"${" + "SearchForm.page > 1}\">");
				ps.println("			<div class=\"row col-sm-10\">");
				ps.println("				<div class=\"col-sm-2\">Total Rows:");
				ps.println("					[[${" + "SearchForm.totalItems}]]</div>");
				ps.println("				<div class=\"col-sm-1\">");
				ps.println("					<span");
				ps.println(
						"						th:each=\"i: ${#numbers.sequence(1, " + "SearchForm.totalPages)}\">");
				ps.println("						<a th:if=\"${" + "SearchForm.page != i}\"");
				ps.println(
						"						th:href=\"@{'/" + fieldName + "s/search/' + ${i}+ '?sortField=' + ${"
								+ "SearchForm.sortField} + '&sortDir=' + ${" + "SearchForm.sortDir}}\">[[${i}]]</a>");
				ps.println("						<span th:unless=\"${" + "SearchForm.page != i}\">[[${i}]]</span>");
				ps.println("						&nbsp; &nbsp;");
				ps.println("					</span>");
				ps.println("				</div>");
				ps.println("				<div class=\"col-sm-1\">");
				ps.println("					<a");
				ps.println("						th:if=\"${" + "SearchForm.page < SearchForm.totalPages}\"");
				ps.println("						th:href=\"@{'/" + fieldName + "s/search/' + ${"
						+ "SearchForm.page + 1}+ '?sortField=' + ${" + "SearchForm.sortField} + '&sortDir=' + ${"
						+ "SearchForm.sortDir}}\">Next</a>");
				ps.println("					<span");
				ps.println("						th:unless=\"${"
						+ "SearchForm.page < SearchForm.totalPages}\">Next</span>");
				ps.println("				</div>");
				ps.println("");
				ps.println("				<div class=\"col-sm-1\">");
				ps.println("					<a");
				ps.println("						th:if=\"${" + "SearchForm.page < SearchForm.totalPages}\"");
				ps.println("						th:href=\"@{'/" + fieldName + "s/search/' + ${"
						+ "SearchForm.totalPages}+ '?sortField=' + ${" + "SearchForm.sortField} + '&sortDir=' + ${"
						+ "SearchForm.sortDir}}\">Last</a>");
				ps.println("					<span");
				ps.println("						th:unless=\"${"
						+ "SearchForm.page < SearchForm.totalPages}\">Last</span>");
				ps.println("				</div>");
				ps.println("			</div>");
				ps.println("		</div>");
				ps.println("");
				ps.println("    </div>");
				ps.println("");
				ps.println(htmlFooter());
				log.warn("Wrote:" + p.toString());
			} catch (Exception e) {
				log.error("failed to create " + p, e);
				p.toFile().delete();
			}
		}
	}

	/**
	 * Write the base class used by tests the employ mock object. TODO: replace with
	 * velocity template
	 * 
	 * @param j2m TODO
	 * @param set
	 */
	private void addMockBaseVars(Map<String, Map<String, ColInfo>> colsInfo, Java2VM j2m) {
		Set<String> set = colsInfo.keySet();
		StringBuilder sb = new StringBuilder();
		if (!set.contains(ACCOUNT_CLASS)) {
			sb.append("import " + basePkg + ".service." + ACCOUNT_CLASS + "Services;").append(System.lineSeparator());
			sb.append("import " + basePkg + ".repo." + ACCOUNT_CLASS + "Repository;").append(System.lineSeparator());
		}
		for (String className : set) {
			sb.append("import " + basePkg + ".repo." + className + "Repository;").append(System.lineSeparator());
			sb.append("import " + basePkg + ".service." + className + "Services;").append(System.lineSeparator());
		}
		j2m.addContext("mockImports", sb.toString());

		sb = new StringBuilder();
		if (!set.contains(ACCOUNT_CLASS)) {
			sb.append("    @MockBean").append(System.lineSeparator());
			sb.append("    protected " + ACCOUNT_CLASS + "Services accountServices;").append(System.lineSeparator());
			sb.append("    @MockBean").append(System.lineSeparator());
			sb.append("    protected " + ACCOUNT_CLASS + "Repository accountRepository;")
					.append(System.lineSeparator());
		}
		for (String className : set) {
			String fieldName = className.substring(0, 1).toLowerCase() + className.substring(1);
			sb.append("    @MockBean").append(System.lineSeparator());
			sb.append("    protected " + className + "Services " + fieldName + "Services;")
					.append(System.lineSeparator());
			sb.append("    @MockBean").append(System.lineSeparator());
			sb.append("    protected " + className + "Repository " + fieldName + "Repository;")
					.append(System.lineSeparator());
		}
		j2m.addContext("mockBeans", sb.toString());

		sb = new StringBuilder();
		for (String className : set) {
			if (classHasUserFields(className)) {
				sb.append("		if (\"" + ADMIN_USER + "\".equals(user)) ").append(System.lineSeparator());
				sb.append("			contentContainsKey(result, \"class." + className + "\", false);")
						.append(System.lineSeparator());
			} else {
				sb.append("		contentContainsKey(result, \"class." + className + "\", false);")
						.append(System.lineSeparator());
			}
		}
		sb.append("// REST menu").append(System.lineSeparator());
		sb.append("		contentContainsKey(result, \"header.restApi\");").append(System.lineSeparator());
		for (String className : set) {
			String fieldName = className.substring(0, 1).toLowerCase() + className.substring(1);
			if (classHasUserFields(className)) {
				sb.append("		if (\"" + ADMIN_USER + "\".equals(user)) ").append(System.lineSeparator());
				sb.append("			contentContainsMarkup(result, \"/api/" + fieldName + "s\", false);")
						.append(System.lineSeparator());
			} else {
				sb.append("		contentContainsMarkup(result, \"/api/" + fieldName + "s\", false);")
						.append(System.lineSeparator());
			}
		}
		j2m.addContext("mockNav", sb.toString());

	}

	/**
	 * Write tests for controller that a works on tableName
	 * 
	 * @param tableName
	 * @param colsInfo
	 * @throws Exception
	 */
	private void writeObjControllerTest(String tableName, Map<String, Map<String, ColInfo>> colsInfo) throws Exception {
		String className = Utils.tabToStr(renames, tableName);
		Map<String, ColInfo> colNameToInfoMap = colsInfo.get(className);

		ColInfo pkinfo = colNameToInfoMap.get(PKEY_INFO);
		String pkgNam = basePkg + ".controller";
		String relPath = pkgNam.replace('.', '/');
		String fieldName = className.substring(0, 1).toLowerCase() + className.substring(1);
		Path p = Utils.createFile(baseDir, "src/test/java", relPath, className + "ControllerTest.java");
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
				ps.println("package " + pkgNam + ';');
				ps.println("import static org.mockito.BDDMockito.given;");
				ps.println("import java.util.ArrayList;");
				ps.println("import java.util.List;");
				ps.println("import org.junit.Test;");
				ps.println("import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;");
				ps.println("import org.springframework.data.domain.Page;");
				ps.println("import org.springframework.test.web.servlet.ResultActions;");
				ps.println("import com.google.common.collect.ImmutableMap;");
				ps.println("import lombok.extern.slf4j.Slf4j;");
				ps.println("");
				ps.println("import " + basePkg + ".MockBase;");
				ps.println("import " + basePkg + ".entity." + className + ";");
				ps.println("import " + basePkg + ".form." + className + "Form;");
				ps.println("import " + basePkg + ".search." + className + "SearchForm;");
				ps.println("");
				ps.println(getClassHeader(className + "ControllerTest", className + "Controller.", null));
				ps.println("@Slf4j");
				ps.println("@WebMvcTest(" + className + "Controller.class)");
				ps.println("public class " + className + "ControllerTest extends MockBase {");
				ps.println("	private " + className + " get" + className + "(" + pkinfo.getType() + " "
						+ pkinfo.getVName() + ") {");
				ps.println("		" + className + " o = new " + className + "();");
				ps.println("		o.set" + pkinfo.getGsName() + "(" + pkinfo.getVName() + ");");
				for (String key : colNameToInfoMap.keySet()) {
					if (PKEY_INFO.equals(key))
						continue;
					ColInfo info = (ColInfo) colNameToInfoMap.get(key);
					if (info.isList()) {
						if (info.isString()) {
							int endIndex = info.getLength();
							if (endIndex > 0) {
								if (info.isPassword()) {
									ps.println("//        o.set" + info.getGsName() + "(getTestPasswordString("
											+ endIndex + "));");
								} else if (info.isEmail()) {
									ps.println("        o.set" + info.getGsName() + "(getTestEmailString(" + endIndex
											+ "));");
								} else {
									ps.println(
											"        o.set" + info.getGsName() + "(getTestString(" + endIndex + "));");
								}
							}
						}
					} else {
						ps.println("		// TODO: confirm ignoring " + className + "." + key);
					}
				}
				ps.println("		return o;");
				ps.println("	}");
				ps.println("");
				ps.println("	/**");
				ps.println("	 * Test method for");
				ps.println("	 * {@link " + basePkg + ".controller." + className + "Controller#getAll" + className
						+ "s(org.springframework.ui.Model)}.");
				ps.println("	 */");
				ps.println("	@Test");
				ps.println("	public void testGetAll" + className + "s() throws Exception {");
				ps.println("		List<" + className + "> list = new ArrayList<>();");
				ps.println("		" + className + " o = get" + className + "(1" + pkinfo.getMod() + ");");
				ps.println("		list.add(o);");
				ps.println("");
				ps.println("		Page<" + className + "> p = getPage(list);");
				ps.println("		given(" + fieldName + "Services.listAll(new " + className
						+ "SearchForm())).willReturn(p);");
				ps.println("");
				ps.println("		ResultActions ra = getAsAdmin(\"/" + fieldName + "s\");");
				ps.println("		contentContainsMarkup(ra,\"<h1>\" + getMsg(\"class." + className
						+ "\") + \" \" + getMsg(\"edit.list\") + \"</h1>\");");
				for (String key : colNameToInfoMap.keySet()) {
					if (PKEY_INFO.equals(key))
						continue;
					ColInfo info = (ColInfo) colNameToInfoMap.get(key);
					if (info.isPk())
						continue;
					if (info.isList()) {
						if (info.isString()) {
							int endIndex = info.getLength();
							if (endIndex > 0) {
								if (info.isPassword()) {
									ps.println("//        contentContainsMarkup(ra,getTestPasswordString(" + endIndex
											+ "));");
								} else if (info.isEmail()) {
									ps.println(
											"        contentContainsMarkup(ra,getTestEmailString(" + endIndex + "));");
								} else {
									ps.println("		contentContainsMarkup(ra,getTestString(" + endIndex + "));");
								}
							}
						}
						if (!info.getVName().endsWith("link"))
							ps.println("		contentContainsMarkup(ra,getMsg(\"" + info.getMsgKey() + "\"));");
					} else {
						ps.println("		// TODO: confirm ignoring " + className + "." + key);
					}

				}
				ps.println("	}");
				ps.println("");
				ps.println("	/**");
				ps.println("	 * Test method for");
				ps.println("	 * {@link " + basePkg + ".controller." + className + "Controller#showNew" + className
						+ "Page(org.springframework.ui.Model)}.");
				ps.println("	 * ");
				ps.println("	 * @throws Exception");
				ps.println("	 */");
				ps.println("	@Test");
				ps.println("	public void testShowNew" + className + "Page() throws Exception {");
				ps.println("		ResultActions ra = getAsAdmin(\"/" + fieldName + "s/new\");");
				ps.println(
						"		contentContainsMarkup(ra,\"<legend>\" + getMsg(\"edit.new\") + \" \" + getMsg(\"class."
								+ className + "\") + \"</legend>\");");
				for (String key : colNameToInfoMap.keySet()) {
					if (PKEY_INFO.equals(key))
						continue;
					ColInfo info = (ColInfo) colNameToInfoMap.get(key);
					if (info.isHidden() || info.isLastMod() || info.isPk()) {
						ps.println("		// TODO: confirm ignoring " + className + "." + key);
						continue;
					}
					ps.println("		contentContainsMarkup(ra,\"" + info.getGsName() + "\");");

				}
				ps.println("	}");
				ps.println("");
				ps.println("	/**");
				ps.println("	 * Test method for");
				ps.println("	 * {@link " + basePkg + ".controller." + className + "Controller#save" + className + "("
						+ basePkg + ".entity." + className + ", java.lang.String)}.");
				ps.println("	 */");
				ps.println("	@Test");
				ps.println("	public void testSave" + className + "Cancel() throws Exception {");
				ps.println("		" + className + " o = get" + className + "(1" + pkinfo.getMod() + ");");
				ps.println("");
				ps.println("		send(SEND_POST, \"/" + fieldName + "s/save\", \"" + fieldName
						+ "\", o, ImmutableMap.of(\"action\", \"cancel\"), ADMIN_USER,");
				ps.println("				\"/" + fieldName + "s\");");
				ps.println("	}");
				ps.println("");
				ps.println("	/**");
				ps.println("	 * Test method for");
				ps.println("	 * {@link " + basePkg + ".controller." + className + "Controller#save" + className + "("
						+ basePkg + ".entity." + className + ", java.lang.String)}.");
				ps.println("	 */");
				ps.println("	@Test");
				ps.println("	public void testSave" + className + "Save() throws Exception {");
				ps.println("		" + className + " o = get" + className + "(0" + pkinfo.getMod() + ");");
				ps.println("		" + className + "Form form = " + className + "Form.getInstance(o);");
				for (String key : colNameToInfoMap.keySet()) {
					if (PKEY_INFO.equals(key))
						continue;
					ColInfo info = (ColInfo) colNameToInfoMap.get(key);
					if (info.isPassword()) {
						ps.println("		form.set" + info.getGsName() + "(getTestPasswordString(" + info.getLength()
								+ "));");
						ps.println(
								"		form.set" + info.getGsName() + "Confirm(form.get" + info.getGsName() + "());");
					}
				}
				ps.println("		log.debug(form.toString());");
				ps.println("");
				ps.println("		send(SEND_POST, \"/" + fieldName + "s/save\", \"" + fieldName
						+ "Form\", form, ImmutableMap.of(\"action\", \"save\"), ADMIN_USER,");
				ps.println("				\"/" + fieldName + "s\");");
				ps.println("	}");
				ps.println("");
				ps.println("	/**");
				ps.println("	 * Test method for");
				ps.println("	 * {@link " + basePkg + ".controller." + className + "Controller#showEdit" + className
						+ "Page(java.lang.Integer)}.");
				ps.println("	 * ");
				ps.println("	 * @throws Exception");
				ps.println("	 */");
				ps.println("	@Test");
				ps.println("	public void testShowEdit" + className + "Page() throws Exception {");
				ps.println("		" + className + " o = get" + className + "(1" + pkinfo.getMod() + ");");
				ps.println("");
				ps.println("		given(" + fieldName + "Services.get(1" + pkinfo.getMod() + ")).willReturn(o);");
				ps.println("");
				ps.println("		ResultActions ra = getAsAdmin(\"/" + fieldName + "s/edit/1\");");
				for (String key : colNameToInfoMap.keySet()) {
					if (PKEY_INFO.equals(key))
						continue;
					ColInfo info = (ColInfo) colNameToInfoMap.get(key);
					if (info.isHidden() || info.isLastMod() || info.isPk()) {
						ps.println("		// TODO: confirm ignoring " + className + "." + key);
						continue;
					}
					if (info.isString()) {
						int endIndex = info.getLength();
						if (endIndex > 0) {
							if (info.isPassword() || info.isHidden()) {
								ps.println("//		contentContainsMarkup(ra,o.get" + info.getGsName() + "());");
							} else {
								ps.println("		contentContainsMarkup(ra,o.get" + info.getGsName() + "());");
							}
						}
					}
					ps.println("		contentContainsMarkup(ra,\"" + info.getGsName() + "\");");
				}
				ps.println("	}");
				ps.println("");
				ps.println("	/**");
				ps.println("	 * Test method for");
				ps.println("	 * {@link " + basePkg + ".controller." + className + "Controller#delete" + className
						+ "(java.lang.Integer)}.");
				ps.println("	 */");
				ps.println("	@Test");
				ps.println("	public void testDelete" + className + "() throws Exception {");
				ps.println(
						"		getAsAdminRedirectExpected(\"/" + fieldName + "s/delete/1\",\"/" + fieldName + "s\");");
				ps.println("	}");
				ps.println("");
				ps.println("}");
				ps.println("");
				log.warn("Wrote:" + p.toString());
			} catch (Exception e) {
				log.error("failed to create " + p, e);
				p.toFile().delete();
			}
		}

	}

	/**
	 * Write controller that a works on tableName. TODO: replace with velocity
	 * template
	 * 
	 * @param tableName
	 * @param colsInfo
	 * @throws Exception
	 */
	private void writeObjController(String tableName, Map<String, Map<String, ColInfo>> colsInfo) throws Exception {
		String className = Utils.tabToStr(renames, tableName);
		Map<String, ColInfo> colNameToInfoMap = colsInfo.get(className);

		ColInfo pkinfo = colNameToInfoMap.get(PKEY_INFO);

		String pkgNam = basePkg + ".controller";
		String relPath = pkgNam.replace('.', '/');
		String fieldName = className.substring(0, 1).toLowerCase() + className.substring(1);
		Path p = Utils.createFile(baseDir, "src/main/java", relPath, className + "Controller.java");
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
				ps.println("package " + pkgNam + ';');
				ps.println("");
				ps.println("import java.util.Date;");
				ps.println("import java.util.List;");
				ps.println("");
				ps.println("import javax.servlet.http.HttpServletRequest;");
				ps.println("import javax.validation.Valid;");
				ps.println("import org.springframework.beans.factory.annotation.Autowired;");
				ps.println("import org.springframework.data.domain.Page;");
				ps.println("import org.springframework.stereotype.Controller;");
				ps.println("import org.springframework.ui.Model;");
				ps.println("import org.springframework.validation.Errors;");
				ps.println("import org.springframework.web.bind.annotation.GetMapping;");
				ps.println("import org.springframework.web.bind.annotation.ModelAttribute;");
				ps.println("import org.springframework.web.bind.annotation.PathVariable;");
				ps.println("import org.springframework.web.bind.annotation.PostMapping;");
				ps.println("import org.springframework.web.bind.annotation.RequestHeader;");
				ps.println("import org.springframework.web.bind.annotation.RequestMapping;");
				ps.println("import org.springframework.web.bind.annotation.RequestParam;");
				ps.println("import org.springframework.web.servlet.ModelAndView;");
				ps.println("import org.springframework.web.servlet.mvc.support.RedirectAttributes;");
				ps.println("");
				ps.println("import " + basePkg + ".entity." + className + ";");
				ps.println("import " + basePkg + ".form." + className + "Form;");
				ps.println("import " + basePkg + ".search." + className + "SearchForm;");
				ps.println("import " + basePkg + ".service." + className + "Services;");
				ps.println("import " + basePkg + ".utils.Message;");
				ps.println("import " + basePkg + ".utils.MessageHelper;");
				ps.println("import " + basePkg + ".utils.Utils;");
				ps.println("import lombok.extern.slf4j.Slf4j;");
				ps.println("");
				ps.println(getClassHeader(className + "Controller", className + "Controller.", null));
				ps.println("@Slf4j");
				ps.println("@Controller");
				ps.println("@RequestMapping(\"/" + fieldName + "s\")");
				ps.println("public class " + className + "Controller {");
				ps.println("");
				ps.println("	@Autowired");
				ps.println("	private " + className + "Services " + fieldName + "Service;");
				ps.println("");
				ps.println("	private " + className + "SearchForm getForm(HttpServletRequest request) {");
				ps.println("		" + className + "SearchForm form = (" + className
						+ "SearchForm) request.getSession().getAttribute(\"" + fieldName + "SearchForm\");");
				ps.println("		if (log.isDebugEnabled())");
				ps.println("			log.debug(\"pulled from session:\" + form);");
				ps.println("		if (form == null) {");
				ps.println("			form = new " + className + "SearchForm();");
				ps.println("		}");
				ps.println("		return form;");
				ps.println("	}");
				ps.println("");
				ps.println("	private void setForm(HttpServletRequest request, " + className + "SearchForm form) {");
				ps.println("		request.getSession().setAttribute(\"" + fieldName + "SearchForm\", form);");
				ps.println("		if (log.isDebugEnabled())");
				ps.println("			log.debug(\"stored:\" + form);");
				ps.println("	}");
				ps.println("");
				ps.println("");
				ps.println("	@GetMapping");
				ps.println("	public ModelAndView getAll(HttpServletRequest request) {");
				ps.println("		return findPaginated(request, 1, \"id\", \"asc\");");
				ps.println("	}");
				ps.println("");
				ps.println("	@GetMapping(\"/new\")");
				ps.println("	public ModelAndView showNewPage() {");
				ps.println("		return showEditPage(0" + pkinfo.getMod() + ");");
				ps.println("	}");
				ps.println("");
				ps.println("	@PostMapping(value = \"/search\")");
				ps.println("	public ModelAndView search(HttpServletRequest request, @ModelAttribute " + className
						+ "SearchForm form, RedirectAttributes ra,");
				ps.println("			@RequestParam(value = \"action\", required = true) String action) {");
				ps.println("		setForm(request, form);");
				ps.println("		ModelAndView mav = findPaginated(request, 1, \"id\", \"asc\");");
				ps.println("		@SuppressWarnings(\"unchecked\")");
				ps.println("		List<" + className + "> list = (List<" + className
						+ ">) mav.getModelMap().getAttribute(\"" + fieldName + "s\");");
				ps.println("		if (list == null || list.isEmpty()) {");
				ps.println("			mav.setViewName(\"search_" + fieldName + "\");");
				ps.println("			mav.getModelMap().addAttribute(Message.MESSAGE_ATTRIBUTE,");
				ps.println("					new Message(\"search.noResult\", Message.Type.WARNING));");
				ps.println("		}");
				ps.println("");
				ps.println("		return mav;");
				ps.println("	}");
				ps.println("");
				ps.println("	@GetMapping(\"/search/{pageNo}\")");
				ps.println(
						"	public ModelAndView findPaginated(HttpServletRequest request, @PathVariable(value = \"pageNo\") int pageNo,");
				ps.println(
						"			@RequestParam(\"sortField\") String sortField, @RequestParam(\"sortDir\") String sortDir) {");
				ps.println("		" + className + "SearchForm form = getForm(request);");
				ps.println("		if (pageNo < 1)");
				ps.println("			pageNo = 1;");
				ps.println("");
				ps.println("		form.setPage(pageNo);");
				ps.println("		form.setSortField(sortField);");
				ps.println("		form.setSortAsc(\"asc\".equalsIgnoreCase(sortDir));");
				ps.println("");
				ps.println("		if (log.isDebugEnabled())");
				ps.println("			log.debug(\"Searching with:\" + form);");
				ps.println("");
				ps.println("		Page<" + className + "> page = " + fieldName + "Service.listAll(form);");
				ps.println("");
				ps.println("		form.setTotalPages(page.getTotalPages());");
				ps.println("		form.setTotalItems(page.getTotalElements());");
				ps.println("		setForm(request, form);");
				ps.println("");
				ps.println("		ModelAndView mav = new ModelAndView(\"" + fieldName + "s\");");
				ps.println("		mav.addObject(\"" + fieldName + "s\", page.getContent());");
				ps.println("		return mav;");
				ps.println("	}");
				ps.println("");
				ps.println("	@GetMapping(\"/search\")");
				ps.println("	public String showSearchPage(HttpServletRequest request, Model model,");
				ps.println(
						"			@RequestHeader(value = \"X-Requested-With\", required = false) String requestedWith) {");
				ps.println("		model.addAttribute(getForm(request));");
				ps.println("		if (Utils.isAjaxRequest(requestedWith)) {");
				ps.println(
						"			return \"search_" + fieldName + "\".concat(\" :: " + fieldName + "SearchForm\");");
				ps.println("		}");
				ps.println("");
				ps.println("		return \"search_" + fieldName + "\";");
				ps.println("	}");
				ps.println("");
				ps.println("	@PostMapping(value = \"/save\")");
				ps.println("	public String save(@Valid @ModelAttribute " + className
						+ "Form form, Errors errors, RedirectAttributes ra,");
				ps.println("			@RequestParam(value = \"action\", required = true) String action) {");
				ps.println("		if (action.equals(\"save\")) {");
				ps.println("			if (errors.hasErrors()) {");
				ps.println("				return \"edit_" + fieldName + "\";");
				ps.println("			}");
				ps.println("");
				ps.println("			" + className + " " + fieldName + " = new " + className + "();");
				for (String key : colNameToInfoMap.keySet()) {
					if (PKEY_INFO.equals(key))
						continue;
					ColInfo info = (ColInfo) colNameToInfoMap.get(key);
					if (!info.isLastMod()) {
						ps.println("			" + fieldName + ".set" + info.getGsName() + "(form.get"
								+ info.getGsName() + "());");
					} else {
						ps.println("			" + fieldName + ".set" + info.getGsName()
								+ "(new Date(System.currentTimeMillis()));");
					}
				}

				ps.println("			try {");
				ps.println("				" + fieldName + " = " + fieldName + "Service.save(" + fieldName + ");");
				ps.println("			} catch (Exception e) {");
				ps.println("				log.error(\"Failed saving:\" + form, e);");
				ps.println("			}");
				ps.println("");
				ps.println("			if (" + fieldName + " == null) {");
				ps.println("				MessageHelper.addErrorAttribute(ra, \"db.failed\");");
				ps.println("			} else {");
				ps.println("				MessageHelper.addSuccessAttribute(ra, \"save.success\");");
				ps.println("			}");
				ps.println("		} else {");
				ps.println("			MessageHelper.addSuccessAttribute(ra, \"save.cancelled\");");
				ps.println("		}");
				ps.println("");
				ps.println("		return \"redirect:/" + fieldName + "s\";");
				ps.println("	}");
				ps.println("");
				ps.println("	@GetMapping(\"/edit/{id}\")");
				ps.println("	public ModelAndView showEditPage(@PathVariable(name = \"id\") " + pkinfo.getType()
						+ " id) {");
				ps.println("		ModelAndView mav = new ModelAndView(\"edit_" + fieldName + "\");");
				ps.println("		" + className + " " + fieldName + " = null;");
				ps.println("		if (id > 0)");
				ps.println("			" + fieldName + " = " + fieldName + "Service.get(id);");
				ps.println("		mav.addObject(\"" + fieldName + "Form\", " + className + "Form.getInstance("
						+ fieldName + "));");
				ps.println("");
				ps.println("		return mav;");
				ps.println("	}");
				ps.println("");
				ps.println("	@GetMapping(\"/delete/{id}\")");
				ps.println("	public String delete(@PathVariable(name = \"id\") " + pkinfo.getType() + " id) {");
				ps.println("		" + fieldName + "Service.delete(id);");
				ps.println("		return \"redirect:/" + fieldName + "s\";");
				ps.println("	}");
				ps.println("}");
				ps.println("");
				log.warn("Wrote:" + p.toString());
			} catch (Exception e) {
				log.error("failed to create " + p, e);
				p.toFile().delete();
			}
		}
	}

	/**
	 * Add any imports needed for fields
	 * 
	 * @param ps               PrintStream to write to. If null ignores
	 * @param colNameToInfoMap
	 * @param clsType          IMPORT_TYPE_SERVICE= general IMPORT_TYPE_FORM=form
	 *                         annotations IMPORT_TYPE_BEAN=bean annotations
	 * @return if ps = null returns String otherwise null
	 */
	private String addImports(PrintStream ps, Map<String, ColInfo> colNameToInfoMap, int clsType) {
		Set<String> imports = new TreeSet<String>();
		for (String key : colNameToInfoMap.keySet()) {
			if (PKEY_INFO.equals(key))
				continue;
			ColInfo info = (ColInfo) colNameToInfoMap.get(key);
			if ("BigDecimal".equals(info.getType()))
				imports.add("import java.math.BigDecimal;");

			if ("Date".equals(info.getType())) {
				imports.add("import java.util.Date;");
				imports.add("import org.springframework.format.annotation.DateTimeFormat;");
			}
			if (clsType == IMPORT_TYPE_FORM) {
				if (info.isPassword()) {
					imports.add("import " + basePkg + ".controller.FieldMatch;");
					imports.add("import " + basePkg + ".controller.ValidatePassword;");
				}
				if (info.getLength() > 0)
					imports.add("import org.hibernate.validator.constraints.Length;");
				if (info.isRequired())
					imports.add("import javax.validation.constraints.NotBlank;");

				if (info.isUnique() && info.isEmail()) {
					imports.add("import " + basePkg + ".controller.UniqueEmail;");
				}
				if (info.isJsonIgnore()) {
					imports.add("import com.fasterxml.jackson.annotation.JsonIgnore;");
				}
				if (info.isEmail()) {
					imports.add("import javax.validation.constraints.Email;");
				}
				if (!StringUtils.isBlank(info.getForeignTable())) {
					imports.add("import " + basePkg + ".entity." + info.getType() + ";");
				}
			}
			if (clsType == IMPORT_TYPE_BEAN) {
				if (info.isJsonIgnore()) {
					imports.add("import com.fasterxml.jackson.annotation.JsonIgnore;");
				}
				if (!StringUtils.isBlank(info.getForeignTable())) {
					imports.add("import javax.persistence.JoinColumn;");
					imports.add("import javax.persistence.ManyToOne;");
				}
				if (info.isCreated() || info.isLastMod()) {
					imports.add("import lombok.EqualsAndHashCode;");
				}

			}

		}
		StringBuilder sb = null;
		if (ps == null)
			sb = new StringBuilder();
		for (String s : imports) {
			if (ps == null)
				sb.append(s).append(System.lineSeparator());
			else
				ps.println(s);
		}
		if (ps == null) {
			sb.append(System.lineSeparator());
			return sb.toString();
		}

		ps.println("");
		return null;
	}

	/**
	 * Write service for tableName
	 * 
	 * @param className
	 * @param colNameToInfoMap
	 */
	private void writeService(String tableName, Map<String, Map<String, ColInfo>> colsInfo) throws Exception {
		String className = Utils.tabToStr(renames, tableName);
		Map<String, ColInfo> colNameToInfoMap = colsInfo.get(className);

		ColInfo pkinfo = colNameToInfoMap.get(PKEY_INFO);
		String pkgNam = basePkg + ".service";
		String relPath = pkgNam.replace('.', '/');
		String fieldName = className.substring(0, 1).toLowerCase() + className.substring(1);
		Path p = Utils.createFile(baseDir, "src/main/java", relPath, className + "Services.java");
		if (p != null) {
			boolean hasPasswordField = false;
			for (ColInfo c : colNameToInfoMap.values()) {
				if (c.isPassword())
					hasPasswordField = true;
			}
			try (PrintStream ps = new PrintStream(p.toFile())) {
				ps.println("package " + pkgNam + ';');
				ps.println("");
				ps.println("import java.math.BigDecimal;");
				ps.println("import java.util.List;");
				ps.println("");
				ps.println("import org.apache.commons.lang3.StringUtils;");
				ps.println("import org.springframework.beans.factory.annotation.Autowired;");
				ps.println("import org.springframework.data.domain.Page;");
				ps.println("import org.springframework.data.domain.PageRequest;");
				ps.println("import org.springframework.data.domain.Pageable;");
				ps.println("import org.springframework.data.domain.Sort;");
				ps.println("import org.springframework.stereotype.Service;");
//				ps.println("import org.springframework.transaction.annotation.Transactional;");
				ps.println("");
				addImports(ps, colNameToInfoMap, IMPORT_TYPE_SERVICE);
				ps.println("import " + basePkg + ".entity." + className + ";");
				ps.println("import " + basePkg + ".repo." + className + "Repository;");
				ps.println("import " + basePkg + ".search.SearchCriteria;");
				ps.println("import " + basePkg + ".search.SearchOperation;");
				ps.println("import " + basePkg + ".search.SearchSpecification;");
				ps.println("import " + basePkg + ".search." + className + "SearchForm;");
				ps.println("import " + basePkg + ".utils.Utils;");
				ps.println("");
				ps.println("import lombok.extern.slf4j.Slf4j;");
				if (ACCOUNT_CLASS.equals(className)) {
					ps.println("import java.util.List;");
					ps.println("import java.util.Optional;");
					ps.println("import java.util.ResourceBundle;");
					ps.println("");
					ps.println("import javax.annotation.PostConstruct;");
					ps.println("import org.apache.commons.lang3.StringUtils;");
				}
				ps.println("");
				ps.println(getClassHeader(className + "Services", className + "Services.", null));
				ps.println("@Slf4j");
				ps.println("@Service");
				if (hasPasswordField) {
					ps.println("public class " + className + "Services extends UserServices<" + className + "> {");
				} else {
					ps.println("public class " + className + "Services {");
				}
				ps.println("    @Autowired");
				ps.println("    private " + className + "Repository " + fieldName + "Repository;");
				ps.println("");
				if (ACCOUNT_CLASS.equals(className)) {
					ps.println("	public static final String ROLE_PREFIX = \"ROLE_\";");
					ps.println("");
					ps.println("	/**");
					ps.println("	 * reset default users. Comment out once done testing");
					ps.println("	 */");
					ps.println("	@PostConstruct");
					ps.println("	protected void initialize() {");
					ps.println("		ResourceBundle bundle = ResourceBundle.getBundle(\"app\");");
					ps.println("		boolean doinit = Utils.getProp(bundle, \"init.default.users\", true);");
					ps.println("		if (doinit) {");
					ps.println("			log.warn(\"Resetting default users\");");
					ps.println("			String user = Utils.getProp(bundle, \"default.user\", null);");
					ps.println("			if (!StringUtils.isBlank(user)) {");
					ps.println("				" + pkinfo.getType()
							+ " id = Utils.getProp(bundle, \"default.userid\", 1" + pkinfo.getMod() + ");");
					ps.println("				String userpass = Utils.getProp(bundle, \"default.userpass\", null);");
					ps.println(
							"				String userrole = ROLE_PREFIX + Utils.getProp(bundle, \"default.userrole\", null);");
					ps.println("				" + ACCOUNT_CLASS + " a = new " + ACCOUNT_CLASS + "();");
					ps.println("				a.setEmail(user);");
					ps.println("				a.setPassword(userpass);");
					ps.println("				a.setRole(userrole);");
					ps.println("				a.setId(id);");
					ps.println("				save(a);");
					ps.println("			}");
					ps.println("");
					ps.println("			user = Utils.getProp(bundle, \"default.admin\", null);");
					ps.println("			if (!StringUtils.isBlank(user)) {");
					ps.println("				" + pkinfo.getType()
							+ " id = Utils.getProp(bundle, \"default.adminid\", 2" + pkinfo.getMod() + ");");
					ps.println("				String userpass = Utils.getProp(bundle, \"default.adminpass\", null);");
					ps.println(
							"				String userrole = ROLE_PREFIX + Utils.getProp(bundle, \"default.adminrole\", null);");
					ps.println("				" + ACCOUNT_CLASS + " a = new " + ACCOUNT_CLASS + "();");
					ps.println("				a.setEmail(user);");
					ps.println("				a.setPassword(userpass);");
					ps.println("				a.setRole(userrole);");
					ps.println("				a.setId(id);");
					ps.println("				save(a);");
					ps.println("			}");
					ps.println("		}");
					ps.println("	}");
					ps.println("");
				}
				ps.println("	public Page<" + className + "> listAll(" + className + "SearchForm form) {");
				ps.println("		SearchSpecification<" + className + "> searchSpec = new SearchSpecification<"
						+ className + ">();");
				ps.println("		if (form != null) {");
				ps.println("			log.debug(form.toString());");
				for (ColInfo c : colNameToInfoMap.values()) {
					if (c.isDate()) {
						ps.println("");
						ps.println("			if (form.get" + c.getGsName() + "Min() != null) {");
						ps.println("				searchSpec.add(new SearchCriteria(\"" + c.getVName()
								+ "\", form.get" + c.getGsName() + "Min(), SearchOperation.GREATER_THAN_EQUAL));");
						ps.println("			}");
						ps.println("			if (form.get" + c.getGsName() + "Max() != null) {");
						ps.println("				searchSpec.add(new SearchCriteria(\"" + c.getVName()
								+ "\", form.get" + c.getGsName() + "Max(), SearchOperation.LESS_THAN_EQUAL));");
						ps.println("			}");
					} else if ("BigDecimal".equals(c.getType())) {
						ps.println("			if (form.get" + c.getGsName() + "Min() != null) {");
						ps.println("				BigDecimal bd = form.get" + c.getGsName() + "Min();");
						ps.println("				searchSpec.add(new SearchCriteria(\"" + c.getVName() + "\",");
						ps.println("						form.get" + c.getGsName()
								+ "Min().setScale(bd.scale() - 1, BigDecimal.ROUND_DOWN),");
						ps.println("						SearchOperation.GREATER_THAN_EQUAL));");
						ps.println("			}");
						ps.println("			if (form.get" + c.getGsName() + "Max() != null) {");
						ps.println("				BigDecimal bd = form.get" + c.getGsName() + "Min();");
						ps.println("				searchSpec.add(new SearchCriteria(\"" + c.getVName() + "\",");
						ps.println("						form.get" + c.getGsName()
								+ "Min().setScale(bd.scale() - 1, BigDecimal.ROUND_UP),");
						ps.println("						SearchOperation.LESS_THAN_EQUAL));");
						ps.println("			}");
					} else if ("String".equals(c.getType())) {
						ps.println("			if (!StringUtils.isBlank(form.get" + c.getGsName() + "())) {");
						ps.println("				searchSpec.add(new SearchCriteria(\"" + c.getVName()
								+ "\", form.get" + c.getGsName() + "().toLowerCase(), SearchOperation.LIKE));");
						ps.println("			}");
					} else { // must be a number
						ps.println("			if (form.get" + c.getGsName() + "Min() != null) {");
						ps.println("				searchSpec.add(new SearchCriteria(\"" + c.getVName()
								+ "\", form.get" + c.getGsName() + "Min(), SearchOperation.GREATER_THAN_EQUAL));");
						ps.println("			}");
						ps.println("			if (form.get" + c.getGsName() + "Max() != null) {");
						ps.println("				searchSpec.add(new SearchCriteria(\"" + c.getVName()
								+ "\", form.get" + c.getGsName() + "Max(), SearchOperation.LESS_THAN_EQUAL));");
						ps.println("			}");
					}
				}
				ps.println("");
				ps.println("		} else {");
				ps.println("			form = new " + className + "SearchForm();");
				ps.println("		}");
				ps.println("		Pageable pageable = PageRequest.of(form.getPage() - 1, form.getPageSize(),");
				ps.println("				form.getSort());");
				ps.println("		return " + fieldName + "Repository.findAll(searchSpec, pageable);");
				ps.println("	}");
				ps.println("	");
				ps.println("	public " + className + " save(" + className + " " + fieldName + ") {");
				if (hasPasswordField) {
					ps.println("		Optional<" + className + "> o = null;");
					ps.println("		if (" + fieldName + ".getId() > 0) {");
					ps.println("			o = " + fieldName + "Repository.findById(" + fieldName + ".getId());");
					ps.println("		}");
					ps.println("");
					for (ColInfo c : colNameToInfoMap.values()) {
						if (c.isCreated()) {
							ps.println("		if (" + fieldName + ".get" + c.getGsName() + "() == null) {");
							ps.println("			" + fieldName + ".set" + c.getGsName() + "(" + c.getDefaultVal()
									+ ");");
							ps.println("		}");
							ps.println("");
						} else if (c.isLastMod()) {
							ps.println(
									"		" + fieldName + ".set" + c.getGsName() + "(" + c.getDefaultVal() + ");");
						}
						if (c.isPassword()) {
							ps.println("		if (o != null && StringUtils.isBlank(" + fieldName + ".get"
									+ c.getGsName() + "())) {");
							ps.println("			" + fieldName + ".set" + c.getGsName() + "(o.get().get"
									+ c.getGsName() + "());");
							ps.println("		} else {");
							ps.println("			" + fieldName + ".set" + c.getGsName() + "(encrypt(" + fieldName
									+ ".get" + c.getGsName() + "()));");
							ps.println("		}");
							ps.println("");
						}
					}
				}
				ps.println("		return " + fieldName + "Repository.save(" + fieldName + ");");
				ps.println("	}");
				ps.println("	");
				ps.println("	public " + className + " get(" + pkinfo.getType() + " id) {");
				ps.println("		return " + fieldName + "Repository.findById(id).get();");
				ps.println("	}");
				ps.println("	");
				ps.println("	public void delete(" + pkinfo.getType() + " id) {");
				ps.println("		" + fieldName + "Repository.deleteById(id);");
				ps.println("	}");
				ps.println("");
				ps.println("}");
				ps.println("");
				log.warn("Wrote:" + p.toString());
			} catch (Exception e) {
				log.error("failed to create " + p, e);
				p.toFile().delete();
			}
		}
	}

	/**
	 * create application.properties
	 */
	private void writeAppProps() {
		String dbUrl = Utils.getProp(bundle, "db.url", "");
		String dbDriver = Utils.getProp(bundle, "db.driver", "");
		Path p = Utils.createFile(baseDir, "src/main/resources/application.properties");
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
				ps.println("spring.jpa.hibernate.ddl-auto=none");
				ps.println("spring.jpa.show-sql=true");
				ps.println(
						"spring.jpa.hibernate.naming.physical-strategy=org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl");
				if (dbDriver.contains("sqlite")) {
					ps.println("## SQLite also needs");
					ps.println("spring.jpa.database-platform=" + basePkg + ".db.SQLiteDialect");
					ps.println("spring.datasource.driver-class-name = org.sqlite.JDBC");
				}
				if (StringUtils.isBlank(dbUrl) && dbDriver.contains("sqlite")) {
					String folder = Utils.getProp(bundle, PROPKEY + ".outdir", ".");
					// db.url=jdbc:sqlite:L:/sites/git/Watchlist/watchlistDB.sqlite
					Path outPath = Utils.getPath(folder);
					if (!outPath.toFile().isDirectory())
						outPath.toFile().mkdirs();

					dbUrl = "jdbc:sqlite:" + outPath.toAbsolutePath().toString().replace('\\', '/') + "/"
							+ getBundelName() + "DB.sqlite";

				}
				ps.println("spring.datasource.url=" + dbUrl);

				String user = Utils.getProp(bundle, "db.user", null);
				if (!StringUtils.isBlank(user)) {
					ps.println("spring.datasource.username=" + user);
					ps.println("spring.datasource.password=" + Utils.getProp(bundle, "db.password", ""));
				}
				ps.println("");
				ps.println("logging.level.root=INFO");
				log.warn("Wrote:" + p.toString());
			} catch (Exception e) {
				log.error("failed to create " + p, e);
				p.toFile().delete();
			}
		}
	}

	/**
	 * Write a form / DTO for record
	 * 
	 * @param tableName
	 * @param className
	 * @param colNameToInfoMap
	 * @param comment
	 */
	private void writeForm(String tableName, Map<String, Map<String, ColInfo>> colsInfo) throws Exception {
		String className = Utils.tabToStr(renames, tableName);
		Map<String, ColInfo> colNameToInfoMap = colsInfo.get(className);

		String pkgNam = basePkg + ".form";
		String relPath = pkgNam.replace('.', '/');
		Path p = Utils.createFile(baseDir, "src/main/java", relPath, className + "Form.java");
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
				ps.println("package " + pkgNam + ';');
				ps.println("");
				ps.println("import java.io.Serializable;");
				ps.println("import lombok.Data;");
				ps.println("");
				ps.println("import " + basePkg + ".utils.MessageHelper;");
				ps.println("import " + basePkg + ".entity." + className + ";");
				ps.println("");
				addImports(ps, colNameToInfoMap, IMPORT_TYPE_FORM);
				ps.println(getClassHeader(tableName + " Form",
						"Class for holding data from the " + tableName + " table for editing.", ""));

				// Build form level rules if needed.
				StringBuilder sb = new StringBuilder();
				for (String key : colNameToInfoMap.keySet()) {
					if (PKEY_INFO.equals(key))
						continue;
					ColInfo info = (ColInfo) colNameToInfoMap.get(key);
					if (info.isPassword()) {
						if (sb.length() == 0) {
							sb.append("@FieldMatch.List({").append(System.lineSeparator());
						} else {
							sb.append(",").append(System.lineSeparator());
						}
						sb.append("		@FieldMatch(fieldName = \"" + info.getVName() + "\", secondFieldName = \""
								+ info.getVName() + "Confirm\", message = \"" + info.getVName() + ".mismatch\")")
								.append(System.lineSeparator());
					}
					if (info.isUnique() && info.isEmail()) {
						ps.println("@UniqueEmail.List({ @UniqueEmail(fieldName = \"" + info.getVName()
								+ "\", message = \"" + info.getVName() + ".unique\") })");
					}
				}
				if (sb.length() > 0) {
					sb.append("		})");
				}
				ps.println(sb.toString());
				ps.println("@Data");
				ps.println("public class " + className + "Form implements Serializable {");
				ps.println("	private static final long serialVersionUID = 1L;");
				ps.println("");
				for (String key : colNameToInfoMap.keySet()) {
					if (PKEY_INFO.equals(key))
						continue;
					ColInfo info = (ColInfo) colNameToInfoMap.get(key);
					if (info.isTimestamp())
						ps.println("    @DateTimeFormat(pattern = \"yyyy-MM-dd hh:mm:ss\")");
					if (info.isDate())
						ps.println("    @DateTimeFormat(pattern = \"yyyy-MM-dd\")");

					if (info.isJsonIgnore())
						ps.println("    @JsonIgnore");
					if (info.isPassword())
						ps.println("    @ValidatePassword(fieldName = \"" + info.getVName() + "Confirm\")");
					if (info.isEmail())
						ps.println("    @Email(message = \"{\"+MessageHelper.email_message+\"}\")");
//						if (info.isPk()) {
//							ps.println("    @Id");
//							if (info.getStype() == Types.INTEGER || info.getStype() == Types.BIGINT)
//								ps.println("    @GeneratedValue(strategy = GenerationType.IDENTITY)");
//						}
					if (info.getLength() > 0 && info.isString())
						ps.println("    @Length(max=" + info.getLength() + ")");
					if (info.isRequired() && info.isString())
						ps.println("    @NotBlank(message = \"{\"+MessageHelper.notBlank_message+\"}\")");
					if (info.isLastMod()) {
						ps.println("	private " + info.getType() + ' ' + info.getVName() + " = "
								+ info.getDefaultVal() + ";");
					} else {
						ps.println("	private " + info.getType() + ' ' + info.getVName() + ';');
					}
					if (info.isPassword()) {
						ps.println("    @ValidatePassword(fieldName = \"" + info.getVName() + "\")");
						ps.println("	private " + info.getType() + ' ' + info.getVName() + "Confirm;");
					}
				}

				ps.println("");
				ps.println("	/**");
				ps.println("	 * Clones " + className + " obj into form");
				ps.println("	 *");
				ps.println("	 * @param obj");
				ps.println("	 */");
				ps.println("	public static " + className + "Form getInstance(" + className + " obj) {");
				ps.println("		" + className + "Form form = new " + className + "Form();");
				ps.println("		if (obj != null) {");
				for (String key : colNameToInfoMap.keySet()) {
					if (PKEY_INFO.equals(key))
						continue;
					ColInfo info = (ColInfo) colNameToInfoMap.get(key);
					// by default do not pass passwords to forms
					if (info.isPassword()) {
						ps.println("//		form.set" + info.getGsName() + "(obj.get" + info.getGsName() + "());");
						ps.println(
								"//		form.set" + info.getGsName() + "Confirm(obj.get" + info.getGsName() + "());");
					} else {
						ps.println("			form.set" + info.getGsName() + "(obj.get" + info.getGsName() + "());");
					}
				}
				ps.println("		}");
				ps.println("		return form;");
				ps.println("	}");
				ps.println("}");
				log.warn("Wrote:" + p.toString());
			} catch (Exception e) {
				log.error("failed to create " + p, e);
				p.toFile().delete();
			}

			log.debug("Created bean" + p);
		}
	}

	private void writeSearchForm(String tableName, Map<String, Map<String, ColInfo>> colsInfo) throws Exception {
		String className = Utils.tabToStr(renames, tableName);
		Map<String, ColInfo> colNameToInfoMap = colsInfo.get(className);

		String pkgNam = basePkg + ".search";
		String relPath = pkgNam.replace('.', '/');
		Path p = Utils.createFile(baseDir, "src/main/java", relPath, className + "SearchForm.java");
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
				ps.println("package " + pkgNam + ';');
				ps.println("");
				ps.println("import java.io.Serializable;");
				addImports(ps, colNameToInfoMap, IMPORT_TYPE_FORM);
				ps.println("");
				ps.println("import org.springframework.data.domain.Sort;");
				ps.println("import org.springframework.format.annotation.DateTimeFormat;");
				ps.println("import " + basePkg + ".utils.MessageHelper;");
				ps.println("import " + basePkg + ".entity." + className + ";");
				ps.println("");
				ps.println("import lombok.Data;");
				ps.println("");
				ps.println(getClassHeader(tableName + " Form",
						"Class for holding data from the " + tableName + " table for editing.", ""));

				ps.println("@Data");
				ps.println("public class " + className + "SearchForm implements Serializable {");
				ps.println("	private static final long serialVersionUID = 1L;");
				ps.println("");
				for (String key : colNameToInfoMap.keySet()) {
					if (PKEY_INFO.equals(key))
						continue;
					ColInfo info = (ColInfo) colNameToInfoMap.get(key);
					if (info.isDate()) {
						ps.println("	@DateTimeFormat(pattern = \"yyyy-MM-dd hh:mm:ss\")");
					}
					if ("String".equals(info.getType())) {
						ps.println("	private " + info.getType() + ' ' + info.getVName() + " = "
								+ info.getDefaultVal() + ";");
					} else {
						ps.println("	private " + info.getType() + ' ' + info.getVName() + "Min;");
						ps.println("	private " + info.getType() + ' ' + info.getVName() + "Max;");
					}
				}

				ps.println("	private String sortField = \"id\";");
				ps.println("	private int page = 1;");
				ps.println("	private int pageSize = 10;");
				ps.println("	private boolean sortAsc = true;");
				ps.println("	private int totalPages = 0;");
				ps.println("	private long totalItems = 0;");
				ps.println("	/**");
				ps.println("	 * Clones " + className + " obj into form");
				ps.println("	 *");
				ps.println("	 * @param obj");
				ps.println("	 */");
				ps.println("	public static " + className + "SearchForm getInstance(" + className + " obj) {");
				ps.println("		" + className + "SearchForm form = new " + className + "SearchForm();");
				for (String key : colNameToInfoMap.keySet()) {
					if (PKEY_INFO.equals(key))
						continue;
					ColInfo info = (ColInfo) colNameToInfoMap.get(key);
					if ("String".equals(info.getType())) {
						ps.println("		form.set" + info.getGsName() + "(obj.get" + info.getGsName() + "());");
					} else {
						ps.println("		form.set" + info.getGsName() + "Min(obj.get" + info.getGsName() + "());");
						ps.println("		form.set" + info.getGsName() + "Max(obj.get" + info.getGsName() + "());");
					}
				}
				ps.println("		return form;");
				ps.println("	}");
				ps.println("");
				ps.println("	/**");
				ps.println("	 * Generate a Sort from fields");
				ps.println("	 * @return");
				ps.println("	 */");
				ps.println("	public Sort getSort() {");
				ps.println("		if (sortAsc)");
				ps.println("			return Sort.by(sortField).ascending();");
				ps.println("");
				ps.println("		return Sort.by(sortField).descending();");
				ps.println("	}");
				ps.println("");
				ps.println("	public String getSortDir() {");
				ps.println("		if (sortAsc)");
				ps.println("			return \"asc\";");
				ps.println("		else");
				ps.println("			return \"desc\";");
				ps.println("	}");
				ps.println("");
				ps.println("	public String getReverseSortDir() {");
				ps.println("		if (sortAsc)");
				ps.println("			return \"desc\";");
				ps.println("		else");
				ps.println("			return \"asc\";");
				ps.println("	}");
				ps.println("");
				ps.println("	boolean getSortAscFlip() {");
				ps.println("		return !sortAsc;");
				ps.println("	}");
				ps.println("}");
				log.warn("Wrote:" + p.toString());
			} catch (Exception e) {
				log.error("failed to create " + p, e);
				p.toFile().delete();
			}

			log.debug("Created bean" + p);
		}
	}

	/**
	 * write pojo class
	 * 
	 * @param tableName        String
	 * @param className
	 * @param colNameToInfoMap
	 * @param comment          any addition header comments
	 * @throws Exception
	 * 
	 */
	private void writeBean(String tableName, Map<String, Map<String, ColInfo>> colsInfo) throws Exception {
		String className = Utils.tabToStr(renames, tableName);
		Map<String, ColInfo> colNameToInfoMap = colsInfo.get(className);

		String pkgNam = basePkg + ".entity";
		String relPath = pkgNam.replace('.', '/');
		String outFile = "src/main/java/" + relPath + '/' + className + ".java";
		Path p = Utils.createFile(baseDir, outFile);
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
				ps.println("package " + pkgNam + ';');
				ps.println("");
				ps.println("import java.io.Serializable;");
				ps.println("");
				ps.println("import javax.persistence.Column;");
				ps.println("import javax.persistence.Entity;");
				ps.println("import javax.persistence.GeneratedValue;");
				ps.println("import javax.persistence.GenerationType;");
				ps.println("import javax.persistence.Id;");
				ps.println("import javax.persistence.Table;");
				ps.println("import lombok.Data;");
				addImports(ps, colNameToInfoMap, IMPORT_TYPE_BEAN);
				StringBuilder comment = new StringBuilder();
				for (String key : colNameToInfoMap.keySet()) {
					if (PKEY_INFO.equals(key))
						continue;
					ColInfo info = (ColInfo) colNameToInfoMap.get(key);
					if (!StringUtils.isBlank(info.getComment())) {
						comment.append(info.getComment());
					}
				}
				ps.println(getClassHeader(tableName + " Bean",
						"Class for holding data from the " + tableName + " table.", comment.toString()));
				ps.println("@Data");
				ps.println("@Entity");
				ps.println("@Table(name = \"`" + tableName + "`\")");
				ps.println("public class " + className + " implements Serializable {");
				ps.println("	private static final long serialVersionUID = 1L;");
				ps.println("");
				for (String key : colNameToInfoMap.keySet()) {
					if (PKEY_INFO.equals(key))
						continue;
					ColInfo info = (ColInfo) colNameToInfoMap.get(key);
					if (!StringUtils.isBlank(info.getForeignTable())) {

						ps.println("	@ManyToOne");
						ps.print("	@JoinColumn(name = \"" + info.getColName());
						if (!StringUtils.isBlank(info.getForeignCol()))
							ps.print("\", referencedColumnName = \"" + info.getForeignCol());
						ps.println("\")");
						ps.println("	private " + info.getType() + " " + info.getVName() + ";");
						continue;
					}

					if (info.isCreated() || info.isLastMod())
						ps.println("    @EqualsAndHashCode.Exclude");
					if (info.isTimestamp())
						ps.println("    @DateTimeFormat(pattern = \"yyyy-MM-dd hh:mm:ss\")");
					if (info.isDate())
						ps.println("    @DateTimeFormat(pattern = \"yyyy-MM-dd\")");

					if (info.isJsonIgnore())
						ps.println("    @JsonIgnore");
					if (info.isPk()) {
						ps.println("    @Id");
						if (info.getStype() == Types.INTEGER || info.getStype() == Types.BIGINT)
							ps.println("    @GeneratedValue(strategy = GenerationType.IDENTITY)");
					}
					StringBuilder sb = new StringBuilder("	@Column(name = \"" + info.getColName() + "\"");
					if (info.isUnique())
						sb.append(", unique = true");
					if (info.isRequired())
						sb.append(", nullable = false");
					if ("String".equals(info.getType()) && info.getLength() > 0) {
						sb.append(", length = " + info.getLength());
					}
					sb.append(")");
					ps.println(sb.toString());
					if (info.isLastMod()) {
						ps.println("	private " + info.getType() + ' ' + info.getVName() + " = "
								+ info.getDefaultVal() + ";");
					} else {
						ps.println("	private " + info.getType() + ' ' + info.getVName() + ';');
					}
				}

				ps.println("}");
				log.warn("Wrote:" + p.toString());
			} catch (Exception e) {
				log.error("failed to create " + p, e);
				p.toFile().delete();
				throw e;
			}

			log.debug("Created bean" + outFile);
		}
	}

	public static void printUsage(String error, Exception e) {
		if (error != null)
			System.err.println(error);
		System.err.println("USAGE: Genspring [options] [table names]");
		System.err.println("Where options are:");
		System.err.println(
				"-double = use Double instead of BigDecimal for entities beans. Can be overridden in properties file.");
//		System.err.println(
//				"-toString = generate toString() methods for entities beans. Can be overridden in properties file.");
		System.err.println("");
		System.err.println("if table names not given then runs on all tables in DB.");
		System.err.println("");
		System.err.println("Note: be sure to set properties in resources/genSpring.properties before running.");

		if (e != null)
			log.error(error, e);

		System.exit(1);
	}

	public List<String> getTablesNames(Db db) throws SQLException {
		List<String> tableNames = new ArrayList<String>();
		String dbName = db.getDbName();

		String query = "SHOW TABLES;"; // mySQL
		if (db.getDbUrl().indexOf("sqlserver") > -1) {
			query = "SELECT NAME,INFO FROM sysobjects WHERE type= 'U'";
		} else if (db.getDbUrl().indexOf("sqlite") > -1) {
			query = "SELECT name FROM sqlite_master WHERE type='table';";
		}
		Connection conn = db.getConnection(PROPKEY + ".main()");
		Statement stmt = conn.createStatement();
		if (log.isInfoEnabled()) {
			log.info("query=" + query);
		}
		ResultSet rs = stmt.executeQuery(query);
		try {
			int rowcount = 0;
			if (rs.last()) {
				rowcount = rs.getRow();
				rs.beforeFirst(); // not rs.first() because the rs.next() below will move on, missing the first
									// element
			}
			log.info("found " + rowcount + " tables");
		} catch (Exception e) {
			log.warn("Could not get table count  ", e);
		}
		while (rs.next()) {
			try {
				String name = null;
				if (db.isMySQL()) {
					name = rs.getString("Tables_in_" + dbName);
				} else {
					name = rs.getString("name").toLowerCase();
				}
				if (!StringUtils.isBlank(name) && !filteredTables.contains(name))
					tableNames.add(name);
				else
					log.info("skipping:" + name);
			} catch (Exception e) {
				printUsage("main() crashed ", e);
			}
		}
		db.close(PROPKEY + ".main()");

		return tableNames;
	}

	/**
	 * Entry point for this app
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			Db db = new Db(PROPKEY + ".main()", PROPKEY);
			int i = 0;
			GenSpring obj = new GenSpring(PROPKEY);
			List<String> tableNames = new ArrayList<String>();
			for (; i < args.length; i++) {
				if (args[i].length() > 0 && args[i].charAt(0) == '-') {
					if ("-h".equals(args[i])) {
						printUsage(null, null);
					}
				} else if (args[i].length() > 0 && args[i].charAt(0) == '-') {
					if ("-beanEquals".equals(args[i])) {
//						obj.beanEquals = true;
					}
				} else if (args[i].length() > 0 && args[i].charAt(0) == '-') {
					if ("-double".equals(args[i])) {
						obj.useDouble = true;
					}
				} else if (args[i].length() > 0 && args[i].charAt(0) == '-') {
					if ("-toString".equals(args[i])) {
						obj.beanToString = true;
					}
				} else {
					tableNames.add(args[i]);
				}
			}

			if (tableNames.isEmpty()) {
				tableNames = obj.getTablesNames(db);
			}
			obj.writeProject(tableNames);
		} catch (Exception e) {
			printUsage("main() crashed  ", e);
		}

		log.info("Done");
	}
}
