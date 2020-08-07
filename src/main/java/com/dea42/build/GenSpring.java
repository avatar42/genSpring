package com.dea42.build;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
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
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.zip.DataFormatException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dea42.common.Db;
import com.dea42.common.Utils;

/**
 * Title: GenSpring <br>
 * Description: Class for generating SIMPLE CRUD web app from table info<br>
 * Copyright: Copyright (c) 2020<br>
 * Company: RMRR<br>
 * <br>
 * 
 * @author David Abigt<br>
 * @version 1.0<br>
 *          <br>
 *          See http://jakarta.apache.org/ojb/jdbc-types.html for data type info
 */
public class GenSpring {
	private static final Logger LOGGER = LoggerFactory.getLogger(GenSpring.class.getName());
	public static final String PROPKEY = "genSpring";
	// Note change pom.xml to match
	public static final String genSpringVersion = "0.2.3";

	private boolean useDouble = false;
	private boolean beanToString = false;
	private boolean beanEquals = false;
	private ResourceBundle bundle;
	private ResourceBundle renames;
	private String bundleName;
	private String srcGroupId = "com.dea42";
	private String srcArtifactId = "genspring";
	private String srcPkg;
	private String srcPath;
	private String baseGroupId;
	private String baseModule;
	private String baseArtifactId;
	private String basePkg;
	private String basePath;
	private String baseDir;
	private String appVersion;
	private int year = 2001;
	private String schema = null;

	public static final String PKEY_INFO = "PRIMARY_KEY_INFO";

	public GenSpring() throws IOException {
		this(PROPKEY);
	}

	public GenSpring(String bundleName) throws IOException {
		this.bundleName = bundleName;
	}

