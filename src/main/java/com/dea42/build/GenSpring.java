package com.dea42.build;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeMap;
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
	private static final String strVal = "ABCDEFGHIJKLMNOPQRSTUVWXYZ01234567890abcdefghijklmnopqrstuvwzyz";
	private static final Logger LOGGER = LoggerFactory.getLogger(GenSpring.class.getName());
	public static final String propKey = "genSpring";

	private boolean useDouble = false;
	private boolean beanToString = false;
	private ResourceBundle bundle = null;
	private ResourceBundle renames = ResourceBundle.getBundle("rename");
	private String srcGroupId = "com.dea42";
	private String srcArtifactId = "genspring";
	private String srcPkg;
	private String srcPath;
	private String baseGroupId;
	private String baseArtifactId;
	private String basePkg;
	private String basePath;
	private String baseDir;
	private int year = 2001;
	private String schema = null;

	public GenSpring() throws IOException {
		GregorianCalendar gc = new GregorianCalendar();
		year = gc.get(Calendar.YEAR);

		bundle = ResourceBundle.getBundle(propKey);
		baseDir = Utils.getProp(bundle, propKey + ".outdir");
		schema = Utils.getProp(bundle, propKey + ".schema");
		baseGroupId = Utils.getProp(bundle, propKey + ".pkg");
		baseArtifactId = Utils.getProp(bundle, propKey + ".module");
		basePkg = baseGroupId + '.' + baseArtifactId;
		basePath = basePkg.replace('.', '/');

		srcPkg = srcGroupId + '.' + srcArtifactId;
		srcPath = srcPkg.replace('.', '/');
		File outDir = new File(baseDir);
		if (!outDir.exists()) {
			if (!outDir.mkdirs()) {
				throw new IOException("Could not create output dir:" + baseDir);
			}
		}
	}

	private Path createFile(String relPath) {
		Path p = Paths.get(baseDir, relPath);
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
	private void genIndex(Set<String> set) {
		Path p = createFile("/src/main/resources/templates/index.html");
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
				ps.println(htmlHeader("Home"));
				for (String clsName : set) {
					String fieldName = clsName.substring(0, 1).toLowerCase() + clsName.substring(1);
					ps.println("    	<a th:href=\"@{/" + fieldName + "s}\">" + clsName + "</a><br>");
				}
				ps.println("    	<a th:href=\"@{/api/}\">/api/</a><br>");
				ps.println("    	<a th:href=\"@{/login}\">Login</a><br>");
				htmlFooter();
				LOGGER.warn("Wrote:" + p.toString());
			} catch (Exception e) {
				LOGGER.error("failed to create " + p, e);
				p.toFile().delete();
			}
		}
	}

	private void genApiIndex(Set<String> set) {
		Path p = createFile("/src/main/resources/templates/api_index.html");
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
				ps.println(htmlHeader("API Home"));
				for (String clsName : set) {
					String fieldName = clsName.substring(0, 1).toLowerCase() + clsName.substring(1);
					ps.println("    	<a th:href=\"@{/api/" + fieldName + "s}\">" + clsName + "</a><br>");
				}
				ps.println("    	<a th:href=\"@{/}\">Home</a><br>");
				ps.println("    	<a th:href=\"@{/login}\">Login</a><br>");
				htmlFooter();
				LOGGER.warn("Wrote:" + p.toString());
			} catch (Exception e) {
				LOGGER.error("failed to create " + p, e);
				p.toFile().delete();
			}
		}
	}

	public void copyCommon() throws IOException {
		String pathString = "static";
		int beginIndex = pathString.length();
		Files.walkFileTree(Paths.get(pathString), new FileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				String relDir = dir.toString().substring(beginIndex).replace('\\', '/').replace(srcPath, basePath);
				Path target = Paths.get(baseDir + relDir);
				Files.createDirectories(target);

				System.out.println("preVisitDirectory: " + dir + "->" + target);
				return FileVisitResult.CONTINUE;
			}

			/**
			 * Copy file into new tree converting package / paths as needed
			 */
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				System.out.println("visitFile: " + file);
				String data = new String(Files.readAllBytes(file));
				data = data.replace(srcPkg, basePkg);
				data = data.replace(srcGroupId, baseGroupId);
				data = data.replace(srcArtifactId, baseArtifactId);

				String outFile = file.toString().substring(beginIndex).replace('\\', '/').replace(srcPath, basePath);
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
				System.out.println("visitFileFailed: " + file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				System.out.println("postVisitDirectory: " + dir);
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
	public void genProject(List<String> tableNames) throws Exception {
		copyCommon();

		writeAppProps();

		Map<String, Map<String, ColInfo>> colsInfo = new HashMap<String, Map<String, ColInfo>>();
		for (String tableName : tableNames) {
			String clsName = Utils.tabToStr(renames, tableName);
			colsInfo.put(clsName, genFiles(tableName));
		}
		writeApiController(colsInfo.keySet());
		writeApiControllerTest(colsInfo);
		writeAppControllerTest(colsInfo.keySet());
		genIndex(colsInfo.keySet());
		genApiIndex(colsInfo.keySet());
	}

	/**
	 * Entry point for generating all the files you need for Spring maint screens to
	 * Add/Edit/Delete/Search record for a table.
	 * 
	 * @param tableName
	 * @throws Exception
	 */
	public Map<String, ColInfo> genFiles(String tableName) throws Exception {

		String fakePK = null;
		String pkCol = null;
		TreeMap<String, ColInfo> namList = new TreeMap<String, ColInfo>();
		String className = null;
		String create = "";
		Map<String, ColInfo> cols = new HashMap<String, ColInfo>(100);

		Db db = new Db(propKey + ".genFiles()", propKey);
		Connection conn = db.getConnection(propKey + ".genFiles()");
		if (db.getDbUrl().indexOf("mysql") > -1) {
			String sql = "show create table " + tableName;
			Statement stmt = conn.createStatement();
			stmt.setMaxRows(1);
			LOGGER.debug("query=" + sql);
			ResultSet rs = stmt.executeQuery(sql);
			if (rs.next()) {
				create = rs.getString("Create Table");
			}
		}

		LOGGER.debug("tableName:" + tableName);
		String query = "SELECT * FROM " + tableName;
		Statement stmt = conn.createStatement();
		stmt.setMaxRows(1);
		LOGGER.debug("query=" + query);
		ResultSet rs = stmt.executeQuery(query);
		ResultSetMetaData rm = rs.getMetaData();
		int size = rm.getColumnCount();
		for (int i = 1; i <= size; i++) {
			ColInfo info = new ColInfo();
			info.setfNum(i);

			String cnam = rm.getColumnName(i);// .toUpperCase();
			info.setColName(cnam);
			if (fakePK == null) {
				fakePK = cnam;
			}
			info.setPk(rm.isAutoIncrement(i) || cnam.equals(pkCol));
			String type = rm.getColumnTypeName(i).toUpperCase();
			int len = rm.getColumnDisplaySize(i);
			if (len >= 2147483647) {
				if (rm.getPrecision(i) < 2147483647) {
					len = rm.getPrecision(i);
				} else {
					len = 0;
				}
			}

			info.setLength(len);
			cnam = Utils.tabToStr(renames, cnam);
			info.setVName(cnam.substring(0, 1).toLowerCase() + cnam.substring(1));
			info.setGsName(cnam);
			// a java.sql.Types
			int stype = rm.getColumnType(i);
			info.setStype(stype);
			switch (stype) {
			case Types.VARCHAR:
			case Types.NVARCHAR:
			case Types.LONGNVARCHAR:
			case Types.LONGVARCHAR:
			case Types.BLOB:
			case Types.CHAR:
			case Types.SQLXML:
				info.setType("String");
				break;

			case Types.REAL:
				info.setType("Float");
				info.setColPrecision(rm.getPrecision(i));
				info.setColScale(rm.getScale(i));
				break;

			case Types.FLOAT:
			case Types.DECIMAL:
			case Types.DOUBLE:
			case Types.NUMERIC:
				if (useDouble) {
					info.setType("Double");
				} else {
					info.setType("BigDecimal");
				}
				info.setColPrecision(rm.getPrecision(i));
				info.setColScale(rm.getScale(i));
				break;

			case Types.INTEGER:
			case Types.SMALLINT:
			case Types.TINYINT:
				info.setType("Integer");
				break;
			case Types.BIGINT:
				info.setType("Long");
				break;
			case Types.TIMESTAMP:
				info.setType("Timestamp");
				break;
			case Types.DATE:
				info.setType("Date");
				break;
			case Types.VARBINARY:
			case Types.CLOB:
				info.setType("byte[]");
				break;
			default:
				// if its something else treat it like a String for now
				System.err.println("Type " + type + " Unknown treating like String");
				info.setType("String");
			}

			if (info.getColName() != null && info.getType() != null && info.getType().length() > 0) {
				LOGGER.info("storing:" + info);
				cols.put(info.getConstName(), info);
				namList.put(info.getVName(), info);
			}
		}

		// write bean with helpers
		className = Utils.tabToStr(renames, tableName);

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
			pkCol = fakePK;
			LOGGER.error(tableName + " does not have a primary key. Using " + pkCol);
		}
		ColInfo pkinfo = cols.get(pkCol.toUpperCase());
		pkinfo.setPk(true);

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
		db.close(propKey + ".genFiles()");

		writeBean(tableName, className, namList, create);
		writeRepo(className, pkinfo);
		writeService(className, pkinfo);
		writeListPage(className, namList, pkinfo);
		writeObjController(className, pkinfo);
		writeObjControllerTest(className, pkinfo, namList);
		writeEditPage(className, namList, pkinfo);

		return cols;
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
				ps.println("/**");
				ps.println(" * Title: ApiController <br>");
				ps.println(" * Description: Api REST Controller. <br>");
				String tmp = Utils.getProp(bundle, "genSpring.Copyright", "");
				if (!org.apache.commons.lang3.StringUtils.isBlank(tmp)) {
					ps.println(" * Copyright: " + tmp + year + "<br>");
				}
				tmp = Utils.getProp(bundle, "genSpring.Company", "");
				if (!org.apache.commons.lang3.StringUtils.isBlank(tmp)) {
					ps.println(" * Company: " + tmp + "<br>");
				}
				ps.println(" * @author Gened by " + this.getClass().getCanonicalName() + "<br>");
				ps.println(" * @version " + Utils.getProp(bundle, "genSpring.version", "1.0") + "<br>");
				ps.println(" */");
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
				ps.println("import org.junit.Before;");
				ps.println("import org.junit.Test;");
				ps.println("import org.junit.runner.RunWith;");
				ps.println("import org.springframework.beans.factory.annotation.Autowired;");
				ps.println("import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;");
				ps.println("import org.springframework.boot.test.mock.mockito.MockBean;");
				ps.println("import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;");
				ps.println("import org.springframework.test.web.servlet.MockMvc;");
				ps.println("import org.springframework.test.web.servlet.setup.MockMvcBuilders;");
				ps.println("import org.springframework.web.context.WebApplicationContext;");
				ps.println("");
				for (String clsName : set) {
					ps.println("import " + basePkg + ".entity." + clsName + ";");
					ps.println("import " + basePkg + ".service." + clsName + "Services;");
				}
				ps.println("");
				ps.println("/**");
				ps.println(" * Title: ApiControllerTest <br>");
				ps.println(" * Description: Api REST Controller Test. <br>");
				String tmp = Utils.getProp(bundle, "genSpring.Copyright", "");
				if (!org.apache.commons.lang3.StringUtils.isBlank(tmp)) {
					ps.println(" * Copyright: " + tmp + year + "<br>");
				}
				tmp = Utils.getProp(bundle, "genSpring.Company", "");
				if (!org.apache.commons.lang3.StringUtils.isBlank(tmp)) {
					ps.println(" * Company: " + tmp + "<br>");
				}
				ps.println(" * @author Gened by " + this.getClass().getCanonicalName() + "<br>");
				ps.println(" * @version " + Utils.getProp(bundle, "genSpring.version", "1.0") + "<br>");
				ps.println(" */");
				ps.println("@RunWith(SpringJUnit4ClassRunner.class)");
				ps.println("@WebMvcTest(ApiController.class)");
				ps.println("public class ApiControllerTest {");
				ps.println("	private MockMvc mockMvc;");
				ps.println("");
				ps.println("	@Autowired");
				ps.println("	private WebApplicationContext webApplicationContext;");
				ps.println("");
				for (String clsName : set) {
					String fieldName = clsName.substring(0, 1).toLowerCase() + clsName.substring(1);
					ps.println("    @MockBean");
					ps.println("    private " + clsName + "Services " + fieldName + "Service;");
				}
				ps.println("");
				ps.println("	@Before()");
				ps.println("	public void setup() {");
				ps.println("		// Init MockMvc Object and build");
				ps.println("		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();");
				ps.println("	}");
				ps.println("");
				ps.println("    public ApiControllerTest(){");
				ps.println("    }");
				for (String clsName : set) {
					Map<String, ColInfo> namList = colsInfo.get(clsName);
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
					ps.println("		o.setId(1);");
					Iterator<String> it = namList.keySet().iterator();
					while (it.hasNext()) {
						String name = it.next();
						ColInfo info = (ColInfo) namList.get(name);
						if (info.isString()) {
							int endIndex = info.getLength();
							if (endIndex > 0) {
								if (endIndex > strVal.length())
									endIndex = strVal.length() - 1;
								ps.println("         o.set" + info.getGsName() + "(\"" + strVal.substring(0, endIndex)
										+ "\");");
							}
						}
					}
					ps.println("		list.add(o);");
					ps.println("");
					ps.println("		given(" + fieldName + "Service.listAll()).willReturn(list);");
					ps.println("");
					ps.println("		this.mockMvc.perform(get(\"/api/" + fieldName
							+ "s\").with(user(\"user\").roles(\"ADMIN\"))).andExpect(status().isOk())");
					it = namList.keySet().iterator();
					while (it.hasNext()) {
						String name = it.next();
						ColInfo info = (ColInfo) namList.get(name);
						if (info.isString()) {
							int endIndex = info.getLength();
							if (endIndex > 0) {
								if (endIndex > strVal.length())
									endIndex = strVal.length() - 1;
								ps.println("				.andExpect(content().string(containsString(\""
										+ strVal.substring(0, endIndex) + "\")))");
							}
						}
						ps.print("				.andExpect(content().string(containsString(\"" + info.getVName()
								+ "\")))");
						if (it.hasNext()) {
							ps.println("");
						} else {
							ps.println(";");
						}
					}
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
	 * @param title
	 * @return
	 */
	private String htmlHeader(String title) {
		StringBuilder sb = new StringBuilder();

		sb.append("<!DOCTYPE html>");
		sb.append("<html lang=\"en\" xmlns:th=\"http://www.thymeleaf.org\">");
		sb.append("<head>");
		sb.append("    <meta charset=\"UTF-8\"/>");
		sb.append("    <title>" + Utils.getProp(bundle, propKey + ".module") + " | " + title + "</title>");
		sb.append("    <link th:href=\"@{/css/site.css}\" rel=\"stylesheet\"/>");
		sb.append("</head>");
		sb.append("<body>");

		return sb.toString();
	}

	private String htmlFooter() {
		StringBuilder sb = new StringBuilder();

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
				ps.println("/**");
				ps.println(" * Title: " + clsName + "Repository <br>");
				ps.println(" * Description: Class for the " + clsName + " Repository. <br>");
				String tmp = Utils.getProp(bundle, "genSpring.Copyright", "");
				if (!org.apache.commons.lang3.StringUtils.isBlank(tmp)) {
					ps.println(" * Copyright: " + tmp + year + "<br>");
				}
				tmp = Utils.getProp(bundle, "genSpring.Company", "");
				if (!org.apache.commons.lang3.StringUtils.isBlank(tmp)) {
					ps.println(" * Company: " + tmp + "<br>");
				}
				ps.println(" * @author Gened by " + this.getClass().getCanonicalName() + "<br>");
				ps.println(" * @version " + Utils.getProp(bundle, "genSpring.version", "1.0") + "<br>");
				ps.println(" */");
				ps.println("@Repository");
				ps.println("public interface " + clsName + "Repository extends JpaRepository<" + clsName + ", "
						+ pkinfo.getType() + ">{");
				if (pkinfo.getStype() != Types.INTEGER) {
					ps.println("//Primary key is not int which will require custom code to be added below");
				}
				ps.println("}");
				LOGGER.warn("Wrote:" + p.toString());
			} catch (Exception e) {
				LOGGER.error("failed to create " + p, e);
				p.toFile().delete();
			}
		}
	}

	private void writeEditPage(String clsName, TreeMap<String, ColInfo> namList, ColInfo pkinfo) {
		String fieldName = clsName.substring(0, 1).toLowerCase() + clsName.substring(1);
		String outFile = "/src/main/resources/templates/edit_" + fieldName + ".html";
		Set<String> set = namList.keySet();
		Path p = createFile(outFile);
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
				ps.println(htmlHeader("Edit " + clsName));
				ps.println("	<div align=\"center\">");
				ps.println("		<h1 th:if=\"${" + fieldName + "." + pkinfo.getVName() + " == 0}\">Create New "
						+ clsName + "</h1>");
				ps.println("		<h1 th:if=\"${" + fieldName + "." + pkinfo.getVName() + " > 0}\">Edit " + clsName
						+ "</h1>");
				ps.println("		<br />");
				ps.println("		<form action=\"#\" th:action=\"@{/" + fieldName + "s/save}\" th:object=\"${"
						+ fieldName + "}\"");
				ps.println("			method=\"post\">");
				ps.println("");
				ps.println("			<table >");
				ps.println("				<tr th:if=\"${" + fieldName + "." + pkinfo.getVName()
						+ " > 0}\">				");
				ps.println("					<td>" + pkinfo.getColName() + ":</td>");
				ps.println("					<td>");
				ps.println("						<input type=\"text\" th:field=\"*{" + pkinfo.getVName()
						+ "}\" readonly=\"readonly\" />");
				ps.println("					</td>");
				ps.println("				</tr>");
				Iterator<String> it = set.iterator();
				while (it.hasNext()) {
					ColInfo info = (ColInfo) namList.get(it.next());
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
						"					<td colspan=\"2\"><button type=\"submit\" name=\"action\" value=\"save\">save</button> <button type=\"submit\" name=\"action\" value=\"cancel\">cancel</button></td>");
				ps.println("				</tr>");
				ps.println("			</table>");
				ps.println("		</form>");
				ps.println("	</div>");
				htmlFooter();
				LOGGER.warn("Wrote:" + p.toString());
			} catch (Exception e) {
				LOGGER.error("failed to create " + p, e);
				p.toFile().delete();
			}
		}
	}

	private Set<String> getListKeys(String clsName, TreeMap<String, ColInfo> namList) throws DataFormatException {
		Set<String> set = namList.keySet();
		List<String> listCols = Utils.getPropList(bundle, clsName + ".list");
		if (listCols == null) {
			set = namList.keySet();
		} else {
			set = listCols.stream().collect(Collectors.toSet());
			// validate list against DB data
			Object[] names = set.toArray();
			for (Object name : names) {
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

	private void writeListPage(String clsName, TreeMap<String, ColInfo> namList, ColInfo pkinfo) {
		String fieldName = clsName.substring(0, 1).toLowerCase() + clsName.substring(1);
		String outFile = "/src/main/resources/templates/" + fieldName + "s.html";

		Path p = createFile(outFile);
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
				Set<String> set = getListKeys(clsName, namList);
				ps.println(htmlHeader(clsName + " List View"));
				ps.println("	<div align=\"center\">");
				ps.println("		<h1>" + clsName + " List</h1>");
				ps.println("		<a th:href=\"@{/" + fieldName + "s/new}\">Create New " + clsName + "</a> <br />");
				ps.println("		<br />");
				ps.println("");
				ps.println("	    <table>");
				ps.println("	        <tr>");

				Iterator<String> it = set.iterator();
				while (it.hasNext()) {
					String name = it.next();
					ColInfo info = (ColInfo) namList.get(name);
					ps.println("            <th>" + info.getColName() + "</th>");
				}
				ps.println("            <th>Actions</th>");
				ps.println("	        </tr>");
				ps.println("	        <tr th:each=\"" + fieldName + ":${" + fieldName + "s}\">");
				it = set.iterator();
				while (it.hasNext()) {
					String name = it.next();
					ColInfo info = (ColInfo) namList.get(name);
					if (namList.containsKey(name + "link")) {
						ColInfo infoLnk = (ColInfo) namList.get(name + "link");
						ps.println("	            <td><a th:href=\"@{${" + fieldName + "." + infoLnk.getVName()
								+ "}}\"><span th:text=\"${" + fieldName + "." + info.getVName()
								+ "}\"></span></a></td>");

					} else {
						ps.println(
								"	            <td th:text=\"${" + fieldName + "." + info.getVName() + "}\"></td>");
					}
				}
				ps.println("				<td><a th:href=\"@{'/" + fieldName + "s/edit/' + ${" + fieldName + "."
						+ pkinfo.getVName() + "}}\">Edit</a>");
				ps.println("					&nbsp;&nbsp;&nbsp; <a th:href=\"@{'/" + fieldName + "s/delete/' + ${"
						+ fieldName + "." + pkinfo.getVName() + "}}\">Delete</a>");
				ps.println("				</td>");
				ps.println("");
				ps.println("	        </tr>");
				ps.println("	    </table>");
				ps.println("    </div>");
				ps.println("");
				htmlFooter();
				LOGGER.warn("Wrote:" + p.toString());
			} catch (Exception e) {
				LOGGER.error("failed to create " + p, e);
				p.toFile().delete();
			}
		}
	}

	private void writeAppControllerTest(Set<String> set) {
		String pkgNam = basePkg + ".controller";
		String relPath = pkgNam.replace('.', '/');
		String outFile = "/src/test/java/" + relPath + "/AppControllerTest.java";
		Path p = createFile(outFile);
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
				ps.println("package " + pkgNam + ';');
				ps.println("");
				ps.println("import static org.hamcrest.CoreMatchers.containsString;");
				ps.println(
						"import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;");
				ps.println("import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;");
				ps.println("import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;");
				ps.println("import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;");
				ps.println("");
				ps.println("import org.junit.Before;");
				ps.println("import org.junit.Test;");
				ps.println("import org.junit.runner.RunWith;");
				ps.println("import org.springframework.beans.factory.annotation.Autowired;");
				ps.println("import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;");
				ps.println("import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;");
				ps.println("import org.springframework.test.web.servlet.MockMvc;");
				ps.println("import org.springframework.test.web.servlet.setup.MockMvcBuilders;");
				ps.println("import org.springframework.web.context.WebApplicationContext;");
				ps.println("");
				ps.println("@RunWith(SpringJUnit4ClassRunner.class)");
				ps.println("@WebMvcTest(AppController.class)");
				ps.println("public class AppControllerTest {");
				ps.println("	private MockMvc mockMvc;");
				ps.println("");
				ps.println("	@Autowired");
				ps.println("	private WebApplicationContext webApplicationContext;");
				ps.println("");
				ps.println("	@Before()");
				ps.println("	public void setup() {");
				ps.println("		// Init MockMvc Object and build");
				ps.println("		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();");
				ps.println("	}");
				ps.println("");
				ps.println("	/**");
				ps.println("	 * Test method for");
				ps.println("	 * {@link " + basePkg + ".controller.AppController#getLoginPage()}.");
				ps.println("	 */");
				ps.println("	@Test");
				ps.println("	public void testGetLoginPage() throws Exception {");
				ps.println("		this.mockMvc.perform(get(\"/login\")).andExpect(status().isOk())");
				ps.println("				.andExpect(content().string(containsString(\"Login\")))");
				ps.println("				.andExpect(content().string(containsString(\"Password:\")))");
				ps.println("				.andExpect(content().string(containsString(\"User Name:\")));");
				ps.println("	}");
				ps.println("");
				ps.println("	/**");
				ps.println("	 * Test method for");
				ps.println("	 * {@link " + basePkg + ".controller.AppController#getIndex()}.");
				ps.println("	 * ");
				ps.println("	 * @throws Exception");
				ps.println("	 */");
				ps.println("	@Test");
				ps.println("	public void testGetIndex() throws Exception {");
				ps.println(
						"		this.mockMvc.perform(get(\"/\").with(user(\"user\").roles(\"ADMIN\"))).andExpect(status().isOk())");
				ps.println("				.andExpect(content().string(containsString(\"Home\")))");
				ps.println("				.andExpect(content().string(containsString(\"Login\")))");
				ps.println("				.andExpect(content().string(containsString(\"/api/\")))");
				Iterator<String> it = set.iterator();
				while (it.hasNext()) {
					ps.print("				.andExpect(content().string(containsString(\"" + it.next() + "\")))");
					if (it.hasNext())
						ps.println("");
					else
						ps.println(";");
				}
				ps.println("	}");
				ps.println("");
				ps.println("	/**");
				ps.println("	 * Test method for");
				ps.println("	 * {@link " + basePkg + ".controller.ApiController#getApiIndex()}.");
				ps.println("	 * ");
				ps.println("	 * @throws Exception");
				ps.println("	 */");
				ps.println("	@Test");
				ps.println("	public void testGetApiIndex() throws Exception {");
				ps.println(
						"		this.mockMvc.perform(get(\"/api/\").with(user(\"user\").roles(\"ADMIN\"))).andExpect(status().isOk())");
				ps.println("				.andExpect(content().string(containsString(\"API Home\")))");
				ps.println("				.andExpect(content().string(containsString(\"Login\")))");
				it = set.iterator();
				while (it.hasNext()) {
					String clsName = it.next();
					String fieldName = clsName.substring(0, 1).toLowerCase() + clsName.substring(1);
					ps.print("				.andExpect(content().string(containsString(\"" + fieldName + "\")))");
					if (it.hasNext())
						ps.println("");
					else
						ps.println(";");
				}
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

	private void writeObjControllerTest(String clsName, ColInfo pkinfo, TreeMap<String, ColInfo> namList) {
		String pkgNam = basePkg + ".controller";
		String relPath = pkgNam.replace('.', '/');
		String outFile = "/src/test/java/" + relPath + '/' + clsName + "ControllerTest.java";
		String fieldName = clsName.substring(0, 1).toLowerCase() + clsName.substring(1);
		Path p = createFile(outFile);
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
				Set<String> set = getListKeys(clsName, namList);
				ps.println("package " + pkgNam + ';');
				ps.println("import static org.hamcrest.CoreMatchers.containsString;");
				ps.println("import static org.mockito.BDDMockito.given;");
				ps.println(
						"import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;");
				ps.println("import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;");
				ps.println("import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;");
				ps.println("import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;");
				ps.println(
						"import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;");
				ps.println("import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;");
				ps.println("");
				ps.println("import java.util.ArrayList;");
				ps.println("import java.util.List;");
				ps.println("");
				ps.println("import org.junit.Before;");
				ps.println("import org.junit.Test;");
				ps.println("import org.junit.runner.RunWith;");
				ps.println("import org.springframework.beans.factory.annotation.Autowired;");
				ps.println("import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;");
				ps.println("import org.springframework.boot.test.mock.mockito.MockBean;");
				ps.println("import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;");
				ps.println("import org.springframework.test.web.servlet.MockMvc;");
				ps.println("import org.springframework.test.web.servlet.setup.MockMvcBuilders;");
				ps.println("import org.springframework.web.context.WebApplicationContext;");
				ps.println("");
				ps.println("import " + basePkg + ".entity." + clsName + ";");
				ps.println("import " + basePkg + ".service." + clsName + "Services;");
				ps.println("");
				ps.println("/**");
				ps.println(" * Title: " + clsName + "ControllerTest <br>");
				ps.println(" * Description: Class for testing the " + clsName + "Controller. <br>");
				String tmp = Utils.getProp(bundle, "genSpring.Copyright", "");
				if (!org.apache.commons.lang3.StringUtils.isBlank(tmp)) {
					ps.println(" * Copyright: " + tmp + year + "<br>");
				}
				tmp = Utils.getProp(bundle, "genSpring.Company", "");
				if (!org.apache.commons.lang3.StringUtils.isBlank(tmp)) {
					ps.println(" * Company: " + tmp + "<br>");
				}
				ps.println(" * @author Gened by " + this.getClass().getCanonicalName() + "<br>");
				ps.println(" * @version " + Utils.getProp(bundle, "genSpring.version", "1.0") + "<br>");
				ps.println(" */");
				ps.println("@RunWith(SpringJUnit4ClassRunner.class)");
				ps.println("@WebMvcTest(" + clsName + "Controller.class)");
				ps.println("public class " + clsName + "ControllerTest {");
				ps.println("	@MockBean");
				ps.println("	private " + clsName + "Services " + fieldName + "Service;");
				ps.println("");
				ps.println("	private MockMvc mockMvc;");
				ps.println("");
				ps.println("	@Autowired");
				ps.println("	private WebApplicationContext webApplicationContext;");
				ps.println("");
				ps.println("	@Before()");
				ps.println("	public void setup() {");
				ps.println("		// Init MockMvc Object and build");
				ps.println("		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();");
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
				ps.println("		" + clsName + " o = new " + clsName + "();");
				ps.println("		o.setId(1);");
				Iterator<String> it = set.iterator();
				while (it.hasNext()) {
					String name = it.next();
					ColInfo info = (ColInfo) namList.get(name);
					if (info.isString()) {
						int endIndex = info.getLength();
						if (endIndex > 0) {
							if (endIndex > strVal.length())
								endIndex = strVal.length() - 1;
							ps.println("         o.set" + info.getGsName() + "(\"" + strVal.substring(0, endIndex)
									+ "\");");
						}
					}
				}
				ps.println("		list.add(o);");
				ps.println("");
				ps.println("		given(" + fieldName + "Service.listAll()).willReturn(list);");
				ps.println("");
				ps.println("		this.mockMvc.perform(get(\"/" + fieldName
						+ "s\").with(user(\"user\").roles(\"ADMIN\"))).andExpect(status().isOk())");
				ps.println("				.andExpect(content().string(containsString(\"<h1>" + clsName
						+ " List</h1>\")))");
				it = set.iterator();
				while (it.hasNext()) {
					String name = it.next();
					ColInfo info = (ColInfo) namList.get(name);
					if (info.isString()) {
						int endIndex = info.getLength();
						if (endIndex > 0) {
							if (endIndex > strVal.length())
								endIndex = strVal.length() - 1;
							ps.println("				.andExpect(content().string(containsString(\""
									+ strVal.substring(0, endIndex) + "\")))");
						}
					}
					ps.print("				.andExpect(content().string(containsString(\"" + info.getColName()
							+ "\")))");
					if (it.hasNext()) {
						ps.println("");
					} else {
						ps.println(";");
					}
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
				ps.println("		this.mockMvc.perform(get(\"/" + fieldName
						+ "s/new\").with(user(\"user\").roles(\"ADMIN\"))).andExpect(status().isOk())");
				ps.println("				.andExpect(content().string(containsString(\"<h1>Create New " + clsName
						+ "</h1>\")))");
				set = namList.keySet();
				it = set.iterator();
				while (it.hasNext()) {
					String name = it.next();
					ColInfo info = (ColInfo) namList.get(name);
					ps.print("				.andExpect(content().string(containsString(\"" + info.getColName()
							+ "\")))");
					if (it.hasNext()) {
						ps.println("");
					} else {
						ps.println(";");
					}
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
				ps.println("		this.mockMvc.perform(post(\"/" + fieldName
						+ "s/save\").param(\"action\", \"cancel\").with(user(\"user\").roles(\"ADMIN\")))");
				ps.println("				.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(\"/"
						+ fieldName + "s\"));");
				ps.println("	}");
				ps.println("");
				ps.println("	/**");
				ps.println("	 * Test method for");
				ps.println("	 * {@link " + basePkg + ".controller." + clsName + "Controller#save" + clsName + "("
						+ basePkg + ".entity." + clsName + ", java.lang.String)}.");
				ps.println("	 */");
				ps.println("	@Test");
				ps.println("	public void testSave" + clsName + "Save() throws Exception {");
				ps.println("		this.mockMvc.perform(post(\"/" + fieldName
						+ "s/save\").param(\"action\", \"save\").with(user(\"user\").roles(\"ADMIN\")))");
				ps.println("				.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(\"/"
						+ fieldName + "s\"));");
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
				ps.println("		" + clsName + " o = new " + clsName + "();");
				ps.println("		o.setId(1);");
				it = set.iterator();
				while (it.hasNext()) {
					String name = it.next();
					ColInfo info = (ColInfo) namList.get(name);
					if (info.isString()) {
						int endIndex = info.getLength();
						if (endIndex > 0) {
							if (endIndex > strVal.length())
								endIndex = strVal.length() - 1;
							ps.println("         o.set" + info.getGsName() + "(\"" + strVal.substring(0, endIndex)
									+ "\");");
						}
					}
				}
				ps.println("");
				ps.println("		given(" + fieldName + "Service.get(1)).willReturn(o);");
				ps.println("");
				ps.println("		this.mockMvc.perform(get(\"/" + fieldName
						+ "s/edit/1\").with(user(\"user\").roles(\"ADMIN\"))).andExpect(status().isOk())");
				it = set.iterator();
				while (it.hasNext()) {
					String name = it.next();
					ColInfo info = (ColInfo) namList.get(name);
					if (info.isString()) {
						int endIndex = info.getLength();
						if (endIndex > 0) {
							if (endIndex > strVal.length())
								endIndex = strVal.length() - 1;
							ps.println("				.andExpect(content().string(containsString(\""
									+ strVal.substring(0, endIndex) + "\")))");
						}
					}
					ps.print("				.andExpect(content().string(containsString(\"" + info.getColName()
							+ "\")))");
					if (it.hasNext()) {
						ps.println("");
					} else {
						ps.println(";");
					}
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
				ps.println("		this.mockMvc.perform(get(\"/" + fieldName
						+ "s/delete/1\").with(user(\"user\").roles(\"ADMIN\")))");
				ps.println("				.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(\"/"
						+ fieldName + "s\"));");
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
				ps.println("/**");
				ps.println(" * Title: " + clsName + "Controller <br>");
				ps.println(" * Description: Class for  " + clsName + "Controller. <br>");
				String tmp = Utils.getProp(bundle, "genSpring.Copyright", "");
				if (!org.apache.commons.lang3.StringUtils.isBlank(tmp)) {
					ps.println(" * Copyright: " + tmp + year + "<br>");
				}
				tmp = Utils.getProp(bundle, "genSpring.Company", "");
				if (!org.apache.commons.lang3.StringUtils.isBlank(tmp)) {
					ps.println(" * Company: " + tmp + "<br>");
				}
				ps.println(" * @author Gened by " + this.getClass().getCanonicalName() + "<br>");
				ps.println(" * @version " + Utils.getProp(bundle, "genSpring.version", "1.0") + "<br>");
				ps.println(" */");
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
				ps.println("import java.util.ArrayList;");
				ps.println("import java.util.List;");
				ps.println("");
				ps.println("import " + basePkg + ".entity." + clsName + ";");
				ps.println("import " + basePkg + ".repo." + clsName + "Repository;");
				ps.println("");
				ps.println("/**");
				ps.println(" * Title: " + clsName + "Services <br>");
				ps.println(" * Description: Class for the " + clsName + "Services. <br>");
				String tmp = Utils.getProp(bundle, "genSpring.Copyright", "");
				if (!org.apache.commons.lang3.StringUtils.isBlank(tmp)) {
					ps.println(" * Copyright: " + tmp + year + "<br>");
				}
				tmp = Utils.getProp(bundle, "genSpring.Company", "");
				if (!org.apache.commons.lang3.StringUtils.isBlank(tmp)) {
					ps.println(" * Company: " + tmp + "<br>");
				}
				ps.println(" * @author Gened by " + this.getClass().getCanonicalName() + "<br>");
				ps.println(" * @version " + Utils.getProp(bundle, "genSpring.version", "1.0") + "<br>");
				ps.println(" */");
				ps.println("@Service");
				ps.println("public class " + clsName + "Services {");
				ps.println("    @Autowired");
				ps.println("    private " + clsName + "Repository " + fieldName + "Repository;");
				ps.println("");
//			ps.println("    public List<" + clsName + "> getAll" + clsName + "s(){");
//			ps.println("        List<" + clsName + "> " + fieldName + "s = new ArrayList<>();");
//			ps.println("        this." + fieldName + "Repository.findAll().forEach(" + fieldName + "s::add);");
//			ps.println("        return " + fieldName + "s;");
//			ps.println("    }");
//			ps.println("");
				ps.println("	public List<" + clsName + "> listAll() {");
				ps.println("		return (List<" + clsName + ">) " + fieldName + "Repository.findAll();");
				ps.println("	}");
				ps.println("	");
				ps.println("	public void save(" + clsName + " product) {");
				ps.println("		" + fieldName + "Repository.save(product);");
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
		Path p = createFile("/src/main/resources/application.properties");
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
				ps.println("spring.jpa.hibernate.ddl-auto=none");
				ps.println("spring.jpa.show-sql=true");
				ps.println(
						"spring.jpa.hibernate.naming.physical-strategy=org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl");
				if (dbUrl.contains("sqlite")) {
					ps.println("## SQLite also needs");
					ps.println("spring.jpa.database-platform=" + basePkg + ".db.SQLiteDialect");
					ps.println("spring.datasource.driver-class-name = org.sqlite.JDBC");
				}
				ps.println("spring.datasource.url=" + dbUrl);
				ps.println("spring.datasource.username=" + Utils.getProp(bundle, "user", null));
				ps.println("spring.datasource.password=" + Utils.getProp(bundle, "password", null));
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

				ps.println("");
				ps.println("/**");
				ps.println(" * Title: " + viewName + " Bean <br>");
				ps.println(" * Description: Class for holding data from the " + viewName + " table. <br>");
				String tmp = Utils.getProp(bundle, "genSpring.Copyright", "");
				if (!org.apache.commons.lang3.StringUtils.isBlank(tmp)) {
					ps.println(" * Copyright: " + tmp + year + "<br>");
				}
				tmp = Utils.getProp(bundle, "genSpring.Company", "");
				if (!org.apache.commons.lang3.StringUtils.isBlank(tmp)) {
					ps.println(" * Company: " + tmp + "<br>");
				}
				ps.println(" * @author Gened by " + this.getClass().getCanonicalName() + "<br>");
				ps.println(" * @version " + Utils.getProp(bundle, "genSpring.version", "1.0") + "<br>");
				ps.println(" */");
				ps.println("@Entity");
				ps.println("@Table(name = \"" + viewName + "\")");
				ps.println("public class " + className + " implements Serializable {");
				ps.println("private static final long serialVersionUID = 1L;");
				ps.println("");
				Set<String> set = namList.keySet();
				Iterator<String> it = set.iterator();
				while (it.hasNext()) {
					ColInfo info = (ColInfo) namList.get(it.next());
					if (info.isPk()) {
						ps.println("    @Id");
						if (info.getStype() == Types.INTEGER)
							ps.println("    @GeneratedValue(strategy = GenerationType.IDENTITY)");
					}
					StringBuilder sb = new StringBuilder("	@Column(name=\"" + info.getColName() + "\"");
					if (info.isRequired())
						sb.append(", nullable=false");
					if ("String".equals(info.getType()) && info.getLength() > 0) {
						sb.append(", length=" + info.getLength());
					}
					sb.append(")");
					ps.println(sb.toString());
					ps.println("private " + info.getType() + ' ' + info.getVName() + ';');
				}

				ps.println("");
				ps.println("/**");
				ps.println(" * Basic constructor");
				ps.println(" */");
				ps.println("public " + className + "() {");
				ps.println("}");
				ps.println("");
				ps.println("/**");
				ps.println(" * Full constructor");
				ps.println(" *");
				ps.println(" */");
				StringBuilder sb = new StringBuilder("	public " + className + "(");
				set = namList.keySet();
				it = set.iterator();
				boolean addCom = false;
				while (it.hasNext()) {
					ColInfo info = (ColInfo) namList.get(it.next());
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
					ColInfo info = (ColInfo) namList.get(it.next());
					ps.println("		this." + info.getVName() + " = " + info.getVName() + ";");
				}
				ps.println("	}");
				writeBeanGetSets(ps, namList);
				if (beanToString) {
					ps.println("/**");
					ps.println(" * Returns a String showing the values of this bean - mainly for debuging");
					ps.println(" *");
					ps.println(" * @return String");
					ps.println(" */");
					ps.println("	public String toString(){");
					ps.println("		StringBuffer sb = new StringBuffer();");
					set = namList.keySet();
					it = set.iterator();
					while (it.hasNext()) {
						ColInfo info = (ColInfo) namList.get(it.next());
						ps.println(
								"		sb.append(\"" + info.getVName() + "= \" + " + info.getVName() + "+\'\\n\');");
					}
					ps.println("		return sb.toString();");
					ps.println("	}");
				}
				LOGGER.debug("Done with get/sets writing");

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
			ColInfo info = (ColInfo) namList.get(it.next());
//			ps.println("/**");
//			ps.println(" * returns value of the " + info.getColName() + " column of this row of data");
//			ps.println(" *");
//			ps.println(" * @return value of this column in this row");
//			ps.println(" */");
//			ps.println("public " + info.getType() + " getObj" + info.getGsName() + "() {");
//			ps.println("    return " + info.getVName() + ';');
//			ps.println("}");
//			ps.println("");

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
				ps.println("	if (" + info.getVName() + "== null)");
				ps.println("	    return 0.0f;");
				ps.println("	return " + info.getVName() + ".floatValue();");
			} else if ("Double".equals(info.getType())) {
				ps.println("	if (" + info.getVName() + "== null)");
				ps.println("	    return 0;");
				ps.println("	return " + info.getVName() + ".doubleValue();");
			} else if ("BigDecimal".equals(info.getType())) {
				ps.println("	if (" + info.getVName() + "== null)");
				ps.println("	    return BigDecimal.ZERO;");
				ps.println("	return " + info.getVName() + ".doubleValue();");
			} else if ("Integer".equals(info.getType())) {
				ps.println("	if (" + info.getVName() + "== null)");
				ps.println("	    return 0;");
				ps.println("	return " + info.getVName() + ".intValue();");
			} else if ("Long".equals(info.getType())) {
				ps.println("	if (" + info.getVName() + "== null)");
				ps.println("	    return 0l;");
				ps.println("	return " + info.getVName() + ".longValue();");
			} else {
				ps.println("	return " + info.getVName() + ';');
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
				ps.print("		if (" + info.getVName() + " != null && " + info.getVName() + ".length() > ");
				ps.println(info.getLength() + "){");
				ps.println("	    " + info.getVName() + " = newVal.substring(0," + (info.getLength() - 1) + ");");
				ps.println("	} else {");
				ps.println("	    " + info.getVName() + " = newVal;");
				ps.println("	}");
			} else {
				ps.println("	" + info.getVName() + " = newVal;");
			}
			ps.println("	}");
			ps.println("");
		}

	}

	/**
	 * Entry point for this app
	 * 
	 * @param args
	 * @noinspection OverlyNestedMethod
	 */
	public static void main(String[] args) {
		try {
			Db db = new Db(propKey + ".main()", propKey);
			String dbName = db.getDbName();
			int i = 0;
			for (; i < args.length; i++) {
				GenSpring obj = new GenSpring();
				if (args[i].length() > 0 && args[i].charAt(0) == '-') {
					if ("-double".equals(args[i])) {
						obj.useDouble = true;
					}
				} else if (args[i].length() > 0 && args[i].charAt(0) == '-') {
					if ("-toString".equals(args[i])) {
						obj.beanToString = true;
					}
				}
			}
			if (args.length >= i) {
				String query = "SHOW TABLES"; // mySQL
				if (db.getDbUrl().indexOf("sqlserver") > -1) {
					query = "SELECT NAME,INFO FROM sysobjects WHERE type= 'U'";
				} else if (db.getDbUrl().indexOf("sqlite") > -1) {
					query = "SELECT name FROM sqlite_master WHERE type='table';";
				}
				Connection conn = db.getConnection(propKey + ".main()");
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
				List<String> tableNames = new ArrayList<String>();
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
							if (!name.startsWith("old_") && !name.startsWith("sqlite_sequence")
									&& !name.startsWith("Providers")) {
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
						LOGGER.error("main() crashed ", e);
					}
				}
				db.close(propKey + ".main()");
				GenSpring obj = new GenSpring();
				obj.genProject(tableNames);
			}
		} catch (Exception e) {
			LOGGER.error("main() crashed  ", e);
		}

		LOGGER.info("Done");
	}
}
