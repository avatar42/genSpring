package com.dea42.build;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
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
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
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
	public static final String genSpringVersion = "0.4.1";
	public static String ACCOUNT_CLASS = "Account";
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

	protected long TEST_USER_ID;
	protected String TEST_USER;
	protected String TEST_PASS;
	protected String TEST_ROLE;

	protected long ADMIN_USER_ID;
	protected String ADMIN_USER;
	protected String ADMIN_PASS;
	protected String ADMIN_ROLE;

	private boolean useDouble = false;
	private boolean beanToString = false;
	private static final boolean beanEquals = true;
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
	private List<String> filteredTables;
	private String colCreated;
	private String colLastMod;

	// TODO: make this configurable and add to PasswordConstraintValidator
	private int maxPassLen = 30;

	public static final String PKEY_INFO = "PRIMARY_KEY_INFO";

	public GenSpring() throws IOException {
		this(PROPKEY);
	}

	public GenSpring(String bundleName) throws IOException {
		this.bundleName = bundleName;
		initVars();
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
					LOGGER.debug(key + "=" + val);
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
				for (String className : set) {
					String fieldName = className.substring(0, 1).toLowerCase() + className.substring(1);
					ps.println("    	<a th:href=\"@{/" + fieldName + "s}\">" + className + "</a><br>");
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
				for (String className : set) {
					String fieldName = className.substring(0, 1).toLowerCase() + className.substring(1);
					ps.println("    	<a th:href=\"@{/api/" + fieldName + "s}\">" + className + "</a><br>");
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
				for (String className : set) {
					String li = makeAdminOnly(ps, classHasUserFields(className), "li");
					String fieldName = className.substring(0, 1).toLowerCase() + className.substring(1);
					ps.println("								<" + li + " th:classappend=\"${module == '" + fieldName
							+ "' ? 'active' : ''}\">");
					ps.println("    								<a id=\"guiItem" + className + "\" th:href=\"@{/"
							+ fieldName + "s}\" th:text=\"#{class." + className + "}\"></a></li>");
				}
				ps.println("							</ul></li>");
				ps.println("						<li id=\"restMenu\" class=\"dropdown\"><a href=\"#\"");
				ps.println("							class=\"dropdown-toggle\" data-toggle=\"dropdown\"><span");
				ps.println(
						"								th:text=\"#{header.restApi}\"></span> <b class=\"caret\"></b></a>");
				ps.println("							<ul class=\"dropdown-menu\">");
				for (String className : set) {
					String li = makeAdminOnly(ps, classHasUserFields(className), "li");
					String fieldName = className.substring(0, 1).toLowerCase() + className.substring(1);
					ps.println("								<" + li + " th:classappend=\"${module == '" + fieldName
							+ "' ? 'active' : ''}\">");
					ps.println(
							"    								<a id=\"apiItem" + className + "\" th:href=\"@{/api/"
									+ fieldName + "s}\" th:text=\"#{class." + className + "}\"></a></li>");
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

//    static String inputTemplate = "java_example.vm";
//    static String className = "VelocityExample";
//    static String message = "Hello World!";
//    static String outputFile = className + ".java";

	public void velocityGenerator(String inputTemplate, String className, String message, String outputFile)
			throws IOException {

		VelocityEngine velocityEngine = new VelocityEngine();
		velocityEngine.init();

		VelocityContext context = new VelocityContext();
		// replace ${className} in file with className value
		context.put("className", className);
		context.put("message", message);

		Writer writer = new FileWriter(new File(outputFile));
		Velocity.mergeTemplate(inputTemplate, "UTF-8", context, writer);
		writer.flush();
		writer.close();

		System.out.println("Generated " + outputFile);
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
			 * Copy file into new tree converting package / paths as needed TODO: change to
			 * use velocityGenerator()
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
		// overridding because testing requires beans to have equals to mock services.
//		beanEquals = Utils.getProp(bundle, PROPKEY + ".beanEquals", beanEquals);
		useDouble = Utils.getProp(bundle, PROPKEY + ".useDouble", useDouble);
		colCreated = Utils.tabToStr(renames, (String) Utils.getProp(bundle, "col.created", null));
		colLastMod = Utils.tabToStr(renames, (String) Utils.getProp(bundle, "col.lastMod", null));

		filteredTables = Utils.getPropList(bundle, PROPKEY + ".filteredTables");
		// SQLite tables to always ignore
		try {
			filteredTables.add("hibernate_sequence");
			filteredTables.add("sqlite_sequence");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		srcPkg = srcGroupId + '.' + srcArtifactId;
		srcPath = srcPkg.replace('.', '/');
		File outDir = Utils.getPath(baseDir).toFile();
		if (!outDir.exists()) {
			if (!outDir.mkdirs()) {
				throw new IOException("Could not create output dir:" + baseDir);
			}
		}

		TEST_USER_ID = Utils.getProp(bundle, "default.userid", 1l);
		TEST_USER = Utils.getProp(bundle, "default.user", "user@dea42.com");
		TEST_PASS = Utils.getProp(bundle, "default.userpass", "ChangeMe");
		TEST_ROLE = Sheets2DB.ROLE_PREFIX + Utils.getProp(bundle, "default.userrole", "USER");
		ADMIN_USER_ID = Utils.getProp(bundle, "default.adminid", 2l);
		ADMIN_USER = Utils.getProp(bundle, "default.admin", "admin@dea42.com");
		ADMIN_PASS = Utils.getProp(bundle, "default.adminpass", "ChangeMe");
		ADMIN_ROLE = Sheets2DB.ROLE_PREFIX + Utils.getProp(bundle, "default.adminrole", "ADMIN");

	}

	/**
	 * Generate folder structure and project level files
	 * 
	 * @param tableNames
	 * @throws Exception
	 */
	public void writeProject(List<String> tableNames) throws Exception {

		copyCommon();

		writeAppProps();
		writeTestProps();

		Map<String, Map<String, ColInfo>> colsInfo = new HashMap<String, Map<String, ColInfo>>();
		for (String tableName : tableNames) {
			String className = Utils.tabToStr(renames, tableName);
			colsInfo.put(className, gatherTableInfo(tableName));
		}

		genTableMaintFiles(tableNames, colsInfo);

		writeMockBase(colsInfo);
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

		Db db = new Db(PROPKEY + ".genFiles()", bundleName, Utils.getProp(bundle, PROPKEY + ".outdir", "."));
		Connection conn = db.getConnection(PROPKEY + ".genFiles()");

		LOGGER.debug("tableName:" + tableName);
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
			if (LOGGER.isDebugEnabled()) {
				for (int i = 1; i <= rm.getColumnCount(); i++) {
					String columnName = rm.getColumnName(i);
					LOGGER.debug(columnName + "='" + rs.getString(columnName) + "'");
				}
			}
			catalog = rs.getString(TABLE_CAT);
			ColInfo colInfo = new ColInfo();
			colInfo.setfNum(rs.getInt(ORDINAL_POSITION) + 1);
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
					LOGGER.error("Failed checking IS_AUTOINCREMENT for:" + columnName, e);
				}
			}
			colInfo.setRequired(rs.getInt(NULLABLE) == 0);
			int stype = rs.getInt(DATA_TYPE);
			if (db.isSQLite()) {
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
				if (db.isSQLite()) {
					tmp = rs.getString(TYPE_NAME);
					int s = tmp.indexOf('(');
					if (s > -1) {
						tmp = tmp.substring(s + 1, tmp.length() - 1);
					} else {
						tmp = null;
						LOGGER.warn(columnName + " def has not len: " + tmp);
					}
					// MySQL seems to have issue the reporting varchar lens>100
				} else if (db.isMySQL()) {
					tmp = rs.getString(COLUMN_SIZE);
					if ("100".equals(tmp)) {
						tmp = rs.getString(REMARKS);
						if (tmp != null && tmp.startsWith("len=")) {
							tmp = tmp.substring(4);
							LOGGER.warn(columnName + " Using REMARKS: " + tmp);
						} else {
							tmp = null;
						}
					} else {
						LOGGER.warn(columnName + " Using COLUMN_SIZE: " + tmp);
					}
				} else { // most others
					tmp = rs.getString(COLUMN_SIZE);
					LOGGER.warn(columnName + " Using COLUMN_SIZE: " + tmp);
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
					colInfo.setImportStr("import java.math.BigDecimal;");
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
				colInfo.setImportStr("import java.util.Date;" + System.lineSeparator()
						+ "import org.springframework.format.annotation.DateTimeFormat;");
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

			if (columnName.equalsIgnoreCase(colCreated))
				colInfo.setCreated(true);

			if (columnName.equalsIgnoreCase(colLastMod))
				colInfo.setLastMod(true);

			columnName = Utils.tabToStr(renames, columnName);
			colInfo.setVName(columnName.substring(0, 1).toLowerCase() + columnName.substring(1));
			colInfo.setMsgKey(className + "." + colInfo.getVName());
			colInfo.setGsName(columnName);

			if (colInfo.getColName() != null && colInfo.getType() != null && colInfo.getType().length() > 0) {
				LOGGER.info("storing:" + colInfo);
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
			LOGGER.error(tableName + " does not have a primary key. Using " + pkCol);
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
			writeRepo(tableName, colsInfo);
			writeService(tableName, colsInfo);
			writeListPage(tableName, colsInfo);
			writeObjController(tableName, colsInfo);
			writeObjControllerTest(tableName, colsInfo);
			writeEditPage(tableName, colsInfo);
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
				LOGGER.warn("Wrote:" + p.toString());
			} else {
				LOGGER.warn(p.toString() + " up to date so will skip.");
			}
		} catch (Exception e) {
			LOGGER.error("failed to update " + p, e);
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
				ps.println("import org.junit.Test;");
				ps.println("import org.junit.runner.RunWith;");
				ps.println("import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;");
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
						LOGGER.error("No PK found for " + className);
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
					ps.println("");
					ps.println("		given(" + fieldName + "Services.listAll()).willReturn(list);");
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
	 * @param className or key to be used for the title
	 * @param pageType  it null className is assumed to be a message key. Should
	 *                  match property in messages.properties as in listView for
	 *                  edit.listView
	 * @return
	 */
	private String htmlHeader(String className, String pageType) {
		StringBuilder sb = new StringBuilder();

		sb.append("<!DOCTYPE html>" + System.lineSeparator());
		sb.append("<html lang=\"en\" xmlns:th=\"http://www.thymeleaf.org\">" + System.lineSeparator());
		sb.append("<head>" + System.lineSeparator() + "<meta charset=\"UTF-8\" />" + System.lineSeparator());
		if (pageType == null)
			sb.append("<title th:text=\"#{" + className + "}\"></title>" + System.lineSeparator());
		else
			sb.append("<title th:text=\"#{edit." + pageType + "} + ' ' + #{class." + className + "}\"></title>"
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
		String outFile = "/src/main/java/" + relPath + '/' + className + "Repository.java";
		Path p = createFile(outFile);
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
				ps.println("package " + pkgNam + ';');
				ps.println("");
				ps.println("import org.springframework.data.jpa.repository.JpaRepository;");
				ps.println("import org.springframework.stereotype.Repository;");
				ps.println("import " + basePkg + ".entity." + className + ";");
				ps.println("");
				ps.println(
						getClassHeader(className + "Repository", "Class for the " + className + " Repository.", null));
				ps.println("@Repository");
				ps.println("public interface " + className + "Repository extends JpaRepository<" + className + ", "
						+ pkinfo.getType() + ">{");
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
				LOGGER.warn("Wrote:" + p.toString());
			} catch (Exception e) {
				LOGGER.error("failed to create " + p, e);
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
//		String unless = "				<div th:unless=\"${" + fieldName + " != null}\"></div>";

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
		String outFile = "/src/main/resources/templates/edit_" + fieldName + ".html";
		Path p = createFile(outFile);
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
				ps.println(htmlHeader(className, "edit"));
				ps.println("	<div class=\"container\">");
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
				ps.println("				<legend th:if=\"${" + fieldName + "Form." + pkinfo.getVName() + " == 0}\"");
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
				ps.println("	</div>");
				ps.println("");
				ps.println(htmlFooter());
				LOGGER.warn("Wrote:" + p.toString());
			} catch (Exception e) {
				LOGGER.error("failed to create " + p, e);
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
	 * @throws Exception
	 */
	private void writeListHeaders(PrintStream ps, Set<String> processed, String tableName,
			Map<String, Map<String, ColInfo>> colsInfo) throws Exception {
		String className = Utils.tabToStr(renames, tableName);
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
					writeListHeaders(ps, processed, info.getForeignTable(), colsInfo);
				} else {
					String th = makeAdminOnly(ps, ACCOUNT_CLASS.equals(className) || info.isAdminOnly(), "th");
					if (colNameToInfoMap.containsKey(key + "link")) {
						ps.println("            <" + th + " th:text=\"#{" + info.getMsgKey() + "}\">"
								+ info.getColName() + "</th>");
						processed.add(key);
						processed.add(key + "link");
					} else if (key.endsWith("link")
							&& colNameToInfoMap.containsKey(key.substring(0, key.length() - 4))) {
						key = key.substring(0, key.length() - 4);
						info = (ColInfo) colNameToInfoMap.get(key);
						ps.println("            <" + th + " th:text=\"#{" + info.getMsgKey() + "}\">"
								+ info.getColName() + "</th>");
						processed.add(key);
						processed.add(key + "link");
					} else {
						ps.println("            <" + th + " th:text=\"#{" + info.getMsgKey() + "}\">"
								+ info.getColName() + "</th>");
						processed.add(key);
					}
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
		String outFile = "/src/main/resources/templates/" + fieldName + "s.html";

		Path p = createFile(outFile);
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
				Set<String> processed = new HashSet<String>();
				ps.println(htmlHeader(className, "listView"));
				ps.println("	<div class=\"container\">");
				ps.println("		<h1 th:text=\"#{class." + className + "} + ' ' + #{edit.list}\"></h1>");
				ps.println("		<a th:href=\"@{/" + fieldName + "s/new}\" th:text=\"#{edit.new} + ' ' + #{class."
						+ className + "}\"></a> <br /><br />");
				ps.println("");
				ps.println("	    <table>");
				ps.println("	        <tr>");
				writeListHeaders(ps, processed, tableName, colsInfo);
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

	/**
	 * Write the base class used by tests the employ mock object. TODO: replace with
	 * velocity template
	 * 
	 * @param set
	 */
	private void writeMockBase(Map<String, Map<String, ColInfo>> colsInfo) {
		Set<String> set = colsInfo.keySet();
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
				ps.println("import " + basePkg + ".service.UserServices;");
				ps.println("import " + basePkg + ".repo.UserRepository;");
				if (!set.contains(ACCOUNT_CLASS)) {
					ps.println("import " + basePkg + ".service." + ACCOUNT_CLASS + "Services;");
					ps.println("import " + basePkg + ".repo." + ACCOUNT_CLASS + "Repository;");
				}
				for (String className : set) {
					ps.println("import " + basePkg + ".repo." + className + "Repository;");
					ps.println("import " + basePkg + ".service." + className + "Services;");
				}
				ps.println("");
				ps.println("import " + basePkg + ".utils.Message;");
				ps.println("import " + basePkg + ".utils.Utils;");
				ps.println("");
				ps.println(getClassHeader("MockBase", "The base class for mock testing.", null));
				ps.println("public class MockBase extends UnitBase {");
				ps.println("    @MockBean");
				ps.println("    protected UserServices<?> userServices;");
				ps.println("    @MockBean");
				ps.println("    protected UserRepository userRepository;");
				ps.println("");
				if (!set.contains(ACCOUNT_CLASS)) {
					ps.println("    @MockBean");
					ps.println("    protected " + ACCOUNT_CLASS + "Services accountServices;");
					ps.println("    @MockBean");
					ps.println("    protected " + ACCOUNT_CLASS + "Repository accountRepository;");
				}
				for (String className : set) {
					String fieldName = className.substring(0, 1).toLowerCase() + className.substring(1);
					ps.println("    @MockBean");
					ps.println("    protected " + className + "Services " + fieldName + "Services;");
					ps.println("    @MockBean");
					ps.println("    protected " + className + "Repository " + fieldName + "Repository;");
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
				ps.println("			try {");
				ps.println(
						"				result.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(redirectedUrl));");
				ps.println("			} catch (Throwable e) {");
				ps.println(
						"				LOGGER.error(\"Instead of redirect got:\" + result.andReturn().getResponse().getContentAsString());");
				ps.println("				throw e;");
				ps.println("			}");
				ps.println("");
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
				ps.println("		contentContainsKey(result, \"app.name\");");
				ps.println("		// GUI menu");
				ps.println("		contentContainsKey(result, \"header.gui\");");
				for (String className : set) {
					if (classHasUserFields(className)) {
						ps.println("		if (\"" + ADMIN_USER + "\".equals(user)) ");
						ps.println("			contentContainsKey(result, \"class." + className + "\", false);");
					} else {
						ps.println("		contentContainsKey(result, \"class." + className + "\", false);");
					}
				}
				ps.println("// REST menu");
				ps.println("		contentContainsKey(result, \"header.restApi\");");
				for (String className : set) {
					String fieldName = className.substring(0, 1).toLowerCase() + className.substring(1);
					if (classHasUserFields(className)) {
						ps.println("		if (\"" + ADMIN_USER + "\".equals(user)) ");
						ps.println("			contentContainsMarkup(result, \"/api/" + fieldName + "s\", false);");
					} else {
						ps.println("		contentContainsMarkup(result, \"/api/" + fieldName + "s\", false);");
					}
				}
				ps.println("// Login / out");
				ps.println("		contentContainsKey(result, \"lang.en\");");
				ps.println("		contentContainsKey(result, \"lang.fr\");");
				ps.println("		contentContainsKey(result, \"lang.de\");");
				ps.println("");
				ps.println("		if (user == null)");
				ps.println("			contentContainsKey(result, \"signin.signin\");");
				ps.println("		else");
				ps.println("			contentContainsKey(result, \"signin.logout\");");
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
				ps.println("	public void contentContainsKey(ResultActions result, String key, String... args) {");
				ps.println("		contentContainsKey(result, key, false, args);");
				ps.println("	}");
				ps.println("");
				ps.println("	public void contentNotContainsKey(ResultActions result, String key, String... args) {");
				ps.println("		contentContainsKey(result, key, true, args);");
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
						"	public void contentContainsKey(ResultActions result, String key, boolean failIfExists, String... args) {");
				ps.println(
						"		String expectedText = Utils.getProp(getMsgBundle(), key, \"??\" + key + \"??\", args);");
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
		String outFile = "/src/test/java/" + relPath + '/' + className + "ControllerTest.java";
		String fieldName = className.substring(0, 1).toLowerCase() + className.substring(1);
		Path p = createFile(outFile);
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
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
				ps.println("import " + basePkg + ".entity." + className + ";");
				ps.println("import " + basePkg + ".form." + className + "Form;");
				ps.println("");
				ps.println(getClassHeader(className + "ControllerTest", className + "Controller.", null));
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
				ps.println("		given(" + fieldName + "Services.listAll()).willReturn(list);");
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
				ps.println("		LOGGER.debug(form.toString());");
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
				LOGGER.warn("Wrote:" + p.toString());
			} catch (Exception e) {
				LOGGER.error("failed to create " + p, e);
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
		String outFile = "/src/main/java/" + relPath + '/' + className + "Controller.java";
		String fieldName = className.substring(0, 1).toLowerCase() + className.substring(1);
		Path p = createFile(outFile);
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
				ps.println("package " + pkgNam + ';');
				ps.println("");
				ps.println("import javax.validation.Valid;");
				ps.println("import org.slf4j.Logger;");
				ps.println("import org.slf4j.LoggerFactory;");
				ps.println("import org.springframework.beans.factory.annotation.Autowired;");
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
				ps.println("import java.util.Date;");
				ps.println("");
				ps.println("import " + basePkg + ".entity." + className + ";");
				ps.println("import " + basePkg + ".form." + className + "Form;");
				ps.println("import " + basePkg + ".service." + className + "Services;");
				ps.println("import " + basePkg + ".utils.MessageHelper;");
				ps.println("import " + basePkg + ".utils.Utils;");
				ps.println("");
				ps.println(getClassHeader(className + "Controller", className + "Controller.", null));
				ps.println("@Controller");
				ps.println("@RequestMapping(\"/" + fieldName + "s\")");
				ps.println("public class " + className + "Controller {");
				ps.println("	private static final Logger LOGGER = LoggerFactory.getLogger(" + className
						+ "Controller.class.getName());");
				ps.println("");
				ps.println("	@Autowired");
				ps.println("	private " + className + "Services " + fieldName + "Service;");
				ps.println("");
				ps.println("	@GetMapping");
				ps.println("	public String getAll" + className + "s(Model model) {");
				ps.println(
						"		model.addAttribute(\"" + fieldName + "s\", this." + fieldName + "Service.listAll());");
				ps.println("		return \"" + fieldName + "s\";");
				ps.println("	}");
				ps.println("");
				ps.println("	@GetMapping(\"/new\")");
				ps.println("	public String showNew" + className + "Page(Model model,");
				ps.println(
						"			@RequestHeader(value = \"X-Requested-With\", required = false) String requestedWith) {");
				ps.println("		model.addAttribute(new " + className + "Form());");
				ps.println("		if (Utils.isAjaxRequest(requestedWith)) {");
				ps.println("			return \"edit_" + fieldName + "\".concat(\" :: " + fieldName + "Form\");");
				ps.println("		}");
				ps.println("");
				ps.println("		return \"edit_" + fieldName + "\";");
				ps.println("	}");
				ps.println("");
				ps.println("	@PostMapping(value = \"/save\")");
				ps.println("	public String save" + className + "(@Valid @ModelAttribute " + className
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
				ps.println("				LOGGER.error(\"Failed saving:\" + form, e);");
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
				ps.println("	public ModelAndView showEdit" + className + "Page(@PathVariable(name = \"id\") "
						+ pkinfo.getType() + " id) {");
				ps.println("		ModelAndView mav = new ModelAndView(\"edit_" + fieldName + "\");");
				ps.println("		" + className + " " + fieldName + " = " + fieldName + "Service.get(id);");
				ps.println("		mav.addObject(\"" + fieldName + "Form\", " + className + "Form.getInstance("
						+ fieldName + "));");
				ps.println("");
				ps.println("		return mav;");
				ps.println("	}");
				ps.println("");
				ps.println("	@GetMapping(\"/delete/{id}\")");
				ps.println("	public String delete" + className + "(@PathVariable(name = \"id\") " + pkinfo.getType()
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
			if (!StringUtils.isBlank(info.getImportStr()))
				imports.add(info.getImportStr());
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
		String outFile = "/src/main/java/" + relPath + '/' + className + "Services.java";
		String fieldName = className.substring(0, 1).toLowerCase() + className.substring(1);
		Path p = createFile(outFile);
		if (p != null) {
			boolean hasPasswordField = false;
			for (ColInfo c : colNameToInfoMap.values()) {
				if (c.isPassword())
					hasPasswordField = true;
			}
			try (PrintStream ps = new PrintStream(p.toFile())) {
				ps.println("package " + pkgNam + ';');
				ps.println("");
				ps.println("import java.util.List;");
				ps.println("import java.util.Optional;");
				ps.println("import java.util.ResourceBundle;");
				ps.println("");
				ps.println("import javax.annotation.PostConstruct;");
				ps.println("");
				ps.println("import org.apache.commons.lang3.StringUtils;");
				ps.println("import org.slf4j.Logger;");
				ps.println("import org.slf4j.LoggerFactory;");
				ps.println("import org.springframework.beans.factory.annotation.Autowired;");
				ps.println("import org.springframework.stereotype.Service;");
//				ps.println("import org.springframework.transaction.annotation.Transactional;");
				ps.println("");
				addImports(ps, colNameToInfoMap, IMPORT_TYPE_SERVICE);
				ps.println("import " + basePkg + ".entity." + className + ";");
				ps.println("import " + basePkg + ".repo." + className + "Repository;");
				ps.println("import " + basePkg + ".utils.Utils;");
				ps.println("");
				ps.println(getClassHeader(className + "Services", className + "Services.", null));
				ps.println("@Service");
				if (hasPasswordField) {
					ps.println("public class " + className + "Services extends UserServices<" + className + "> {");
				} else {
					ps.println("public class " + className + "Services {");
				}
				ps.println("	private static final Logger LOGGER = LoggerFactory.getLogger(" + className
						+ "Services.class.getName());");
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
					ps.println("			LOGGER.warn(\"Resetting default users\");");
					ps.println("			String user = Utils.getProp(bundle, \"default.user\", null);");
					ps.println("			if (!StringUtils.isBlank(user)) {");
					ps.println("				" + pkinfo.getType()
							+ " id = Utils.getProp(bundle, \"default.userid\", 1" + pkinfo.getMod() + ");");
					ps.println("				String userpass = Utils.getProp(bundle, \"default.userpass\", null);");
					ps.println(
							"				String userrole = ROLE_PREFIX + Utils.getProp(bundle, \"default.userrole\", null);");
					ps.println("				" + ACCOUNT_CLASS + " a = new " + ACCOUNT_CLASS
							+ "(user, userpass, userrole);");
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
					ps.println("				" + ACCOUNT_CLASS + " a = new " + ACCOUNT_CLASS
							+ "(user, userpass, userrole);");
					ps.println("				a.setId(id);");
					ps.println("				save(a);");
					ps.println("			}");
					ps.println("		}");
					ps.println("	}");
					ps.println("");
				}
				ps.println("	public List<" + className + "> listAll() {");
				ps.println("		return (List<" + className + ">) " + fieldName + "Repository.findAll();");
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
	 * Write hashCode() and equals() methods to bean Java file
	 * 
	 * @param ps
	 * @param colNameToInfoMap
	 * @param className
	 * @param isForm           if true skip lastMod col and add password confirms as
	 *                         needed
	 */
	private void writeEquals(PrintStream ps, Map<String, ColInfo> colNameToInfoMap, String className, boolean isForm) {
		if (beanEquals) {
			ps.println("	@Override");
			ps.println("	public int hashCode() {");
			ps.println("		final int prime = 31;");
			ps.println("		int result = 1;");
			ps.println("");
			for (String key : colNameToInfoMap.keySet()) {
				if (PKEY_INFO.equals(key))
					continue;
				ColInfo info = (ColInfo) colNameToInfoMap.get(key);
				// if using mod cols and this is one leave out
				if (info.isCreated() || info.isLastMod()) {
					continue;
				}
				ps.println("		result = prime * result + ((" + info.getVName() + " == null) ? 0 : "
						+ info.getVName() + ".hashCode());");
			}
			ps.println("		return result;");
			ps.println("	}");
			ps.println("");
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
			for (String key : colNameToInfoMap.keySet()) {
				if (PKEY_INFO.equals(key))
					continue;
				ColInfo info = (ColInfo) colNameToInfoMap.get(key);
				// if using mod cols and this is one leave out
				if (info.isCreated() || info.isLastMod()) {
					continue;
				}

				ps.println("		if (get" + info.getGsName() + "() == null) {");
				ps.println("			if (other.get" + info.getGsName() + "() != null)");
				ps.println("				return false;");
				ps.println("		} else if (!get" + info.getGsName() + "().equals(other.get" + info.getGsName()
						+ "()))");
				ps.println("			return false;");
				ps.println("");
			}
			ps.println("		return true;");
			ps.println("	}");
		}

		ps.println("");

	}

	/**
	 * Add a toString() method if beanToString is set to true
	 * 
	 * @param ps
	 * @param colNameToInfoMap
	 * @param className
	 * @param isForm           if true skip lastMod col and add password confirms as
	 *                         needed
	 */
	private void writeToString(PrintStream ps, Map<String, ColInfo> colNameToInfoMap, String className,
			boolean isForm) {
		if (beanToString) {
			ps.println("	/**");
			ps.println("	 * Returns a String showing the values of this bean - mainly for debuging");
			ps.println("	 *");
			ps.println("	 * @return String");
			ps.println("	 */");
			ps.println("	public String toString(){");
			ps.println("		StringBuilder builder = new StringBuilder();");
			ps.println("		builder.append(\"" + className + " [\");");
			String comma = "";
			for (String key : colNameToInfoMap.keySet()) {
				if (PKEY_INFO.equals(key))
					continue;
				ColInfo info = (ColInfo) colNameToInfoMap.get(key);
				// if using mod cols and this is one leave out
				if (isForm && info.isLastMod()) {
					continue;
				}
				ps.println(
						"		builder.append(\"" + comma + info.getVName() + "=\").append(" + info.getVName() + ");");
				if (StringUtils.isBlank(comma))
					comma = ", ";
				if (isForm && info.isPassword()) {
					ps.println("		builder.append(\"" + comma + info.getVName() + "Confirm=\").append("
							+ info.getVName() + "Confirm);");

				}
			}
			ps.println("		builder.append(\"]\");");
			ps.println("		return builder.toString();");
			ps.println("	}");
		}

		ps.println("");

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
		String outFile = "/src/main/java/" + relPath + '/' + className + "Form.java";
		Path p = createFile(outFile);
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
				ps.println("package " + pkgNam + ';');
				ps.println("");
				ps.println("import java.io.Serializable;");
				ps.println("");
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
				ps.println("	 * Basic constructor");
				ps.println("	 */");
				ps.println("	public " + className + "Form() {");
				ps.println("	}");
				ps.println("");
				ps.println("	/**");
				ps.println("	 * Clones " + className + " obj into form");
				ps.println("	 *");
				ps.println("	 * @param obj");
				ps.println("	 */");
				ps.println("	public static " + className + "Form getInstance(" + className + " obj) {");
				ps.println("		" + className + "Form form = new " + className + "Form();");
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
						ps.println("		form.set" + info.getGsName() + "(obj.get" + info.getGsName() + "());");
					}
				}
				ps.println("		return form;");
				ps.println("	}");
				writeBeanGetSets(ps, colNameToInfoMap, true);
				LOGGER.debug("Done with get/sets writing");
				writeToString(ps, colNameToInfoMap, className, true);
				writeEquals(ps, colNameToInfoMap, className, true);
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
		String outFile = "/src/main/java/" + relPath + '/' + className + ".java";
		Path p = createFile(outFile);
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
				ps.println("");
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

				ps.println("");
				ps.println("	/**");
				ps.println("	 * Basic constructor");
				ps.println("	 */");
				ps.println("	public " + className + "() {");
				ps.println("	}");
				ps.println("");
				if (ACCOUNT_CLASS.equals(className)) {
					ps.println("	/**");
					ps.println("	 * Special constructor for sign up");
					ps.println("	 * @param email");
					ps.println("	 * @param password");
					ps.println("	 * @param role");
					ps.println("	 */");
					ps.println("	public " + ACCOUNT_CLASS + "(String email, String password, String role) {");
					ps.println("		this.email = email;");
					ps.println("		this.password = password;");
					ps.println("		this.role = role;");
					if (!StringUtils.isBlank(colCreated)) {
						ColInfo ci = (ColInfo) colNameToInfoMap.get(colCreated.toLowerCase());
						ps.println("		this." + ci.getVName() + " = " + ci.getDefaultVal() + ";");
					}

					ps.println("");
					ps.println("	}");
					ps.println("");
				}
				ps.println("	/**");
				ps.println("	 * Full constructor");
				ps.println("	 *");
				ps.println("	 */");
				StringBuilder sb = new StringBuilder("	public " + className + "(");
				boolean addCom = false;
				for (String key : colNameToInfoMap.keySet()) {
					if (PKEY_INFO.equals(key))
						continue;
					ColInfo info = (ColInfo) colNameToInfoMap.get(key);
					if (!StringUtils.isBlank(info.getForeignTable())) {
						continue;
					}
					if (!info.isLastMod()) {
						if (addCom) {
							sb.append(", ");
						} else {
							addCom = true;
						}
						sb.append(info.getType() + ' ' + info.getVName());
					}
				}
				sb.append(") {");
				ps.println(sb.toString());
				for (String key : colNameToInfoMap.keySet()) {
					if (PKEY_INFO.equals(key))
						continue;
					ColInfo info = (ColInfo) colNameToInfoMap.get(key);
					if (!StringUtils.isBlank(info.getForeignTable())) {
						continue;
					}
					if (!info.isLastMod()) {
						ps.println("		this." + info.getVName() + " = " + info.getVName() + ";");
					}
				}
				ps.println("	}");
				writeBeanGetSets(ps, colNameToInfoMap, false);
				LOGGER.debug("Done with get/sets writing");
				writeToString(ps, colNameToInfoMap, className, false);
				writeEquals(ps, colNameToInfoMap, className, false);
				ps.println("}");
				LOGGER.warn("Wrote:" + p.toString());
			} catch (Exception e) {
				LOGGER.error("failed to create " + p, e);
				p.toFile().delete();
				throw e;
			}

			LOGGER.debug("Created bean" + outFile);
		}
	}

	/**
	 * Write getters and setters to pojo class file.
	 * 
	 * @param ps               PrintWriter
	 * @param colNameToInfoMap
	 * @param isForm           adds confirm fields
	 */
	private void writeBeanGetSets(PrintStream ps, Map<String, ColInfo> colNameToInfoMap, boolean isForm) {
		for (String key : colNameToInfoMap.keySet()) {
			if (PKEY_INFO.equals(key))
				continue;
			ColInfo info = (ColInfo) colNameToInfoMap.get(key);
			ps.println("	/**");
			ps.println("	 * returns value of the " + info.getColName() + " column of this row of data");
			ps.println("	 *");
			ps.println("	 * @return value of this column in this row");
			ps.println("	 */");
			ps.println("	public " + info.getType() + " get" + info.getGsName() + "() {");
			if ("Float".equals(info.getType())) {
				ps.println("		if (" + info.getVName() + " == null)");
				ps.println("	    	return 0.0f;");
				ps.println("		return " + info.getVName() + ".floatValue();");
			} else if ("Double".equals(info.getType())) {
				ps.println("		if (" + info.getVName() + " == null)");
				ps.println("	    	return 0;");
				ps.println("		return " + info.getVName() + ".doubleValue();");
			} else if ("BigDecimal".equals(info.getType())) {
				ps.println("		if (" + info.getVName() + " == null)");
				ps.println("	    	return BigDecimal.ZERO;");
				ps.println("		return " + info.getVName() + ";");
			} else if ("Integer".equals(info.getType())) {
				ps.println("		if (" + info.getVName() + " == null)");
				ps.println("	    	return 0;");
				ps.println("		return " + info.getVName() + ".intValue();");
			} else if ("Long".equals(info.getType())) {
				ps.println("		if (" + info.getVName() + " == null)");
				ps.println("	    	return 0l;");
				ps.println("		return " + info.getVName() + ".longValue();");
			} else {
				ps.println("		return " + info.getVName() + ';');
			}
			ps.println("	}");
			if (isForm && info.isPassword()) {
				ps.println("	public " + info.getType() + " get" + info.getGsName() + "Confirm() {");
				ps.println("		return " + info.getVName() + "Confirm;");
				ps.println("	}");

			}
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
				ps.println("	 * This field has a max length of " + info.getLength());
			}
			ps.println("	 */");
			ps.println("	public void set" + info.getGsName() + '(' + info.getType() + " newVal) {");
			ps.println("		" + info.getVName() + " = newVal;");
			ps.println("	}");
			ps.println("");
			if (isForm && info.isPassword()) {
				ps.println("	public void set" + info.getGsName() + "Confirm(" + info.getType() + " newVal) {");
				ps.println("		" + info.getVName() + "Confirm = newVal;");
				ps.println("	}");
				ps.println("");
			}
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
//		System.err.println(
//				"-beanEquals = generate equals() methods for entities beans. Can be overridden in properties file.");
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

		String query = "SHOW TABLES;"; // mySQL
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
				String name = null;
				if (db.isMySQL()) {
					name = rs.getString("Tables_in_" + dbName);
				} else {
					name = rs.getString("name").toLowerCase();
				}
				if (!StringUtils.isBlank(name) && !filteredTables.contains(name))
					tableNames.add(name);
				else
					LOGGER.info("skipping:" + name);
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
			obj.initVars();

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