	/**
	 * Simplify test generation by just passing props use to gen to the app's tests
	 */
	private void writeTestProps() {
		Path p = createFile("/src/test/resources/test.properties");
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
				for (String key : bundle.keySet()) {
					String val = bundle.getString(key);
					if (StringUtils.isBlank(val))
						ps.println(key + "=");
					else
						ps.println(key + "=" + val);
				}
				LOGGER.warn("Wrote:" + p.toString());
			} catch (Exception e) {
				LOGGER.error("failed to create " + p, e);
				p.toFile().delete();
			}
		}
		p = createFile("/src/test/resources/rename.properties");
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
				for (String key : renames.keySet()) {
					String val = renames.getString(key);
					if (StringUtils.isBlank(val))
						ps.println(key + "=");
					else
						ps.println(key + "=" + val);
				}
				LOGGER.warn("Wrote:" + p.toString());
			} catch (Exception e) {
				LOGGER.error("failed to create " + p, e);
				p.toFile().delete();
			}
		}
	}

	private Path createFile(String relPath) {
		Path p = Utils.getPath(baseDir, relPath);
		try {
			Files.createDirectories(p.getParent());
			p = Files.createFile(p);
		} catch (IOException e) {
			LOGGER.warn(e.getMessage() + " exists will skip");
			return null;
		}

		return p;
	}

	public boolean isUseDouble() {
		return useDouble;
	}

	public void setUseDouble(boolean useDouble) {
		this.useDouble = useDouble;
	}

	/**
	 * Creates a top level page to ref the other pages.
	 * 
	 * @param list
	 */
	private void writeIndex(Set<String> set) {
		Path p = createFile("/src/main/resources/templates/index.html");
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
				ps.println(htmlHeader("header.home", null));
				for (String clsName : set) {
					String fieldName = clsName.substring(0, 1).toLowerCase() + clsName.substring(1);
					ps.println("    	<a th:href=\"@{/" + fieldName + "s}\">" + clsName + "</a><br>");
				}
				ps.println("    	<a th:href=\"@{/api/}\">/api/</a><br>");
				ps.println("    	<a th:href=\"@{/login}\">Login</a><br>");
				ps.println(htmlFooter());
				LOGGER.warn("Wrote:" + p.toString());
			} catch (Exception e) {
				LOGGER.error("failed to create " + p, e);
				p.toFile().delete();
			}
		}
	}

	private void writeApiIndex(Set<String> set) {
		Path p = createFile("/src/main/resources/templates/api_index.html");
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
				ps.println(htmlHeader("header.restApi", null));
				for (String clsName : set) {
					String fieldName = clsName.substring(0, 1).toLowerCase() + clsName.substring(1);
					ps.println("    	<a th:href=\"@{/api/" + fieldName + "s}\">" + clsName + "</a><br>");
				}
				ps.println("    	<a th:href=\"@{/}\">Home</a><br>");
				ps.println("    	<a th:href=\"@{/login}\">Login</a><br>");
				ps.println(htmlFooter());
				LOGGER.warn("Wrote:" + p.toString());
			} catch (Exception e) {
				LOGGER.error("failed to create " + p, e);
				p.toFile().delete();
			}
		}
	}

	private void writeNav(Set<String> set) {
		Path p = createFile("/src/main/resources/templates/fragments/header.html");
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
				ps.println("<!DOCTYPE html>");
				ps.println("<html xmlns:th=\"http://www.thymeleaf.org\">");
				ps.println("<head>");
				ps.println("<link rel=\"stylesheet\" media=\"screen\"");
				ps.println("	th:href=\"@{/resources/css/bootstrap.min.css}\" />");
				ps.println("</head>");
				ps.println("<body>");
				ps.println("	<div th:fragment=\"header\">");
				ps.println("		<nav class=\"navbar navbar-inverse navbar-fixed-top\">");
				ps.println("			<div class=\"container\">");
				ps.println("				<div class=\"navbar-header\">");
				ps.println(
						"					<button type=\"button\" class=\"navbar-toggle\" data-toggle=\"collapse\"");
				ps.println("						data-target=\".nav-collapse\">");
				ps.println(
						"						<span class=\"icon-bar\"></span> <span class=\"icon-bar\"></span> <span");
				ps.println("							class=\"icon-bar\"></span>");
				ps.println("					</button>");
				ps.println(
						"					<a class=\"navbar-brand\" th:href=\"@{/}\" th:text=\"#{app.name}\"></a>");
				ps.println("				</div>");
				ps.println("				<div class=\"navbar-collapse collapse\">");
				ps.println("					<ul class=\"nav navbar-nav\">");
				ps.println("						<li id=\"guiMenu\" class=\"dropdown\"><a href=\"#\"");
				ps.println("							class=\"dropdown-toggle\" data-toggle=\"dropdown\"><span");
				ps.println(
						"								th:text=\"#{header.gui}\"></span> <b class=\"caret\"></b> </a>");
				ps.println("							<ul class=\"dropdown-menu\">");
				for (String clsName : set) {
					String fieldName = clsName.substring(0, 1).toLowerCase() + clsName.substring(1);
					ps.println("								<li th:classappend=\"${module == '" + fieldName
							+ "' ? 'active' : ''}\">");
					ps.println("    								<a id=\"guiItem" + clsName + "\" th:href=\"@{/"
							+ fieldName + "s}\" th:text=\"#{class." + clsName + "}\"></a></li>");
				}
				ps.println("							</ul></li>");
				ps.println("						<li id=\"restMenu\" class=\"dropdown\"><a href=\"#\"");
				ps.println("							class=\"dropdown-toggle\" data-toggle=\"dropdown\"><span");
				ps.println(
						"								th:text=\"#{header.restApi}\"></span> <b class=\"caret\"></b></a>");
				ps.println("							<ul class=\"dropdown-menu\">");
				for (String clsName : set) {
					String fieldName = clsName.substring(0, 1).toLowerCase() + clsName.substring(1);
					ps.println("								<li th:classappend=\"${module == '" + fieldName
							+ "' ? 'active' : ''}\">");
					ps.println("    								<a id=\"apiItem" + clsName + "\" th:href=\"@{/api/"
							+ fieldName + "s}\" th:text=\"#{class." + clsName + "}\"></a></li>");
				}
				ps.println("							</ul></li>");
				ps.println("					</ul>");
				ps.println("					<ul class=\"nav navbar-nav navbar-right\">");
				ps.println("						<li id=\"langMenu\" class=\"dropdown\"><a href=\"#\"");
				ps.println("							class=\"dropdown-toggle\" data-toggle=\"dropdown\"><span");
				ps.println(
						"								th:text=\"#{lang.change}\"></span> <b class=\"caret\"></b></a>");
				ps.println("							<ul class=\"dropdown-menu\">");
				ps.println("								<li th:classappend=\"'active'\"><a");
				ps.println(
						"									th:href=\"@{/international?lang=en}\" th:text=\"#{lang.en}\"></a></li>");
				ps.println("								<li th:classappend=\"'active'\"><a");
				ps.println(
						"									th:href=\"@{/international?lang=fr}\" th:text=\"#{lang.fr}\"></a></li>");
				ps.println("								<li th:classappend=\"'active'\"><a");
				ps.println(
						"									th:href=\"@{/international?lang=de}\" th:text=\"#{lang.de}\"></a></li>");
				ps.println("							</ul></li>");
				ps.println("					</ul>");
				ps.println("					<ul class=\"nav navbar-nav navbar-right\">");
				ps.println("						<li th:if=\"${#authorization.expression('!isAuthenticated()')}\">");
				ps.println("							<a th:href=\"@{/login}\"> <span");
				ps.println(
						"								class=\"glyphicon glyphicon-log-in\" aria-hidden=\"true\"></span>&nbsp;<span");
				ps.println("								th:text=\"#{signin.signin}\"></span>");
				ps.println("						</a>");
				ps.println("");
				ps.println("						</li>");
				ps.println("						<li th:if=\"${#authorization.expression('isAuthenticated()')}\">");
				ps.println("							<a th:href=\"@{#}\" onclick=\"$('#form').submit();\"> <span");
				ps.println(
						"								class=\"glyphicon glyphicon-log-out\" aria-hidden=\"true\"></span>&nbsp;<span");
				ps.println("								th:text=\"#{signin.logout}\"></span>");
				ps.println("						</a>");
				ps.println(
						"							<form style=\"visibility: hidden\" id=\"form\" method=\"post\"");
				ps.println("								action=\"#\" th:action=\"@{/logout}\"></form>");
				ps.println("						</li>");
				ps.println("					</ul>");
				ps.println("				</div>");
				ps.println("				<!--/.nav-collapse -->");
				ps.println("			</div>");
				ps.println("		</nav>");
				ps.println("		<br>");
				ps.println("		<div class=\"container\">");
				ps.println("			<!-- /* Handle the flash message in container */-->");
				ps.println("			<th:block th:if=\"${message != null}\">");
				ps.println("				<!-- /* The message code is returned from the @Controller */ -->");
				ps.println("				<div");
				ps.println(
						"					th:replace=\"fragments/alert :: alert (type=${#strings.toLowerCase(message.type)}, message=#{${message.message}(${#authentication.name})})\">&nbsp;</div>");
				ps.println("			</th:block>");
				ps.println("		</div>");
				ps.println("	</div>");
				ps.println("	<!-- End of header -->");
				ps.println("</body>");
				ps.println("</html>");
				LOGGER.warn("Wrote:" + p.toString());
			} catch (Exception e) {
				LOGGER.error("failed to create " + p, e);
				p.toFile().delete();
			}
		}
	}

	public void copyCommon() throws IOException {
		Path staticPath = Utils.getPath("static");
		Files.walkFileTree(staticPath, new FileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				String relDir = staticPath.relativize(dir).toString().replace('\\', '/').replace(srcPath, basePath);
				Path target = Utils.getPath(baseDir + "/" + relDir);
				Files.createDirectories(target);

				LOGGER.debug("preVisitDirectory: " + dir + "->" + target);
				return FileVisitResult.CONTINUE;
			}

			/**
			 * Copy file into new tree converting package / paths as needed
			 */
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				LOGGER.info("Copying:" + file);
				String data = new String(Files.readAllBytes(file));
				if (file.getFileName().endsWith("pom.xml")) {
					data = data.replace(srcPkg, basePkg);
					data = data.replace("<artifactId>" + srcArtifactId + "</artifactId>",
							"<artifactId>" + baseArtifactId + "</artifactId>");
					data = data.replace("<version>1.0.0-SNAPSHOT</version>",
							"<version>" + appVersion + "-SNAPSHOT</version>");
				} else {
					data = data.replace(srcPkg, basePkg);
					data = data.replace(srcGroupId, baseGroupId);
					data = data.replace(srcArtifactId, baseModule);
					data = data.replace("genSpringVersion", genSpringVersion);
					data = data.replace("@version 1.0<br>", "@version " + appVersion + "<br>");
				}
				Path relPath = staticPath.relativize(file);
				String outFile = baseDir + "/" + relPath.toString().replace('\\', '/').replace(srcPath, basePath);
				Path p = createFile(outFile);
				if (p != null) {
					try {
						Files.write(p, data.getBytes(), StandardOpenOption.APPEND);
						LOGGER.warn("Wrote:" + p.toString());
					} catch (Exception e) {
						LOGGER.error("failed to create " + p, e);
					}
				}
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				LOGGER.debug("visitFileFailed: " + file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				LOGGER.debug("postVisitDirectory: " + dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	protected void initVars() throws IOException {
		GregorianCalendar gc = new GregorianCalendar();
		year = gc.get(Calendar.YEAR);

		bundle = ResourceBundle.getBundle(bundleName);
		renames = ResourceBundle.getBundle("rename");
		baseDir = Utils.getProp(bundle, PROPKEY + ".outdir");
		schema = Utils.getProp(bundle, PROPKEY + ".schema");
		baseGroupId = Utils.getProp(bundle, PROPKEY + ".pkg");
		baseModule = Utils.getProp(bundle, PROPKEY + ".module");
		baseArtifactId = Utils.getProp(bundle, PROPKEY + ".artifactId", baseModule);
		basePkg = baseGroupId + '.' + baseModule;
		basePath = basePkg.replace('.', '/');
		appVersion = Utils.getProp(bundle, PROPKEY + ".version", "1.0");
		beanToString = Utils.getProp(bundle, PROPKEY + ".beanToString", beanToString);
		beanEquals = Utils.getProp(bundle, PROPKEY + ".beanEquals", beanEquals);

		srcPkg = srcGroupId + '.' + srcArtifactId;
		srcPath = srcPkg.replace('.', '/');
		File outDir = Utils.getPath(baseDir).toFile();
		if (!outDir.exists()) {
			if (!outDir.mkdirs()) {
				throw new IOException("Could not create output dir:" + baseDir);
			}
		}

	}

	/**
	 * Generate folder structure and project level files
	 * 
	 * @param tableNames
	 * @throws Exception
	 */
	public void writeProject(List<String> tableNames) throws Exception {
		initVars();

		copyCommon();

		writeAppProps();
		writeTestProps();

		Map<String, Map<String, ColInfo>> colsInfo = new HashMap<String, Map<String, ColInfo>>();
		for (String tableName : tableNames) {
			String clsName = Utils.tabToStr(renames, tableName);
			colsInfo.put(clsName, genTableMaintFiles(tableName));
		}
		writeMockBase(colsInfo.keySet());
		writeApiController(colsInfo.keySet());
		writeApiControllerTest(colsInfo);
		writeNav(colsInfo.keySet());
		writeIndex(colsInfo.keySet());
		writeApiIndex(colsInfo.keySet());
		updateMsgProps(colsInfo);
	}

	/**
	 * @return the bundleName
	 */
	public String getBundleName() {
		return bundleName;
	}

	/**
	 * @param bundleName the bundleName to set
	 */
	public void setBundleName(String bundleName) {
		this.bundleName = bundleName;
	}

	public void getKeys(Db db, String tableName) {
		String create;
		Connection conn = db.getConnection(PROPKEY + ".genFiles()");
		// SELECT * FROM pragma_foreign_key_list('my_table');
		if (db.getDbUrl().indexOf("mysql") > -1) {
			String sql = "SELECT * FROM pragma_foreign_key_list('" + tableName + "');";
			try {
				Statement stmt = conn.createStatement();
				stmt.setMaxRows(1);
				LOGGER.debug("query=" + sql);
				ResultSet rs = stmt.executeQuery(sql);
				if (rs.next()) {
					// TODO: test this. Reported failing with "java.sql.SQLException: Column 'Create
					// Table' not found."
					// See https://dev.mysql.com/doc/refman/8.0/en/show-create-table.html
					create = rs.getString("Create Table");
				}
			} catch (SQLException e) {
				LOGGER.error(sql + " failed", e);
			}
		}

	}

	/**
	 * Entry point for generating all the files you need for Spring maint screens to
	 * Add/Edit/Delete/Search record for a table.
	 * 
	 * @param tableName
	 * @throws Exception
	 */
	public Map<String, ColInfo> genTableMaintFiles(String tableName) throws Exception {

		String firstColumnName = null;
		String pkCol = null;
		String className = Utils.tabToStr(renames, tableName);
		String create = "";

		Db db = new Db(PROPKEY + ".genFiles()", bundleName, Utils.getProp(bundle, PROPKEY + ".outdir", "."));
		Connection conn = db.getConnection(PROPKEY + ".genFiles()");

		if (db.getDbUrl().indexOf("mysql") > -1) {
			String sql = "show create table " + tableName;
			try {
				Statement stmt = conn.createStatement();
				stmt.setMaxRows(1);
				LOGGER.debug("query=" + sql);
				ResultSet rs = stmt.executeQuery(sql);
				if (rs.next()) {
					create = rs.getString("Create Table");
				}
			} catch (SQLException e) {
				LOGGER.error(sql + " failed", e);
			}
		}

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

		LOGGER.debug("tableName:" + tableName);
		String query = "SELECT * FROM " + tableName;
		Statement stmt = conn.createStatement();
		stmt.setMaxRows(1);
		LOGGER.debug("query=" + query);
		ResultSet rs = stmt.executeQuery(query);
		ResultSetMetaData rm = rs.getMetaData();
		int size = rm.getColumnCount();
		// Map indexed by column name
		Map<String, ColInfo> cols = new HashMap<String, ColInfo>(size);
		List<String> jsonIgnoreCols = Utils.getPropList(bundle, className + ".JsonIgnore");
		List<String> uniqueCols = Utils.getPropList(bundle, className + ".unique");

		for (int i = 1; i <= size; i++) {
			ColInfo colInfo = new ColInfo();
			colInfo.setfNum(i);

			String columnName = rm.getColumnName(i);
			colInfo.setColName(columnName);
			if (firstColumnName == null) {
				firstColumnName = columnName;
			}
			colInfo.setPk(rm.isAutoIncrement(i) || columnName.equals(pkCol));
			if (jsonIgnoreCols.contains(columnName)) {
				colInfo.setJsonIgnore(true);
			}
			if (uniqueCols.contains(columnName)) {
				colInfo.setUnique(true);
			}
			String type = rm.getColumnTypeName(i).toUpperCase();
			int len = rm.getColumnDisplaySize(i);
			if (len >= 2147483647) {
				if (rm.getPrecision(i) < 2147483647) {
					len = rm.getPrecision(i);
				} else {
					len = 0;
				}
			}

			colInfo.setLength(len);
			columnName = Utils.tabToStr(renames, columnName);
			colInfo.setVName(columnName.substring(0, 1).toLowerCase() + columnName.substring(1));
			colInfo.setGsName(columnName);
			// a java.sql.Types
			int stype = rm.getColumnType(i);
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
				break;

			case Types.REAL:
				colInfo.setType("Float");
				colInfo.setColPrecision(rm.getPrecision(i));
				colInfo.setColScale(rm.getScale(i));
				break;

			case Types.FLOAT:
			case Types.DECIMAL:
			case Types.DOUBLE:
			case Types.NUMERIC:
				if (useDouble) {
					colInfo.setType("Double");
				} else {
					colInfo.setType("BigDecimal");
					colInfo.setImportStr("import java.math.BigDecimal;");
				}
				colInfo.setColPrecision(rm.getPrecision(i));
				colInfo.setColScale(rm.getScale(i));
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
//				colInfo.setImportStr("import java.sql.Timestamp;");
				colInfo.setImportStr("import java.util.Date;" + System.lineSeparator()
						+ "import org.springframework.format.annotation.DateTimeFormat;");
				break;
//			case Types.DATE:
//				colInfo.setType("Date");
//				colInfo.setImportStr("import java.sql.Date;");
//				break;
			case Types.VARBINARY:
			case Types.CLOB:
				colInfo.setType("byte[]");
				break;
			default:
				// if its something else treat it like a String for now
				System.err.println("Type " + type + " Unknown treating like String");
				colInfo.setType("String");
			}

			if (colInfo.getColName() != null && colInfo.getType() != null && colInfo.getType().length() > 0) {
				LOGGER.info("storing:" + colInfo);
				cols.put(colInfo.getConstName(), colInfo);
			}
		}

		// write bean with helpers

		// TODO: add support for composite keys
		DatabaseMetaData metaData = conn.getMetaData();
		rs = metaData.getPrimaryKeys("", null, tableName);
		while (rs.next()) {
			pkCol = rs.getString("COLUMN_NAME");
			LOGGER.info("Table name: " + rs.getString("TABLE_NAME"));
			LOGGER.info("Column name: " + pkCol);
			LOGGER.info("Catalog name: " + rs.getString("TABLE_CAT"));
			LOGGER.info("Primary key sequence: " + rs.getString("KEY_SEQ"));
			LOGGER.info("Primary key name: " + rs.getString("PK_NAME"));
			LOGGER.info(" ");
		}
		if (StringUtils.isBlank(pkCol)) {
			pkCol = firstColumnName;
			LOGGER.error(tableName + " does not have a primary key. Using " + pkCol);
		}

		rs = metaData.getImportedKeys("", schema, tableName);
		while (rs.next()) {
			ColInfo ci = cols.get(rs.getString("FKCOLUMN_NAME").toUpperCase());
			ci.setForeignTable(rs.getString("PKTABLE_NAME"));
			ci.setForeignCol(rs.getString("PKCOLUMN_NAME"));
			LOGGER.info(rs.getString("PKTABLE_CAT") + " => primary key table catalog being imported (may be null)");
			LOGGER.info(rs.getString("PKTABLE_SCHEM") + " => primary key table schema being imported (may be null) ");
			LOGGER.info(rs.getString("PKTABLE_NAME") + " => primary key table name being imported ");
			LOGGER.info(rs.getString("PKCOLUMN_NAME") + " => primary key column name being imported");
			LOGGER.info(rs.getString("FKTABLE_CAT") + " => foreign key table catalog (may be null)");
			LOGGER.info(rs.getString("FKTABLE_SCHEM") + " => foreign key table schema (may be null)");
			LOGGER.info(rs.getString("FKTABLE_NAME") + " => foreign key table name ");
			LOGGER.info(rs.getString("FKCOLUMN_NAME") + " => foreign key column name");
			LOGGER.info(rs.getString("KEY_SEQ")
					+ " => sequence number within a foreign key( a valueof 1 represents the first column of the foreign key, a value of 2 would represent the second column within the foreign key).");
			LOGGER.info(
					rs.getString("UPDATE_RULE") + " => What happens to a foreign key when the primary key is updated:");

			LOGGER.info(rs.getString("DELETE_RULE") + " => What happens to the foreign key when primary is deleted.");
			LOGGER.info(rs.getString("FK_NAME") + " => foreign key name (may be null) ");
			LOGGER.info(rs.getString("PK_NAME") + " => primary key name (may be null) ");
			LOGGER.info(rs.getString("DEFERRABILITY") + " DEFERRABILITY");
			LOGGER.info(" ");
		}
		db.close(PROPKEY + ".genFiles()");

		ColInfo pkinfo = cols.get(pkCol.toUpperCase());
		pkinfo.setPk(true);

		// Map indexed by field name
		TreeMap<String, ColInfo> namList = new TreeMap<String, ColInfo>();
		for (String colName : cols.keySet()) {
			ColInfo info = cols.get(colName);
			namList.put(info.getVName(), info);
			if (info.isPk()) {
				namList.put(PKEY_INFO, info);
			}
		}
		cols.put(PKEY_INFO, pkinfo);

		writeBean(tableName, className, namList, create);
		writeRepo(className, pkinfo);
		writeService(className, pkinfo);
		writeListPage(className, namList);
		writeObjController(className, pkinfo);
		writeObjControllerTest(className, namList);
		writeEditPage(className, namList);

		return cols;
	}

	/**
	 * Generate comment header for a class
	 * 
	 * @param className
	 * @param description
	 * @return
	 */
	private String getClassHeader(String className, String description) {
		StringBuilder sb = new StringBuilder();
		sb.append("/**").append(System.lineSeparator());
		sb.append(" * Title: " + className + " <br>").append(System.lineSeparator());
		sb.append(" * Description: " + description + " <br>").append(System.lineSeparator());
		String tmp = Utils.getProp(bundle, PROPKEY + ".Copyright", "");
		if (!org.apache.commons.lang3.StringUtils.isBlank(tmp)) {
			sb.append(" * Copyright: " + tmp + year + "<br>").append(System.lineSeparator());
		}
		tmp = Utils.getProp(bundle, PROPKEY + ".Company", "");
		if (!org.apache.commons.lang3.StringUtils.isBlank(tmp)) {
			sb.append(" * Company: " + tmp + "<br>").append(System.lineSeparator());
		}
		sb.append(" * @author Gened by " + this.getClass().getCanonicalName() + " version " + genSpringVersion + "<br>")
				.append(System.lineSeparator());
		sb.append(" * @version " + appVersion + "<br>").append(System.lineSeparator());
		sb.append(" */");

		return sb.toString();
	}

	private void writeApiController(Set<String> set) {
		String pkgNam = basePkg + ".controller";
		String relPath = pkgNam.replace('.', '/');
		String outFile = "/src/main/java/" + relPath + "/ApiController.java";
		Path p = createFile(outFile);
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
				ps.println("package " + pkgNam + ';');
				ps.println("");
				ps.println("import org.springframework.beans.factory.annotation.Autowired;");
				ps.println("import org.springframework.web.bind.annotation.GetMapping;");
				ps.println("import org.springframework.web.bind.annotation.RequestMapping;");
				ps.println("import org.springframework.web.bind.annotation.RestController;");
				for (String clsName : set) {
					ps.println("import " + basePkg + ".entity." + clsName + ";");
					ps.println("import " + basePkg + ".service." + clsName + "Services;");
				}
				ps.println("");
				ps.println("import java.util.List;");
				ps.println(getClassHeader("ApiController", "Api REST Controller."));
				ps.println("@RestController");
				ps.println("@RequestMapping(\"/api\")");
				ps.println("public class ApiController {");
				ps.println("");
				for (String clsName : set) {
					String fieldName = clsName.substring(0, 1).toLowerCase() + clsName.substring(1);
					ps.println("    @Autowired");
					ps.println("    private " + clsName + "Services " + fieldName + "Services;");
				}
				ps.println("");
				ps.println("    public ApiController(){");
				ps.println("    }");
				for (String clsName : set) {
					String fieldName = clsName.substring(0, 1).toLowerCase() + clsName.substring(1);
					ps.println("");
					ps.println("    @GetMapping(\"/" + fieldName + "s\")");
					ps.println("    public List<" + clsName + "> getAll" + clsName + "s(){");
					ps.println("        return this." + fieldName + "Services.listAll();");
					ps.println("    }");
				}
				ps.println("}");
				LOGGER.warn("Wrote:" + p.toString());
			} catch (Exception e) {
				LOGGER.error("failed to create " + p, e);
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
		String outFile = baseDir + "/src/main/resources/messages.properties";
		Path p = Utils.getPath(outFile);
		String data = "";
		boolean dataChged = false;
		try {
			data = new String(Files.readAllBytes(p));
			Set<String> set = colsInfo.keySet();
			for (String clsName : set) {
				// If table already in there then skip
				if (!data.contains("## for " + clsName + System.lineSeparator())) {
					dataChged = true;
					data = data + "## for " + clsName + System.lineSeparator();
					data = data + "class." + clsName + "=" + clsName + System.lineSeparator();
					Map<String, ColInfo> namList = colsInfo.get(clsName);
					Iterator<String> it = namList.keySet().iterator();
					while (it.hasNext()) {
						String name = it.next();
						if (PKEY_INFO.equals(name))
							continue;
						ColInfo info = (ColInfo) namList.get(name);
						data = data + clsName + "." + info.getVName() + "=" + info.getGsName() + System.lineSeparator();
					}
					data = data + System.lineSeparator();
				}
			}
			if (dataChged) {
				Files.write(p, data.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
				LOGGER.warn("Wrote:" + p.toString());
			} else {
				LOGGER.warn(p.toString() + " up to date so will skip.");
			}
		} catch (Exception e) {
			LOGGER.error("failed to update " + p, e);
		}
	}

	/**
	 * Created mock unit test of ApiController. TODO: need updated to use MockBase
	 * 
	 * @param colsInfo
	 * @param pkinfo   TODO
	 */
	private void writeApiControllerTest(Map<String, Map<String, ColInfo>> colsInfo) {
		Set<String> set = colsInfo.keySet();
		String pkgNam = basePkg + ".controller";
		String relPath = pkgNam.replace('.', '/');
		String outFile = "/src/test/java/" + relPath + "/ApiControllerTest.java";
		Path p = createFile(outFile);
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
				ps.println("import java.util.List;");
				ps.println("");
//				ps.println("import org.junit.Before;");
				ps.println("import org.junit.Test;");
				ps.println("import org.junit.runner.RunWith;");
//				ps.println("import org.springframework.beans.factory.annotation.Autowired;");
				ps.println("import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;");
//				ps.println("import org.springframework.boot.test.mock.mockito.MockBean;");
				ps.println("import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;");
//				ps.println("import org.springframework.test.web.servlet.MockMvc;");
//				ps.println("import org.springframework.test.web.servlet.setup.MockMvcBuilders;");
//				ps.println("import org.springframework.web.context.WebApplicationContext;");
				ps.println("");
				ps.println("import " + basePkg + ".MockBase;");
				for (String clsName : set) {
					ps.println("import " + basePkg + ".entity." + clsName + ";");
//					ps.println("import " + basePkg + ".service." + clsName + "Services;");
				}
				ps.println("");
				ps.println(getClassHeader("ApiControllerTest", "REST Api Controller Test."));
				ps.println("@RunWith(SpringJUnit4ClassRunner.class)");
				ps.println("@WebMvcTest(ApiController.class)");
				ps.println("public class ApiControllerTest extends MockBase {");
				ps.println("");
				for (String clsName : set) {
					Map<String, ColInfo> namList = colsInfo.get(clsName);
					ColInfo pkinfo = namList.get(PKEY_INFO);
					if (pkinfo == null) {
						LOGGER.error("No PK found for " + clsName);
						System.exit(2);
					}
					String idMod = "";
					if ("Long".equals(pkinfo.getType()))
						idMod = "l";
					String fieldName = clsName.substring(0, 1).toLowerCase() + clsName.substring(1);
					ps.println("");
					ps.println("	/**");
					ps.println("	 * Test method for");
					ps.println("	 * {@link " + basePkg + ".controller." + clsName + "Controller#getAll" + clsName
							+ "s(org.springframework.ui.Model)}.");
					ps.println("	 */");
					ps.println("	@Test");
					ps.println("	public void testGetAll" + clsName + "s() throws Exception {");
					ps.println("		List<" + clsName + "> list = new ArrayList<>();");
					ps.println("		" + clsName + " o = new " + clsName + "();");
//					ps.println("		o.setId(1" + idMod + ");");
					Iterator<String> it = namList.keySet().iterator();
					while (it.hasNext()) {
						String name = it.next();
						if (PKEY_INFO.equals(name))
							continue;
						ColInfo info = (ColInfo) namList.get(name);
						if (info.isString()) {
							int endIndex = info.getLength();
							if (endIndex > 0) {
								ps.println("        o.set" + info.getGsName() + "(getTestString(" + endIndex + "));");
							}
						} else if (info.isPk()) {
							ps.println("		o.set" + info.getGsName() + "(1" + idMod + ");");
						}
					}
					ps.println("		list.add(o);");
					ps.println("");
					ps.println("		given(" + fieldName + "Services.listAll()).willReturn(list);");
					ps.println("");
					ps.println("		this.mockMvc.perform(get(\"/api/" + fieldName
							+ "s\").with(user(\"user\").roles(\"ADMIN\"))).andExpect(status().isOk())");
					it = namList.keySet().iterator();
					while (it.hasNext()) {
						String name = it.next();
						if (PKEY_INFO.equals(name))
							continue;

						ColInfo info = (ColInfo) namList.get(name);
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
						if (it.hasNext()) {
							ps.println("");
						}
					}
					ps.println(";");
					ps.println("	}");
					ps.println("");
				}
				ps.println("}");
				LOGGER.warn("Wrote:" + p.toString());
			} catch (Exception e) {
				LOGGER.error("failed to create " + p, e);
				p.toFile().delete();
			}
		}
	}

	/**
	 * gen HTML header for page
	 * 
	 * @param clsName  or key to be used for the title
	 * @param pageType it null clsName is assumed to be a message key. Should match
	 *                 property in messages.properties as in listView for
	 *                 edit.listView
	 * @return
	 */
	private String htmlHeader(String clsName, String pageType) {
		StringBuilder sb = new StringBuilder();

		sb.append("<!DOCTYPE html>" + System.lineSeparator());
		sb.append("<html lang=\"en\" xmlns:th=\"http://www.thymeleaf.org\">" + System.lineSeparator());
		sb.append("<head>" + System.lineSeparator() + "<meta charset=\"UTF-8\" />" + System.lineSeparator());
		if (pageType == null)
			sb.append("<title th:text=\"#{" + clsName + "}\"></title>" + System.lineSeparator());
		else
			sb.append("<title th:text=\"#{edit." + pageType + "} + ' ' + #{class." + clsName + "}\"></title>"
					+ System.lineSeparator());
		sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />" + System.lineSeparator());
		sb.append("<link rel=\"stylesheet\" media=\"screen\"" + System.lineSeparator());
		sb.append("	th:href=\"@{/resources/css/bootstrap.min.css}\" />" + System.lineSeparator());
		sb.append("<link rel=\"stylesheet\" media=\"screen\"" + System.lineSeparator());
		sb.append("	th:href=\"@{/resources/css/site.css}\" />" + System.lineSeparator());
		sb.append("<script th:src=\"@{/resources/js/jquery.min.js}\"></script>" + System.lineSeparator());
		sb.append("<script th:src=\"@{/resources/js/bootstrap.min.js}\"></script>" + System.lineSeparator());
		sb.append("</head>" + System.lineSeparator());
		sb.append("<body>" + System.lineSeparator());

		sb.append("	<div th:replace=\"fragments/header :: header\">&nbsp;</div>" + System.lineSeparator());

		return sb.toString();
	}

	private String htmlFooter() {
		StringBuilder sb = new StringBuilder();
		sb.append("	<div th:insert=\"fragments/footer :: footer\">&copy; 2020 default</div>" + System.lineSeparator());
		sb.append("</body>");
		sb.append("</html>");

		return sb.toString();
	}

	/**
	 * Write the CrudRepository interface for a bean
	 * 
	 * @param clsName
	 * @param pkType
	 */
	private void writeRepo(String clsName, ColInfo pkinfo) {
		String pkgNam = basePkg + ".repo";
		String relPath = pkgNam.replace('.', '/');
		String outFile = "/src/main/java/" + relPath + '/' + clsName + "Repository.java";
		Path p = createFile(outFile);
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
				ps.println("package " + pkgNam + ';');
				ps.println("");
				ps.println("import org.springframework.data.jpa.repository.JpaRepository;");
				ps.println("import org.springframework.stereotype.Repository;");
				ps.println("import " + basePkg + ".entity." + clsName + ";");
				ps.println("");
				ps.println(getClassHeader(clsName + "Repository", "Class for the " + clsName + " Repository."));
				ps.println("@Repository");
				ps.println("public interface " + clsName + "Repository extends JpaRepository<" + clsName + ", "
						+ pkinfo.getType() + ">{");
				if ((pkinfo.getStype() != Types.INTEGER) && (pkinfo.getStype() != Types.BIGINT)) {
					ps.println(
							"//TODO: Primary key is not int or bigint which will require custom code to be added below");
				}
				ps.println("}");
				LOGGER.warn("Wrote:" + p.toString());
			} catch (Exception e) {
				LOGGER.error("failed to create " + p, e);
				p.toFile().delete();
			}
		}
	}

	private void writeEditPage(String clsName, TreeMap<String, ColInfo> namList) {
		ColInfo pkinfo = namList.get(PKEY_INFO);
		String fieldName = clsName.substring(0, 1).toLowerCase() + clsName.substring(1);
		String outFile = "/src/main/resources/templates/edit_" + fieldName + ".html";
		Set<String> set = namList.keySet();
		Path p = createFile(outFile);
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
				ps.println(htmlHeader(clsName, "edit"));
				ps.println("	<div class=\"container\">");
				ps.println("		<h1 th:if=\"${" + fieldName + "." + pkinfo.getVName() + " == 0}\"");
				ps.println("			th:text=\"#{edit.new} + ' ' + #{class." + clsName + "}\"></h1>");
				ps.println("		<h1 th:if=\"${" + fieldName + "." + pkinfo.getVName() + " > 0}\"");
				ps.println("			th:text=\"#{edit.edit} + ' ' + #{class." + clsName + "}\"></h1>");
				ps.println("		<br />");
				ps.println("		<form action=\"#\" th:action=\"@{/" + fieldName + "s/save}\" th:object=\"${"
						+ fieldName + "}\"");
				ps.println("			method=\"post\">");
				ps.println("");
				ps.println("			<table >");
				ps.println("				<tr th:if=\"${" + fieldName + "." + pkinfo.getVName()
						+ " > 0}\">				");
				ps.println("					<td th:text=\"#{" + clsName + "." + pkinfo.getVName() + "} + ':'\">"
						+ pkinfo.getColName() + ":</td>");
				ps.println("					<td>");
				ps.println("						<input type=\"text\" th:field=\"*{" + pkinfo.getVName()
						+ "}\" readonly=\"readonly\" />");
				ps.println("					</td>");
				ps.println("				</tr>");
				Iterator<String> it = set.iterator();
				while (it.hasNext()) {
					String name = it.next();
					if (PKEY_INFO.equals(name))
						continue;
					ColInfo info = (ColInfo) namList.get(name);
					if (!pkinfo.getColName().equals(info.getColName())) {
						ps.println("				<tr>");
						ps.println("            		<td>" + info.getColName() + ":</td>");
						ps.println("					<td>");
						ps.println("						<input type=\"text\" th:field=\"*{" + info.getVName()
								+ "}\" />");
						ps.println("					</td>");
						ps.println("				</tr>");
					}
				}
				ps.println("				<tr>");
				ps.println(
						"					<td colspan=\"2\"><button type=\"submit\" name=\"action\" value=\"save\" th:text=\"#{edit.save}\"></button> <button type=\"submit\" name=\"action\" value=\"cancel\" th:text=\"#{edit.cancel}\"></button></td>");
				ps.println("				</tr>");
				ps.println("			</table>");
				ps.println("		</form>");
				ps.println("	</div>");
				ps.println(htmlFooter());
				LOGGER.warn("Wrote:" + p.toString());
			} catch (Exception e) {
				LOGGER.error("failed to create " + p, e);
				p.toFile().delete();
			}
		}
	}

	/**
	 * Get a list of all the columns to include in list pages
	 * 
	 * @param clsName
	 * @param namList
	 * @return
	 * @throws DataFormatException
	 */
	private Set<String> getListKeys(String clsName, TreeMap<String, ColInfo> namList) throws DataFormatException {
		Set<String> set = namList.keySet();
		List<String> listCols = Utils.getPropList(bundle, clsName + ".list");
		if (listCols.isEmpty()) {
			set = namList.keySet();
		} else {
			set = listCols.stream().collect(Collectors.toSet());
			// validate list against DB data
			Object[] names = set.toArray();
			for (Object name : names) {
				if (PKEY_INFO.equals(name))
					continue;
				ColInfo info = (ColInfo) namList.get(name);
				if (info == null) {
					info = (ColInfo) namList.get(((String) name).toLowerCase());
					if (info != null) {
						set.remove(name);
						set.add(((String) name).toLowerCase());
						LOGGER.warn(name + " not in " + clsName + ".list but " + ((String) name).toLowerCase()
								+ " was so using it instead");
					} else {
						throw new DataFormatException(
								name + " in " + clsName + ".list columns but no info found for it in " + namList);
					}
				}
			}
		}

		return set;
	}

	private void writeListPage(String clsName, TreeMap<String, ColInfo> namList) {
		ColInfo pkinfo = namList.get(PKEY_INFO);
		String fieldName = clsName.substring(0, 1).toLowerCase() + clsName.substring(1);
		String outFile = "/src/main/resources/templates/" + fieldName + "s.html";

		Path p = createFile(outFile);
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
				Set<String> processed = new HashSet<String>();
				Set<String> set = getListKeys(clsName, namList);
				ps.println(htmlHeader(clsName, "listView"));
				ps.println("	<div class=\"container\">");
				ps.println("		<h1 th:text=\"#{class." + clsName + "} + ' ' + #{edit.list}\"></h1>");
				ps.println("		<a th:href=\"@{/" + fieldName + "s/new}\" th:text=\"#{edit.new} + ' ' + #{class."
						+ clsName + "}\"></a> <br /><br />");
				ps.println("");
				ps.println("	    <table>");
				ps.println("	        <tr>");

				Iterator<String> it = set.iterator();
				while (it.hasNext()) {
					String name = it.next();
					if (PKEY_INFO.equals(name))
						continue;
					ColInfo info = (ColInfo) namList.get(name);
					if (!processed.contains(name)) {

						if (namList.containsKey(name + "link")) {
							ps.println("            <th th:text=\"#{" + clsName + "." + info.getVName() + "}\">"
									+ info.getColName() + "</th>");
							processed.add(name);
							processed.add(name + "link");
						} else if (name.endsWith("link") && namList.containsKey(name.substring(0, name.length() - 4))) {
							name = name.substring(0, name.length() - 4);
							info = (ColInfo) namList.get(name);
							ps.println("            <th th:text=\"#{" + clsName + "." + info.getVName() + "}\">"
									+ info.getColName() + "</th>");
							processed.add(name);
							processed.add(name + "link");
						} else {
							ps.println("            <th th:text=\"#{" + clsName + "." + info.getVName() + "}\">"
									+ info.getColName() + "</th>");
							processed.add(name);
						}
					}
				}
				ps.println("            <th th:text=\"#{edit.actions}\"></th>");
				ps.println("	        </tr>");
				ps.println("	        <tr th:each=\"" + fieldName + ":${" + fieldName + "s}\">");
				processed = new HashSet<String>();
				it = set.iterator();
				while (it.hasNext()) {
					String name = it.next();
					if (PKEY_INFO.equals(name))
						continue;
					ColInfo info = (ColInfo) namList.get(name);
					if (!processed.contains(name)) {
						if (namList.containsKey(name + "link")) {
							ColInfo infoLnk = (ColInfo) namList.get(name + "link");
							ps.println("	            <td><a th:href=\"@{${" + fieldName + "." + infoLnk.getVName()
									+ "}}\" th:text=\"${" + fieldName + "." + info.getVName() + "}\"></a></td>");
							processed.add(name);
							processed.add(name + "link");
						} else if (name.endsWith("link") && namList.containsKey(name.substring(0, name.length() - 4))) {
							name = name.substring(0, name.length() - 4);
							ColInfo infoLnk = (ColInfo) info;
							info = (ColInfo) namList.get(name);
							ps.println("	            <td><a th:href=\"@{${" + fieldName + "." + infoLnk.getVName()
									+ "}}\" th:text=\"${" + fieldName + "." + info.getVName() + "}\"></a></td>");
							processed.add(name);
							processed.add(name + "link");
						} else {
							ps.println("	            <td th:text=\"${" + fieldName + "." + info.getVName()
									+ "}\"></td>");
							processed.add(name);
						}
					}
				}
				ps.println("				<td><a th:href=\"@{'/" + fieldName + "s/edit/' + ${" + fieldName + "."
						+ pkinfo.getVName() + "}}\" th:text=\"#{edit.edit}\"></a>");
				ps.println("					&nbsp;&nbsp;&nbsp; <a th:href=\"@{'/" + fieldName + "s/delete/' + ${"
						+ fieldName + "." + pkinfo.getVName() + "}}\" th:text=\"#{edit.delete}\"></a>");
				ps.println("				</td>");
				ps.println("");
				ps.println("	        </tr>");
				ps.println("	    </table>");
				ps.println("    </div>");
				ps.println("");
				ps.println(htmlFooter());
				LOGGER.warn("Wrote:" + p.toString());
			} catch (Exception e) {
				LOGGER.error("failed to create " + p, e);
				p.toFile().delete();
			}
		}
	}

	private void writeMockBase(Set<String> set) {
		String pkgNam = basePkg;
		String relPath = pkgNam.replace('.', '/');
		String outFile = "/src/test/java/" + relPath + "/MockBase.java";
		Path p = createFile(outFile);
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
				ps.println("package " + pkgNam + ';');
				ps.println("");
				ps.println("import static org.hamcrest.CoreMatchers.containsString;");
				ps.println(
						"import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;");
				ps.println(
						"import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;");
				ps.println("import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;");
				ps.println("import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;");
				ps.println("import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;");
				ps.println("import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;");
				ps.println(
						"import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;");
				ps.println("import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;");
				ps.println("");
				ps.println("import java.util.Map;");
				ps.println("");
				ps.println("import javax.servlet.Filter;");
				ps.println("");
				ps.println("import org.apache.commons.lang3.StringUtils;");
				ps.println("import org.apache.tools.ant.UnsupportedAttributeException;");
				ps.println("import org.junit.Before;");
				ps.println("import org.springframework.beans.factory.annotation.Autowired;");
				ps.println("import org.springframework.boot.test.mock.mockito.MockBean;");
				ps.println(
						"import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor;");
				ps.println("import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;");
				ps.println("import org.springframework.test.web.servlet.ResultActions;");
				ps.println("import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;");
				ps.println("import org.springframework.test.web.servlet.request.RequestPostProcessor;");
				ps.println("import org.springframework.test.web.servlet.setup.MockMvcBuilders;");
				ps.println("import org.springframework.web.context.WebApplicationContext;");
				if (!set.contains("Account")) {
					ps.println("import " + basePkg + ".service.AccountServices;");
					ps.println("import " + basePkg + ".repo.AccountRepository;");
				}
				for (String clsName : set) {
					ps.println("import " + basePkg + ".repo." + clsName + "Repository;");
					ps.println("import " + basePkg + ".service." + clsName + "Services;");
				}
				ps.println("");
				ps.println("import " + basePkg + ".utils.Message;");
				ps.println("import " + basePkg + ".utils.Utils;");
				ps.println("");
				ps.println(getClassHeader("MockBase", "The base class for mock testing."));
				ps.println("public class MockBase extends UnitBase {");
				if (!set.contains("Account")) {
					ps.println("    @MockBean");
					ps.println("    protected AccountServices accountServices;");
					ps.println("    @MockBean");
					ps.println("    protected AccountRepository accountRepository;");
				}
				for (String clsName : set) {
					String fieldName = clsName.substring(0, 1).toLowerCase() + clsName.substring(1);
					ps.println("    @MockBean");
					ps.println("    protected " + clsName + "Services " + fieldName + "Services;");
					ps.println("    @MockBean");
					ps.println("    protected " + clsName + "Repository " + fieldName + "Repository;");
				}
				ps.println("	@Autowired");
				ps.println("	protected WebApplicationContext webApplicationContext;");
				ps.println("");
				ps.println("	@Autowired");
				ps.println("	private Filter springSecurityFilterChain;");
				ps.println("");
				ps.println("	@Before()");
				ps.println("	public void setup() {");
				ps.println("		// Init MockMvc Object and build");
				ps.println("		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)");
				ps.println(
						"				.apply(SecurityMockMvcConfigurers.springSecurity()).addFilters(springSecurityFilterChain).build();");
				ps.println("	}");
				ps.println("");
				ps.println("	/**");
				ps.println("	 * Send request and if not expecting request check header for nav");
				ps.println("	 * ");
				ps.println("	 * @param type");
				ps.println("	 * @param relURL");
				ps.println("	 * @param modelName");
				ps.println("	 * @param model");
				ps.println("	 * @param params");
				ps.println("	 * @param login");
				ps.println("	 * @param redirectedUrl if null expects 200 return code.");
				ps.println("	 * @return ResultActions object for further checks.");
				ps.println("	 * @throws Exception");
				ps.println("	 */");
				ps.println(
						"	protected ResultActions send(String type, String relURL, String modelName, Object model, Map<String, String> params,");
				ps.println("			String login, String redirectedUrl) throws Exception {");
				ps.println("		MockHttpServletRequestBuilder req = null;");
				ps.println("		if (SEND_GET.equals(type)) {");
				ps.println("			req = get(relURL);");
				ps.println("		} else if (SEND_POST.equals(type)) {");
				ps.println("			req = post(relURL);");
				ps.println("		} else {");
				ps.println("			throw new UnsupportedAttributeException(\"Send type not supported\", type);");
				ps.println("		}");
				ps.println("		if (!StringUtils.isAllBlank(modelName)) {");
				ps.println("			req = req.flashAttr(modelName, model);");
				ps.println("		}");
				ps.println("		if (params != null && !params.isEmpty()) {");
				ps.println("			for (String key : params.keySet()) {");
				ps.println("				req = req.param(key, params.get(key));");
				ps.println("			}");
				ps.println("		}");
				ps.println("		if (!StringUtils.isAllBlank(login)) {");
				ps.println("			UserRequestPostProcessor urpp = user(login);");
				ps.println("			if (ADMIN_USER.equals(login)) {");
				ps.println("				urpp = urpp.roles(ADMIN_ROLE);");
				ps.println("			} else {");
				ps.println("				urpp = urpp.roles(TEST_ROLE);");
				ps.println("			}");
				ps.println("			req = req.with(urpp);");
				ps.println("		} else {");
				ps.println("			req = req.with(anonymous());");
				ps.println("		}");
				ps.println("");
				ps.println("		ResultActions result = this.mockMvc.perform(req);");
				ps.println("		if (redirectedUrl != null) {");
				ps.println("			// If redirect then just check right one");
				ps.println(
						"			result.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(redirectedUrl));");
				ps.println("		} else if (headless.contains(relURL)) {");
				ps.println("			// For pops just check status");
				ps.println("			result.andExpect(status().isOk());");
				ps.println("		} else {");
				ps.println("			// else do full header check");
				ps.println("			checkHeader(result, login);");
				ps.println("		}");
				ps.println("		return result;");
				ps.println("	}");
				ps.println("");
				ps.println("	/**");
				ps.println("	 * Check header is on page and complete. TODO: add active module check");
				ps.println("	 * ");
				ps.println("	 * @param result");
				ps.println("	 * @throws Exception");
				ps.println("	 */");
				ps.println("	public void checkHeader(ResultActions result, String user) throws Exception {");
				ps.println("		result.andExpect(status().isOk());");
				ps.println("		contentContainsKey(result, \"app.name\", false);");
				ps.println("		// GUI menu");
				ps.println("		contentContainsKey(result, \"header.gui\", false);");
				for (String clsName : set) {
					ps.println("		contentContainsKey(result, \"class." + clsName + "\", false);");
				}
				ps.println("// REST menu");
				ps.println("		contentContainsKey(result, \"header.restApi\", false);");
				for (String clsName : set) {
					String fieldName = clsName.substring(0, 1).toLowerCase() + clsName.substring(1);
					ps.println("		contentContainsMarkup(result, \"/api/" + fieldName + "s\", false);");
				}
				ps.println("// Login / out");
				ps.println("		contentContainsKey(result, \"lang.eng\", false);");
				ps.println("		contentContainsKey(result, \"lang.fr\", false);");
				ps.println("		contentContainsKey(result, \"lang.de\", false);");
				ps.println("");
				ps.println("		if (user == null)");
				ps.println("			contentContainsKey(result, \"signin.signin\", false);");
				ps.println("		else");
				ps.println("			contentContainsKey(result, \"signin.logout\", false);");
				ps.println("");
				ps.println("	}");
				ps.println("");
				ps.println("	/**");
				ps.println("	 * Do mock get as admin user, check the nav header then return the handle.");
				ps.println("	 * ");
				ps.println("	 * @param relURL");
				ps.println("	 * @return");
				ps.println("	 * @throws Exception");
				ps.println("	 */");
				ps.println("	protected ResultActions getAsAdmin(String relURL) throws Exception {");
				ps.println("		return send(SEND_GET, relURL, null, null, null, ADMIN_USER, null);");
				ps.println("	}");
				ps.println("");
				ps.println("	/**");
				ps.println("	 * Do mock get as admin user, check for redirect. Wrapper for send().");
				ps.println("	 * ");
				ps.println("	 * @param relURL");
				ps.println("	 * @throws Exception");
				ps.println("	 */");
				ps.println(
						"	protected void getAsAdminRedirectExpected(String relURL, String redirectedUrl) throws Exception {");
				ps.println("		send(SEND_GET, relURL, null, null, null, ADMIN_USER, redirectedUrl);");
				ps.println("	}");
				ps.println("");
				ps.println("	/**");
				ps.println("	 * Do mock get not logged in, check for redirect. Wrapper for send().");
				ps.println("	 * ");
				ps.println("	 * @param relURL");
				ps.println("	 * @throws Exception");
				ps.println("	 */");
				ps.println(
						"	protected void getAsNoOneRedirectExpected(String relURL, String redirectedUrl) throws Exception {");
				ps.println("		send(SEND_GET, relURL, null, null, null, ADMIN_USER, redirectedUrl);");
				ps.println("	}");
				ps.println("");
				ps.println("	/**");
				ps.println("	 * Do mock get not logged in, check the nav header then return the handle.");
				ps.println("	 * Wrapper for send().");
				ps.println("	 * ");
				ps.println("	 * @param relURL");
				ps.println("	 * @throws Exception");
				ps.println("	 */");
				ps.println("	protected ResultActions getAsNoOne(String relURL) throws Exception {");
				ps.println("		return send(SEND_GET, relURL, null, null, null, null, null);");
				ps.println("	}");
				ps.println("");
				ps.println("	/**");
				ps.println("	 * Confirm text of key is in content");
				ps.println("	 * ");
				ps.println("	 * @param result");
				ps.println("	 * @param key");
				ps.println("	 * @param failIfExists flip to fail if there");
				ps.println("	 */");
				ps.println(
						"	public void contentContainsKey(ResultActions result, String key, boolean failIfExists) {");
				ps.println("		String expectedText = Utils.getProp(getMsgBundle(), key);");
				ps.println("		contentContainsMarkup(result, expectedText, failIfExists);");
				ps.println("	}");
				ps.println("");
				ps.println("	/**");
				ps.println("	 * Check to see if htmlString is in HTML content of result");
				ps.println("	 * ");
				ps.println("	 * @param result");
				ps.println("	 * @param htmlString");
				ps.println("	 */");
				ps.println("	public void contentContainsMarkup(ResultActions result, String htmlString) {");
				ps.println("		contentContainsMarkup(result, htmlString, false);");
				ps.println("	}");
				ps.println("");
				ps.println("	/**");
				ps.println("	 * Check to see if htmlString is in HTML content of result");
				ps.println("	 * ");
				ps.println("	 * @param result");
				ps.println("	 * @param htmlString   if null or blank String then just returns");
				ps.println("	 * @param failIfExists flip to fail if exists instead of when missing.");
				ps.println("	 */");
				ps.println(
						"	public void contentContainsMarkup(ResultActions result, String htmlString, boolean failIfExists) {");
				ps.println("		// if");
				ps.println("		if (StringUtils.isBlank(htmlString))");
				ps.println("			return;");
				ps.println("");
				ps.println("		try {");
				ps.println("			result.andExpect(content().string(containsString(htmlString)));");
				ps.println("			if (failIfExists) {");
				ps.println("				LOGGER.error(\"Found '\" + htmlString + \"' in \" + content());");
				ps.println("				fail(\"Found '\" + htmlString + \"' in content\");");
				ps.println("			}");
				ps.println("		} catch (Throwable e) {");
				ps.println("			if (!failIfExists) {");
				ps.println("				LOGGER.error(\"Did not find '\" + htmlString + \"' in \" + content(), e);");
				ps.println("				fail(\"Did not find '\" + htmlString + \"' in content\");");
				ps.println("			}");
				ps.println("		}");
				ps.println("	}");
				ps.println("");
				ps.println("	public void expectSuccessMsg(ResultActions ra, String msgKey) throws Exception {");
				ps.println("		expectSuccessMsg(ra, msgKey, new Object[0]);");
				ps.println("	}");
				ps.println("");
				ps.println(
						"	public void expectSuccessMsg(ResultActions ra, String msgKey, Object... args) throws Exception {");
				ps.println("		Message msg = new Message(msgKey, Message.Type.SUCCESS, args);");
				ps.println("		// Compares type, key and params.");
				ps.println("		ra.andExpect(flash().attribute(Message.MESSAGE_ATTRIBUTE, msg));");
				ps.println("	}");
				ps.println("");
				ps.println("	public void expectErrorMsg(ResultActions ra, String msgKey) throws Exception {");
				ps.println("		expectErrorMsg(ra, msgKey, new Object[0]);");
				ps.println("	}");
				ps.println("");
				ps.println(
						"	public void expectErrorMsg(ResultActions ra, String msgKey, Object... args) throws Exception {");
				ps.println("		Message msg = new Message(msgKey, Message.Type.DANGER, args);");
				ps.println("		// Compares type, key and params.");
				ps.println("		ra.andExpect(flash().attribute(Message.MESSAGE_ATTRIBUTE, msg));");
				ps.println("	}");
				ps.println("");
				ps.println("	/**");
				ps.println("	 * @deprecated see send()");
				ps.println("	 * @return");
				ps.println("	 */");
				ps.println("	protected RequestPostProcessor getMockTestUser() {");
				ps.println("		LOGGER.debug(\"Using the user:\" + TEST_USER + \" with role:ROLE_\" + TEST_ROLE);");
				ps.println("		UserRequestPostProcessor rtn = user(TEST_USER).roles(TEST_ROLE);");
				ps.println("		LOGGER.debug(\"Returning:\" + rtn);");
				ps.println("		return rtn;");
				ps.println("	}");
				ps.println("");
				ps.println("	/**");
				ps.println("	 * @deprecated see send()");
				ps.println("	 * @return");
				ps.println("	 */");
				ps.println("	protected RequestPostProcessor getMockTestAdmin() {");
				ps.println(
						"		LOGGER.debug(\"Using the user:\" + ADMIN_USER + \" with role:ROLE_\" + ADMIN_ROLE);");
				ps.println("		UserRequestPostProcessor rtn = user(ADMIN_USER).roles(ADMIN_ROLE);");
				ps.println("		LOGGER.debug(\"Returning:\" + rtn);");
				ps.println("		return rtn;");
				ps.println("	}");
				ps.println("}");
				ps.println("");
				LOGGER.warn("Wrote:" + p.toString());
			} catch (Exception e) {
				LOGGER.error("failed to create " + p, e);
				p.toFile().delete();
			}
		}
	}

	private void writeObjControllerTest(String clsName, TreeMap<String, ColInfo> namList) {
		ColInfo pkinfo = namList.get(PKEY_INFO);
		String idMod = "";
		if ("Long".equals(pkinfo.getType()))
			idMod = "l";
		String pkgNam = basePkg + ".controller";
		String relPath = pkgNam.replace('.', '/');
		String outFile = "/src/test/java/" + relPath + '/' + clsName + "ControllerTest.java";
		String fieldName = clsName.substring(0, 1).toLowerCase() + clsName.substring(1);
		Path p = createFile(outFile);
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
				Set<String> set = getListKeys(clsName, namList);
				ps.println("package " + pkgNam + ';');
				ps.println("import static org.mockito.BDDMockito.given;");
				ps.println("import java.util.ArrayList;");
				ps.println("import java.util.List;");
				ps.println("import org.junit.Test;");
				ps.println("import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;");
				ps.println("import org.springframework.test.web.servlet.ResultActions;");
				ps.println("import com.google.common.collect.ImmutableMap;");
				ps.println("");
				ps.println("import " + basePkg + ".MockBase;");
				ps.println("import " + basePkg + ".entity." + clsName + ";");
				ps.println("");
				ps.println(getClassHeader(clsName + "ControllerTest", clsName + "Controller."));
				ps.println("@WebMvcTest(" + clsName + "Controller.class)");
				ps.println("public class " + clsName + "ControllerTest extends MockBase {");
				ps.println("	private " + clsName + " get" + clsName + "(" + pkinfo.getType() + " "
						+ pkinfo.getVName() + ") {");
				ps.println("		" + clsName + " o = new " + clsName + "();");
				ps.println("		o.set" + pkinfo.getGsName() + "(" + pkinfo.getVName() + ");");
				Iterator<String> it = set.iterator();
				while (it.hasNext()) {
					String name = it.next();
					if (PKEY_INFO.equals(name))
						continue;
					ColInfo info = (ColInfo) namList.get(name);
					if (info.isString()) {
						int endIndex = info.getLength();
						if (endIndex > 0) {
							ps.println("        o.set" + info.getGsName() + "(getTestString(" + endIndex + "));");
						}
					}
				}
				ps.println("		return o;");
				ps.println("	}");
				ps.println("");
				ps.println("	/**");
				ps.println("	 * Test method for");
				ps.println("	 * {@link " + basePkg + ".controller." + clsName + "Controller#getAll" + clsName
						+ "s(org.springframework.ui.Model)}.");
				ps.println("	 */");
				ps.println("	@Test");
				ps.println("	public void testGetAll" + clsName + "s() throws Exception {");
				ps.println("		List<" + clsName + "> list = new ArrayList<>();");
				ps.println("		" + clsName + " o = get" + clsName + "(1" + idMod + ");");
				ps.println("		list.add(o);");
				ps.println("");
				ps.println("		given(" + fieldName + "Services.listAll()).willReturn(list);");
				ps.println("");
				ps.println("		ResultActions ra = getAsAdmin(\"/" + fieldName + "s\");");
				ps.println("		contentContainsMarkup(ra,\"<h1>\" + getMsg(\"class." + clsName
						+ "\") + \" \" + getMsg(\"edit.list\") + \"</h1>\");");
				it = set.iterator();
				while (it.hasNext()) {
					String name = it.next();
					if (PKEY_INFO.equals(name))
						continue;
					ColInfo info = (ColInfo) namList.get(name);
					if (info.isString()) {
						int endIndex = info.getLength();
						if (endIndex > 0) {
							ps.println("		contentContainsMarkup(ra,getTestString(" + endIndex + "));");
						}
					}
					if (!info.getVName().endsWith("link"))
						ps.println("		contentContainsMarkup(ra,getMsg(\"" + clsName + "." + info.getVName()
								+ "\"));");
				}
				ps.println("	}");
				ps.println("");
				ps.println("	/**");
				ps.println("	 * Test method for");
				ps.println("	 * {@link " + basePkg + ".controller." + clsName + "Controller#showNew" + clsName
						+ "Page(org.springframework.ui.Model)}.");
				ps.println("	 * ");
				ps.println("	 * @throws Exception");
				ps.println("	 */");
				ps.println("	@Test");
				ps.println("	public void testShowNew" + clsName + "Page() throws Exception {");
				ps.println("		ResultActions ra = getAsAdmin(\"/" + fieldName + "s/new\");");
				ps.println("		contentContainsMarkup(ra,\"<h1>\" + getMsg(\"edit.new\") + \" \" + getMsg(\"class."
						+ clsName + "\") + \"</h1>\");");
				set = namList.keySet();
				it = set.iterator();
				while (it.hasNext()) {
					String name = it.next();
					if (PKEY_INFO.equals(name))
						continue;
					ColInfo info = (ColInfo) namList.get(name);
					ps.println("		contentContainsMarkup(ra,\"" + info.getColName() + "\");");
				}
				ps.println("	}");
				ps.println("");
				ps.println("	/**");
				ps.println("	 * Test method for");
				ps.println("	 * {@link " + basePkg + ".controller." + clsName + "Controller#save" + clsName + "("
						+ basePkg + ".entity." + clsName + ", java.lang.String)}.");
				ps.println("	 */");
				ps.println("	@Test");
				ps.println("	public void testSave" + clsName + "Cancel() throws Exception {");
				ps.println("		" + clsName + " o = get" + clsName + "(1" + idMod + ");");
				ps.println("");
				ps.println("		send(SEND_POST, \"/" + fieldName + "s/save\", \"" + fieldName
						+ "\", o, ImmutableMap.of(\"action\", \"cancel\"), ADMIN_USER,");
				ps.println("				\"/" + fieldName + "s\");");
				ps.println("	}");
				ps.println("");
				ps.println("	/**");
				ps.println("	 * Test method for");
				ps.println("	 * {@link " + basePkg + ".controller." + clsName + "Controller#save" + clsName + "("
						+ basePkg + ".entity." + clsName + ", java.lang.String)}.");
				ps.println("	 */");
				ps.println("	@Test");
				ps.println("	public void testSave" + clsName + "Save() throws Exception {");
				ps.println("		" + clsName + " o = get" + clsName + "(0" + idMod + ");");
				ps.println("");
				ps.println("		send(SEND_POST, \"/" + fieldName + "s/save\", \"" + fieldName
						+ "\", o, ImmutableMap.of(\"action\", \"save\"), ADMIN_USER,");
				ps.println("				\"/" + fieldName + "s\");");
				ps.println("	}");
				ps.println("");
				ps.println("	/**");
				ps.println("	 * Test method for");
				ps.println("	 * {@link " + basePkg + ".controller." + clsName + "Controller#showEdit" + clsName
						+ "Page(java.lang.Integer)}.");
				ps.println("	 * ");
				ps.println("	 * @throws Exception");
				ps.println("	 */");
				ps.println("	@Test");
				ps.println("	public void testShowEdit" + clsName + "Page() throws Exception {");
				ps.println("		" + clsName + " o = get" + clsName + "(1" + idMod + ");");
				ps.println("");
				ps.println("		given(" + fieldName + "Services.get(1" + idMod + ")).willReturn(o);");
				ps.println("");
				ps.println("		ResultActions ra = getAsAdmin(\"/" + fieldName + "s/edit/1\");");
				it = set.iterator();
				while (it.hasNext()) {
					String name = it.next();
					if (PKEY_INFO.equals(name))
						continue;
					ColInfo info = (ColInfo) namList.get(name);
					if (info.isString()) {
						int endIndex = info.getLength();
						if (endIndex > 0) {
							ps.println("		contentContainsMarkup(ra,o.get" + info.getGsName() + "());");
						}
					}
					ps.println("		contentContainsMarkup(ra,\"" + info.getColName() + "\");");
				}
				ps.println("	}");
				ps.println("");
				ps.println("	/**");
				ps.println("	 * Test method for");
				ps.println("	 * {@link " + basePkg + ".controller." + clsName + "Controller#delete" + clsName
						+ "(java.lang.Integer)}.");
				ps.println("	 */");
				ps.println("	@Test");
				ps.println("	public void testDelete" + clsName + "() throws Exception {");
				ps.println(
						"		getAsAdminRedirectExpected(\"/" + fieldName + "s/delete/1\",\"/" + fieldName + "s\");");
				ps.println("	}");
				ps.println("");
				ps.println("}");
				ps.println("");
				LOGGER.warn("Wrote:" + p.toString());
			} catch (Exception e) {
				LOGGER.error("failed to create " + p, e);
				p.toFile().delete();
			}
		}

	}

	private void writeObjController(String clsName, ColInfo pkinfo) {
		String pkgNam = basePkg + ".controller";
		String relPath = pkgNam.replace('.', '/');
		String outFile = "/src/main/java/" + relPath + '/' + clsName + "Controller.java";
		String fieldName = clsName.substring(0, 1).toLowerCase() + clsName.substring(1);
		Path p = createFile(outFile);
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
				ps.println("package " + pkgNam + ';');
				ps.println("");
				ps.println("import org.springframework.beans.factory.annotation.Autowired;");
				ps.println("import org.springframework.stereotype.Controller;");
				ps.println("import org.springframework.ui.Model;");
				ps.println("import org.springframework.web.bind.annotation.GetMapping;");
				ps.println("import org.springframework.web.bind.annotation.ModelAttribute;");
				ps.println("import org.springframework.web.bind.annotation.PathVariable;");
				ps.println("import org.springframework.web.bind.annotation.PostMapping;");
				ps.println("import org.springframework.web.bind.annotation.RequestMapping;");
				ps.println("import org.springframework.web.bind.annotation.RequestParam;");
				ps.println("import org.springframework.web.servlet.ModelAndView;");
				ps.println("");
				ps.println("import " + basePkg + ".entity." + clsName + ";");
				ps.println("import " + basePkg + ".service." + clsName + "Services;");
				ps.println("");
				ps.println(getClassHeader(clsName + "Controller", clsName + "Controller."));
				ps.println("@Controller");
				ps.println("@RequestMapping(\"/" + fieldName + "s\")");
				ps.println("public class " + clsName + "Controller {");
				ps.println("");
				ps.println("	@Autowired");
				ps.println("	private " + clsName + "Services " + fieldName + "Service;");
				ps.println("");
				ps.println("	@GetMapping");
				ps.println("	public String getAll" + clsName + "s(Model model) {");
				ps.println(
						"		model.addAttribute(\"" + fieldName + "s\", this." + fieldName + "Service.listAll());");
				ps.println("		return \"" + fieldName + "s\";");
				ps.println("	}");
				ps.println("");
				ps.println("	@GetMapping(\"/new\")");
				ps.println("	public String showNew" + clsName + "Page(Model model) {");
				ps.println("		" + clsName + " " + fieldName + " = new " + clsName + "();");
				ps.println("		model.addAttribute(\"" + fieldName + "\", " + fieldName + ");");
				ps.println("");
				ps.println("		return \"edit_" + fieldName + "\";");
				ps.println("	}");
				ps.println("");
				ps.println("	@PostMapping(value = \"/save\")");
				ps.println("	public String save" + clsName + "(@ModelAttribute(\"" + fieldName + "\") " + clsName
						+ " " + fieldName + ",");
				ps.println("			@RequestParam(value = \"action\", required = true) String action) {");
				ps.println("		if (action.equals(\"save\")) {");
				ps.println("			" + fieldName + "Service.save(" + fieldName + ");");
				ps.println("		}");
				ps.println("		return \"redirect:/" + fieldName + "s\";");
				ps.println("	}");
				ps.println("");
				ps.println("	@GetMapping(\"/edit/{id}\")");
				ps.println("	public ModelAndView showEdit" + clsName + "Page(@PathVariable(name = \"id\") "
						+ pkinfo.getType() + " id) {");
				ps.println("		ModelAndView mav = new ModelAndView(\"edit_" + fieldName + "\");");
				ps.println("		" + clsName + " " + fieldName + " = " + fieldName + "Service.get(id);");
				ps.println("		mav.addObject(\"" + fieldName + "\", " + fieldName + ");");
				ps.println("");
				ps.println("		return mav;");
				ps.println("	}");
				ps.println("");
				ps.println("	@GetMapping(\"/delete/{id}\")");
				ps.println("	public String delete" + clsName + "(@PathVariable(name = \"id\") " + pkinfo.getType()
						+ " id) {");
				ps.println("		" + fieldName + "Service.delete(id);");
				ps.println("		return \"redirect:/" + fieldName + "s\";");
				ps.println("	}");
				ps.println("}");
				ps.println("");
				LOGGER.warn("Wrote:" + p.toString());
			} catch (Exception e) {
				LOGGER.error("failed to create " + p, e);
				p.toFile().delete();
			}
		}
	}

	/**
	 * Write service of bean
	 * 
	 * @param clsName
	 * @param pkinfo
	 */
	private void writeService(String clsName, ColInfo pkinfo) {
		String pkgNam = basePkg + ".service";
		String relPath = pkgNam.replace('.', '/');
		String outFile = "/src/main/java/" + relPath + '/' + clsName + "Services.java";
		String fieldName = clsName.substring(0, 1).toLowerCase() + clsName.substring(1);
		Path p = createFile(outFile);
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
				ps.println("package " + pkgNam + ';');
				ps.println("");
				ps.println("import org.springframework.beans.factory.annotation.Autowired;");
				ps.println("import org.springframework.stereotype.Service;");
				ps.println("");
				ps.println("import java.util.List;");
				ps.println("");
				ps.println("import " + basePkg + ".entity." + clsName + ";");
				ps.println("import " + basePkg + ".repo." + clsName + "Repository;");
				ps.println("");
				ps.println(getClassHeader(clsName + "Services", clsName + "Services."));
				ps.println("@Service");
				ps.println("public class " + clsName + "Services {");
				ps.println("    @Autowired");
				ps.println("    private " + clsName + "Repository " + fieldName + "Repository;");
				ps.println("");
				ps.println("	public List<" + clsName + "> listAll() {");
				ps.println("		return (List<" + clsName + ">) " + fieldName + "Repository.findAll();");
				ps.println("	}");
				ps.println("	");
				ps.println("	public void save(" + clsName + " item) {");
				ps.println("		" + fieldName + "Repository.save(item);");
				ps.println("	}");
				ps.println("	");
				ps.println("	public " + clsName + " get(" + pkinfo.getType() + " id) {");
				ps.println("		return " + fieldName + "Repository.findById(id).get();");
				ps.println("	}");
				ps.println("	");
				ps.println("	public void delete(" + pkinfo.getType() + " id) {");
				ps.println("		" + fieldName + "Repository.deleteById(id);");
				ps.println("	}");
				ps.println("");
				ps.println("}");
				ps.println("");
				LOGGER.warn("Wrote:" + p.toString());
			} catch (Exception e) {
				LOGGER.error("failed to create " + p, e);
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
		Path p = createFile("/src/main/resources/application.properties");
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

					dbUrl = "jdbc:sqlite:" + outPath.toAbsolutePath().toString().replace('\\', '/') + "/" + bundleName
							+ "DB.sqlite";

				}
				ps.println("spring.datasource.url=" + dbUrl);

				String user = Utils.getProp(bundle, "db.user", null);
				if (!StringUtils.isBlank(user)) {
					ps.println("spring.datasource.username=" + user);
					ps.println("spring.datasource.password=" + Utils.getProp(bundle, "db.password", ""));
				}
				ps.println("");
				ps.println("logging.level.root=INFO");
				LOGGER.warn("Wrote:" + p.toString());
			} catch (Exception e) {
				LOGGER.error("failed to create " + p, e);
				p.toFile().delete();
			}
		}
	}

	/**
	 * write pojo class
	 * 
	 * @param viewName  String
	 * @param className
	 * @param namList
	 * @param create    SQL statement if available
	 * @throws FileNotFoundException
	 */
	private void writeBean(String viewName, String className, TreeMap<String, ColInfo> namList, String create) {
		String pkgNam = basePkg + ".entity";
		String relPath = pkgNam.replace('.', '/');
		String outFile = "/src/main/java/" + relPath + '/' + className + ".java";
		Path p = createFile(outFile);
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
				ps.println("package " + pkgNam + ';');
				ps.println("");
				ps.println("import java.io.Serializable;");
				ps.println("import javax.persistence.*;");
				Set<String> set = namList.keySet();
				Iterator<String> it = set.iterator();
				Set<String> imports = new TreeSet<String>();
				while (it.hasNext()) {
					String name = it.next();
					if (PKEY_INFO.equals(name))
						continue;
					ColInfo info = (ColInfo) namList.get(name);
					if (!StringUtils.isBlank(info.getImportStr()))
						imports.add(info.getImportStr());
				}
				for (String s : imports) {
					ps.println(s);
				}
				ps.println("");
				ps.println(
						getClassHeader(viewName + " Bean", "Class for holding data from the " + viewName + " table."));
				ps.println("@Entity");
				ps.println("@Table(name = \"`" + viewName + "`\")");
				ps.println("public class " + className + " implements Serializable {");
				ps.println("	private static final long serialVersionUID = 1L;");
				ps.println("");
				it = set.iterator();
				while (it.hasNext()) {
					String name = it.next();
					if (PKEY_INFO.equals(name))
						continue;
					ColInfo info = (ColInfo) namList.get(name);
					if (info.isTimestamp())
						ps.println("    @DateTimeFormat(pattern = \"yyyy-mm-dd hh:mm:ss\")");
					if (info.isDate())
						ps.println("    @DateTimeFormat(pattern = \"yyyy-mm-dd\")");

					if (info.isJsonIgnore())
						ps.println("    @JsonIgnore");
					if (info.isPk()) {
						ps.println("    @Id");
						if (info.getStype() == Types.INTEGER || info.getStype() == Types.BIGINT)
							ps.println("    @GeneratedValue(strategy = GenerationType.IDENTITY)");
					}
					StringBuilder sb = new StringBuilder("	@Column(name=\"" + info.getColName() + "\"");
					if (info.isUnique())
						sb.append(", unique=false");
					if (info.isRequired())
						sb.append(", nullable=false");
					if ("String".equals(info.getType()) && info.getLength() > 0) {
						sb.append(", length=" + info.getLength());
					}
					sb.append(")");
					ps.println(sb.toString());
					ps.println("	private " + info.getType() + ' ' + info.getVName() + ';');
				}

				ps.println("");
				ps.println("	/**");
				ps.println("	 * Basic constructor");
				ps.println("	 */");
				ps.println("	public " + className + "() {");
				ps.println("	}");
				ps.println("");
				ps.println("	/**");
				ps.println("	 * Full constructor");
				ps.println("	 *");
				ps.println("	 */");
				StringBuilder sb = new StringBuilder("	public " + className + "(");
				set = namList.keySet();
				it = set.iterator();
				boolean addCom = false;
				while (it.hasNext()) {
					String name = it.next();
					if (PKEY_INFO.equals(name))
						continue;
					ColInfo info = (ColInfo) namList.get(name);
					if (addCom) {
						sb.append(", ");
					} else {
						addCom = true;
					}
					sb.append(info.getType() + ' ' + info.getVName());
				}
				sb.append(") {");
				ps.println(sb.toString());
				set = namList.keySet();
				it = set.iterator();
				while (it.hasNext()) {
					String name = it.next();
					if (PKEY_INFO.equals(name))
						continue;
					ColInfo info = (ColInfo) namList.get(name);
					ps.println("		this." + info.getVName() + " = " + info.getVName() + ";");
				}
				ps.println("	}");
				writeBeanGetSets(ps, namList);
				LOGGER.debug("Done with get/sets writing");
				if (beanToString) {
					ps.println("	/**");
					ps.println("	 * Returns a String showing the values of this bean - mainly for debuging");
					ps.println("	 *");
					ps.println("	 * @return String");
					ps.println("	 */");
					ps.println("	public String toString(){");
					ps.println("		StringBuffer sb = new StringBuffer();");
					set = namList.keySet();
					it = set.iterator();
					while (it.hasNext()) {
						String name = it.next();
						if (PKEY_INFO.equals(name))
							continue;
						ColInfo info = (ColInfo) namList.get(name);
						ps.println(
								"		sb.append(\"" + info.getVName() + "= \" + " + info.getVName() + "+\'\\n\');");
					}
					ps.println("		return sb.toString();");
					ps.println("	}");
				}

				ps.println("");

				if (beanEquals) {
					ps.println("	/**");
					ps.println("	 * Mainly for mock testing");
					ps.println("	 *");
					ps.println("	 * @return boolean");
					ps.println("	 */");
					ps.println("	@Override");
					ps.println("	public boolean equals(Object obj) {");
					ps.println("		if (this == obj)");
					ps.println("			return true;");
					ps.println("		if (obj == null)");
					ps.println("			return false;");
					ps.println("		if (getClass() != obj.getClass())");
					ps.println("			return false;");
					ps.println("		" + className + " other = (" + className + ") obj;");
					ps.println("");
					set = namList.keySet();
					it = set.iterator();
					while (it.hasNext()) {
						String name = it.next();
						if (PKEY_INFO.equals(name))
							continue;
						ColInfo info = (ColInfo) namList.get(name);
						ps.println("		if (get" + info.getGsName() + "() == null) {");
						ps.println("			if (other.get" + info.getGsName() + "() != null)");
						ps.println("				return false;");
						ps.println("		} else if (!get" + info.getGsName() + "().equals(other.get"
								+ info.getGsName() + "()))");
						ps.println("			return false;");
						ps.println("");
					}
					ps.println("		return true;");
					ps.println("	}");
				}

				ps.println("");
				ps.println("}");
				LOGGER.warn("Wrote:" + p.toString());
			} catch (Exception e) {
				LOGGER.error("failed to create " + p, e);
				p.toFile().delete();
			}

			LOGGER.debug("Created bean" + outFile);
		}
	}

	/**
	 * Write getters and setters to pojo class file.
	 * 
	 * @param ps      PrintWriter
	 * @param namList
	 */
	private void writeBeanGetSets(PrintStream ps, TreeMap<String, ColInfo> namList) {
		Set<String> set = namList.keySet();
		Iterator<String> it = set.iterator();
		while (it.hasNext()) {
			String name = it.next();
			if (PKEY_INFO.equals(name))
				continue;
			ColInfo info = (ColInfo) namList.get(name);
			ps.println("	/**");
			ps.println("	 * returns value of the " + info.getColName() + " column of this row of data");
			ps.println("	 *");
			ps.println("	 * @return value of this column in this row");
			// TODO: add Foreign key handling
			if (!StringUtils.isBlank(info.getForeignCol())) {
				ps.println("	 * TODO: Add join for Foreign key to " + info.getForeignTable() + "."
						+ info.getForeignCol());
			}
			ps.println("	 */");
			ps.println("	public " + info.getType() + " get" + info.getGsName() + "() {");
			if ("Float".equals(info.getType())) {
				ps.println("		if (" + info.getVName() + "== null)");
				ps.println("	    	return 0.0f;");
				ps.println("		return " + info.getVName() + ".floatValue();");
			} else if ("Double".equals(info.getType())) {
				ps.println("		if (" + info.getVName() + "== null)");
				ps.println("	    	return 0;");
				ps.println("		return " + info.getVName() + ".doubleValue();");
			} else if ("BigDecimal".equals(info.getType())) {
				ps.println("		if (" + info.getVName() + "== null)");
				ps.println("	    	return BigDecimal.ZERO;");
				ps.println("		return " + info.getVName() + ";");
			} else if ("Integer".equals(info.getType())) {
				ps.println("		if (" + info.getVName() + "== null)");
				ps.println("	    	return 0;");
				ps.println("		return " + info.getVName() + ".intValue();");
			} else if ("Long".equals(info.getType())) {
				ps.println("		if (" + info.getVName() + "== null)");
				ps.println("	    	return 0l;");
				ps.println("		return " + info.getVName() + ".longValue();");
			} else {
				ps.println("		return " + info.getVName() + ';');
			}
			ps.println("	}");
			ps.println("");

			ps.println("	/**");
			ps.println("	 * sets value of the " + info.getColName() + " column of this row of data");
			ps.println("	 * default value for this field set by the DB is " + info.getDefaultVal());
			if (info.getConstraint() != null) {
				ps.println("	 * Note this field has a constraint named " + info.getConstraint());
			}
			if (info.isPk()) {
				ps.println("	 * This is the primary key for this table");
			}
			if ("String".equals(info.getType()) && info.getLength() > 0) {
				ps.println("	 * This field has a max length of " + info.getLength()
						+ ", longer strings will be truncated");
			}
			ps.println("	 */");
			ps.println("	public void set" + info.getGsName() + '(' + info.getType() + " newVal) {");
			if ("String".equals(info.getType()) && info.getLength() > 0) {
				ps.println("		if (" + info.getVName() + " != null && " + info.getVName() + ".length() > "
						+ info.getLength() + "){");
				ps.println("			" + info.getVName() + " = newVal.substring(0," + (info.getLength() - 1) + ");");
				ps.println("		} else {");
				ps.println("	    	" + info.getVName() + " = newVal;");
				ps.println("		}");
			} else {
				ps.println("		" + info.getVName() + " = newVal;");
			}
			ps.println("	}");
			ps.println("");
		}

	}

	public static void printUsage(String error, Exception e) {
		if (error != null)
			System.err.println(error);
		System.err.println("USAGE: Genspring [options] [table names]");
		System.err.println("Where options are:");
		System.err.println(
				"-double = use Double instead of BigDecimal for entities beans. Can be overridden in properties file.");
		System.err.println(
				"-toString = generate toString() methods for entities beans. Can be overridden in properties file.");
		System.err.println(
				"-beanEquals = generate equals() methods for entities beans. Can be overridden in properties file.");
		System.err.println("");
		System.err.println("if table names not given then runs on all tables in DB.");
		System.err.println("");
		System.err.println("Note: be sure to set properties in resources/genSpring.properties before running.");

		if (e != null)
			LOGGER.error(error, e);

		System.exit(1);
	}

	public List<String> getTablesNames(Db db) throws SQLException {
		List<String> tableNames = new ArrayList<String>();
		String dbName = db.getDbName();

		String query = "SHOW TABLES"; // mySQL
		if (db.getDbUrl().indexOf("sqlserver") > -1) {
			query = "SELECT NAME,INFO FROM sysobjects WHERE type= 'U'";
		} else if (db.getDbUrl().indexOf("sqlite") > -1) {
			query = "SELECT name FROM sqlite_master WHERE type='table';";
		}
		Connection conn = db.getConnection(PROPKEY + ".main()");
		Statement stmt = conn.createStatement();
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("query=" + query);
		}
		ResultSet rs = stmt.executeQuery(query);
		try {
			int rowcount = 0;
			if (rs.last()) {
				rowcount = rs.getRow();
				rs.beforeFirst(); // not rs.first() because the rs.next() below will move on, missing the first
									// element
			}
			LOGGER.info("found " + rowcount + " tables");
		} catch (Exception e) {
			LOGGER.warn("Could not get table count  ", e);
		}
		while (rs.next()) {
			try {
				// TODO: get these table filters from a props file.
				if (db.getDbUrl().indexOf("mysql") > -1) {
					String name = rs.getString("Tables_in_" + dbName);
					if (!name.startsWith("gl_") && !name.startsWith("hostinfo")) {
						tableNames.add(rs.getString("Tables_in_" + dbName));
					}
				} else if (db.getDbUrl().indexOf("sqlite") > -1) {
					String name = rs.getString("name").toLowerCase();
					if (!name.startsWith("old_") && !name.startsWith("sqlite_sequence") && !name.equals("providers")
							&& !name.equals("account") && !name.equals("hibernate_sequence")) {
						tableNames.add(name);
					}
				} else if (db.getDbUrl().indexOf("sqlserver") > -1) {
					int info = rs.getInt("INFO"); // check for hidden or
													// bad table
					String name = rs.getString("name").toLowerCase();
					// filter tables that don't show in enterprise
					// manager but do here
					LOGGER.info("read:" + name + ':' + info);
					if (info > 0 && !name.startsWith("dtproperties")) {
						tableNames.add(name);
					}
				}
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
			GenSpring obj = new GenSpring();
			List<String> tableNames = new ArrayList<String>();
			for (; i < args.length; i++) {
				if (args[i].length() > 0 && args[i].charAt(0) == '-') {
					if ("-h".equals(args[i])) {
						printUsage(null, null);
					}
				} else if (args[i].length() > 0 && args[i].charAt(0) == '-') {
					if ("-beanEquals".equals(args[i])) {
						obj.beanEquals = true;
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

		LOGGER.info("Done");
	}
}
