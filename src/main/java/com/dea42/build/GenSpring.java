package com.dea42.build;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

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
	public static final String propKey = "genSpring";

	private boolean useDouble = false;
	private boolean beanToString = false;
	private ResourceBundle bundle = null;
	private ResourceBundle renames = ResourceBundle.getBundle("rename");
	private String basePkg;
	private String baseDir;
	private int year = 2001;
	private String schema = null;

	public GenSpring() throws IOException {
		GregorianCalendar gc = new GregorianCalendar();
		year = gc.get(Calendar.YEAR);

		bundle = ResourceBundle.getBundle(propKey);
		baseDir = Utils.getProp(bundle, propKey + ".outdir");
		schema = Utils.getProp(bundle, propKey + ".schema");
		basePkg = Utils.getProp(bundle, propKey + ".pkg") + '.' + Utils.getProp(bundle, propKey + ".module");
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
	private void genIndex(List<String> clsNames) {
		Path p = createFile("/src/main/resources/templates/index.html");
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
				ps.println("<!DOCTYPE html>");
				ps.println("<html lang=\"en\" xmlns:th=\"http://www.thymeleaf.org\">");
				ps.println("<head>");
				ps.println("    <meta charset=\"UTF-8\"/>");
				ps.println("    <title>watchlist | Home</title>");
				ps.println("</head>");
				ps.println("<body>");
				for (String clsName : clsNames) {
					String fieldName = clsName.substring(0, 1).toLowerCase() + clsName.substring(1);
					ps.println("    	<a href=\"/" + fieldName + "s\">" + clsName + "</a><br>");
				}
				ps.println("    	<a href=\"/api/\">/api/</a><br>");
				ps.println("    	<a href=\"/login\">Login</a><br>");
				ps.println("</body>");
				ps.println("</html>");
				LOGGER.warn("Wrote:" + p.toString());
			} catch (Exception e) {
				LOGGER.error("failed to create " + p, e);
				p.toFile().delete();
			}
		}
	}

	/**
	 * Generate folder structure and project level files
	 * 
	 * @param tableNames
	 * @throws Exception
	 */
	public void genProject(List<String> tableNames) throws Exception {
		Path p = Paths.get(baseDir + "/src/main/java");
		Files.createDirectories(p);
		p = Paths.get(baseDir + "/src/main/resources/templates");
		Files.createDirectories(p);
		p = Paths.get(baseDir + "/src/test/java");
		Files.createDirectories(p);
		p = Paths.get(baseDir + "/src/test/resources");
		Files.createDirectories(p);

		writePom();
		writeAppProps();
		writeCss();
		writeLoginHtml();
		writeAppController();
		writeWebApp();
		writeSecurityConfiguration();

		List<String> clsNames = new ArrayList<String>();
		for (String tableName : tableNames) {
			clsNames.add(genFiles(tableName));
		}
		writeApiController(clsNames);
		genIndex(clsNames);
		writeWebAppTest();
	}

	/**
	 * Entry point for generating all the files you need for Spring maint screens to
	 * Add/Edit/Delete/Search record for a table.
	 * 
	 * @param tableName
	 * @throws Exception
	 */
	public String genFiles(String tableName) throws Exception {

		String fakePK = null;
		String pkCol = null;
		TreeMap<String, ColInfo> namList = new TreeMap<String, ColInfo>();
		String className = null;
		String create = "";
		HashMap<String, ColInfo> cols = new HashMap<String, ColInfo>(100);

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
				LOGGER.info("storing:" + info.getVName());
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
		writeEditPage(className, namList, pkinfo);

		return className;
	}

	private void writeApiController(List<String> clsNames) {
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
				for (String clsName : clsNames) {
					ps.println("import " + basePkg + ".entity." + clsName + ";");
					ps.println("import " + basePkg + ".service." + clsName + "Services;");
				}
				ps.println("");
				ps.println("import java.util.List;");
				ps.println("");
				ps.println("@RestController");
				ps.println("@RequestMapping(\"/api\")");
				ps.println("public class ApiController {");
				ps.println("");
				for (String clsName : clsNames) {
					String fieldName = clsName.substring(0, 1).toLowerCase() + clsName.substring(1);
					ps.println("    @Autowired");
					ps.println("    private " + clsName + "Services " + fieldName + "Services;");
				}
				ps.println("");
				ps.println("    public ApiController(){");// + clsName + "Services " + fieldName + "Services){");
//			ps.println("        super();");
//			ps.println("        this." + fieldName + "Services = " + fieldName + "Services;");
				ps.println("    }");
				for (String clsName : clsNames) {
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

	private void writeSecurityConfiguration() {
		String pkgNam = basePkg;
		String relPath = pkgNam.replace('.', '/');
		String outFile = "/src/main/java/" + relPath + "/SecurityConfiguration.java";
		Path p = createFile(outFile);
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
				ps.println("package " + pkgNam + ';');
				ps.println("");
				ps.println("import org.springframework.beans.factory.annotation.Autowired;");
				ps.println("import org.springframework.context.annotation.Configuration;");
				ps.println(
						"import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;");
				ps.println("import org.springframework.security.config.annotation.web.builders.HttpSecurity;");
				ps.println(
						"import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;");
				ps.println(
						"import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;");
				ps.println("import org.springframework.security.crypto.password.NoOpPasswordEncoder;");
				ps.println("");
				ps.println("@Configuration");
				ps.println("@EnableWebSecurity");
				ps.println("public class SecurityConfiguration extends WebSecurityConfigurerAdapter{");
				ps.println("    @Override");
				ps.println("    protected void configure(HttpSecurity http) throws Exception {");
				ps.println("        http.authorizeRequests().antMatchers(\"/\", \"/api/*\").permitAll()");
				ps.println("                .anyRequest().authenticated()");
				ps.println("                .and()");
				ps.println("                .formLogin()");
				ps.println("                .loginPage(\"/login\")");
				ps.println("                .permitAll()");
				ps.println("                .and()");
				ps.println("                .logout()");
				ps.println("                .permitAll();");
				ps.println("    }");
				ps.println("");
				ps.println("    @Autowired");
				ps.println("    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception{");
				ps.println("		//TODO: replace this with real auth system");
				ps.println("        auth.inMemoryAuthentication()");
				ps.println("                .passwordEncoder(NoOpPasswordEncoder.getInstance())");
				ps.println("                .withUser(\"user\").password(\"password\").roles(\"USER\");");
				ps.println("    }");
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
	 * Create main web app starter class
	 */
	private void writeWebApp() {
		String pkgNam = basePkg;
		String relPath = pkgNam.replace('.', '/');
		String outFile = "/src/main/java/" + relPath + "/WebAppApplication.java";
		Path p = createFile(outFile);
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
				ps.println("package " + pkgNam + ';');
				ps.println("");
				ps.println("import org.springframework.boot.SpringApplication;");
				ps.println("import org.springframework.boot.autoconfigure.SpringBootApplication;");
				ps.println("");
				ps.println("@SpringBootApplication");
				ps.println("public class WebAppApplication {");
				ps.println("");
				ps.println("	public static void main(String[] args) {");
				ps.println("		SpringApplication.run(WebAppApplication.class, args);");
				ps.println("	}");
				ps.println("}");
				LOGGER.warn("Wrote:" + p.toString());
			} catch (Exception e) {
				LOGGER.error("failed to create " + p, e);
				p.toFile().delete();
			}
		}
	}

	private void writeWebAppTest() {
		String pkgNam = basePkg;
		String relPath = pkgNam.replace('.', '/');
		String outFile = "/src/test/java/" + relPath + "/WebAppApplicationTest.java";
		Path p = createFile(outFile);
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
				ps.println("package " + pkgNam + ';');
				ps.println("");
				ps.println("import org.junit.Test;");
				ps.println("import org.junit.runner.RunWith;");
				ps.println("import org.springframework.boot.test.context.SpringBootTest;");
				ps.println("import org.springframework.test.context.junit4.SpringRunner;");
				ps.println("");
				ps.println("@RunWith(SpringRunner.class)");
				ps.println("@SpringBootTest");
				ps.println("public class WebAppApplicationTest {");
				ps.println("");
				ps.println("	/**");
				ps.println("	* Quick test that build works and config loads");
				ps.println("	*/");
				ps.println("	@Test");
				ps.println("	public void contextLoads() {");
				ps.println("	}");
				ps.println("}");
				LOGGER.warn("Wrote:" + p.toString());
			} catch (Exception e) {
				LOGGER.error("failed to create " + p, e);
				p.toFile().delete();
			}
		}
	}

	/**
	 * create login controller
	 */
	private void writeAppController() {
		String pkgNam = basePkg + ".controller";
		String relPath = pkgNam.replace('.', '/');
		String outFile = "/src/main/java/" + relPath + "/AppController.java";
		Path p = createFile(outFile);
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
				ps.println("package " + pkgNam + ';');
				ps.println("");
				ps.println("import org.springframework.stereotype.Controller;");
				ps.println("import org.springframework.web.bind.annotation.GetMapping;");
				ps.println("");
				ps.println("@Controller");
				ps.println("public class AppController {");
				ps.println("");
				ps.println("    @GetMapping(\"/login\")");
				ps.println("    public String getLoginPage(){");
				ps.println("        return \"login\";");
				ps.println("    }");
				ps.println("");
				ps.println("    @GetMapping(\"/\")");
				ps.println("    public String getIndex(){");
				ps.println("        return \"index\";");
				ps.println("    }");
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
	 * gen simple css file for site
	 */
	private void writeCss() {
		Path p = createFile("/src/main/resources/static/css/site.css");
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
				ps.println("body{");
				ps.println("    background-color: gray;");
				ps.println("}");
				LOGGER.warn("Wrote:" + p.toString());
			} catch (Exception e) {
				LOGGER.error("failed to create " + p, e);
				p.toFile().delete();
			}
		}
	}

	/**
	 * Create basic login page
	 */
	private void writeLoginHtml() {
		Path p = createFile("/src/main/resources/templates/login.html");
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
				ps.println("<!DOCTYPE html>");
				ps.println("<html lang=\"en\" xmlns:th=\"http://www.thymeleaf.org\">");
				ps.println("<head>");
				ps.println("    <meta charset=\"UTF-8\"/>");
				ps.println("    <title>" + Utils.getProp(bundle, propKey + ".module") + " | Login</title>");
				ps.println("</head>");
				ps.println("<body>");
				ps.println("    <form th:action=\"@{/login}\" method=\"post\">");
				ps.println("        <div>");
				ps.println("            <label>User Name: <input type=\"text\" name=\"username\"/></label>");
				ps.println("            <label>Password: <input type=\"password\" name=\"password\"/></label>");
				ps.println("        </div>");
				ps.println("        <div><input type=\"submit\" value=\"Login\"/></div>");
				ps.println("    </form>");
				ps.println("</body>");
				ps.println("</html>");
				LOGGER.warn("Wrote:" + p.toString());
			} catch (Exception e) {
				LOGGER.error("failed to create " + p, e);
				p.toFile().delete();
			}
		}
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
				ps.println("<!DOCTYPE html>");
				ps.println("<html xmlns=\"http://www.w3.org/1999/xhtml\"");
				ps.println("	xmlns:th=\"http://www.thymeleaf.org\">");
				ps.println("<head>");
				ps.println("<meta charset=\"utf-8\" />");
				ps.println("<title>Edit " + clsName + "</title>");
				ps.println("</head>");
				ps.println("<body>");
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
				ps.println("			<table border=\"1\" cellpadding=\"10\">");
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
				ps.println("</body>");
				ps.println("</html>");
				LOGGER.warn("Wrote:" + p.toString());
			} catch (Exception e) {
				LOGGER.error("failed to create " + p, e);
				p.toFile().delete();
			}
		}
	}

	private void writeListPage(String clsName, TreeMap<String, ColInfo> namList, ColInfo pkinfo) {
		String fieldName = clsName.substring(0, 1).toLowerCase() + clsName.substring(1);
		String outFile = "/src/main/resources/templates/" + fieldName + "s.html";
		Set<String> set = namList.keySet();
		List<String> listCols = Utils.getPropList(bundle, clsName + ".list");

		Path p = createFile(outFile);
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
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
								throw new NullPointerException(name + " in " + clsName
										+ ".list columns but no info found for it in " + namList);
							}
						}
					}
				}
				ps.println("<!DOCTYPE html>");
				ps.println("<html lang=\"en\" xmlns:th=\"http://www.thymeleaf.org\">");
				ps.println("<head>");
				ps.println("    <meta charset=\"UTF-8\"/>");
				ps.println("    <title>" + Utils.getProp(bundle, propKey + ".module") + " | " + clsName
						+ " List View</title>");
				ps.println("    <link th:href=\"@{/css/site.css}\" rel=\"stylesheet\"/>");
				ps.println("</head>");
				ps.println("<body>");
				ps.println("	<div align=\"center\">");
				ps.println("		<h1>" + clsName + " List</h1>");
				ps.println("		<a href=\"/" + fieldName + "s/new\">Create New " + clsName + "</a> <br />");
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
								+ "}}\"><span th:text=\"${" + fieldName + "." + info.getVName() + "}\"></span></a></td>");

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
				ps.println("</body>");
				ps.println("</html>");
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
				ps.println("import com.dea42.watchlist.entity." + clsName + ";");
				ps.println("import com.dea42.watchlist.service." + clsName + "Services;");
				ps.println("");
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
					String pkgNam = basePkg + ".db";
					ps.println("## SQLite");
					ps.println("spring.jpa.database-platform=" + pkgNam + ".SQLiteDialect");
					ps.println("spring.datasource.driver-class-name = org.sqlite.JDBC");
					writeSQLiteDialect(pkgNam);
				} else {
					ps.println("#spring.datasource.url=jdbc:mysql://localhost:3306/watchlist");
					ps.println("#spring.datasource.username=" + Utils.getProp(bundle, "user", null));
					ps.println("#spring.datasource.password=" + Utils.getProp(bundle, "password", null));
				}
				ps.println("spring.datasource.url=" + dbUrl);
				ps.println("");
				ps.println("logging.level.root=INFO");
				LOGGER.warn("Wrote:" + p.toString());
			} catch (Exception e) {
				LOGGER.error("failed to create " + p, e);
				p.toFile().delete();
			}
		}
	}

	private void writeSQLiteDialect(String pkgNam) {
		String relPath = pkgNam.replace('.', '/');
		String outFile = "/src/main/java/" + relPath + "/SQLiteDialect.java";
		Path p = createFile(outFile);
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
				ps.println("package " + pkgNam + ";");
				ps.println("");
				ps.println("import java.sql.Types;");
				ps.println("");
				ps.println("import org.hibernate.dialect.Dialect;");
				ps.println("import org.hibernate.dialect.function.SQLFunctionTemplate;");
				ps.println("import org.hibernate.dialect.function.StandardSQLFunction;");
				ps.println("import org.hibernate.dialect.function.VarArgsSQLFunction;");
				ps.println("import org.hibernate.type.StringType;");
				ps.println("");
				ps.println("public class SQLiteDialect extends Dialect {");
				ps.println("");
				ps.println("    public SQLiteDialect() {");
				ps.println("        registerColumnType(Types.BIT, \"integer\");");
				ps.println("        registerColumnType(Types.TINYINT, \"tinyint\");");
				ps.println("        registerColumnType(Types.SMALLINT, \"smallint\");");
				ps.println("        registerColumnType(Types.INTEGER, \"integer\");");
				ps.println("        registerColumnType(Types.BIGINT, \"bigint\");");
				ps.println("        registerColumnType(Types.FLOAT, \"float\");");
				ps.println("        registerColumnType(Types.REAL, \"real\");");
				ps.println("        registerColumnType(Types.DOUBLE, \"double\");");
				ps.println("        registerColumnType(Types.NUMERIC, \"numeric\");");
				ps.println("        registerColumnType(Types.DECIMAL, \"decimal\");");
				ps.println("        registerColumnType(Types.CHAR, \"char\");");
				ps.println("        registerColumnType(Types.VARCHAR, \"varchar\");");
				ps.println("        registerColumnType(Types.LONGVARCHAR, \"longvarchar\");");
				ps.println("        registerColumnType(Types.DATE, \"date\");");
				ps.println("        registerColumnType(Types.TIME, \"time\");");
				ps.println("        registerColumnType(Types.TIMESTAMP, \"timestamp\");");
				ps.println("        registerColumnType(Types.BINARY, \"blob\");");
				ps.println("        registerColumnType(Types.VARBINARY, \"blob\");");
				ps.println("        registerColumnType(Types.LONGVARBINARY, \"blob\");");
				ps.println("        // registerColumnType(Types.NULL, \"null\");");
				ps.println("        registerColumnType(Types.BLOB, \"blob\");");
				ps.println("        registerColumnType(Types.CLOB, \"clob\");");
				ps.println("        registerColumnType(Types.BOOLEAN, \"integer\");");
				ps.println("");
				ps.println(
						"        registerFunction(\"concat\", new VarArgsSQLFunction(StringType.INSTANCE, \"\", \"||\", \"\"));");
				ps.println(
						"        registerFunction(\"mod\", new SQLFunctionTemplate(StringType.INSTANCE, \"?1 % ?2\"));");
				ps.println(
						"        registerFunction(\"substr\", new StandardSQLFunction(\"substr\", StringType.INSTANCE));");
				ps.println(
						"        registerFunction(\"substring\", new StandardSQLFunction(\"substr\", StringType.INSTANCE));");
				ps.println("    }");
				ps.println("");
				ps.println("    public boolean supportsIdentityColumns() {");
				ps.println("        return true;");
				ps.println("    }");
				ps.println("");
				ps.println("    /*");
				ps.println("  public boolean supportsInsertSelectIdentity() {");
				ps.println("    return true; // As specify in NHibernate dialect");
				ps.println("  }");
				ps.println("     */");
				ps.println("    public boolean hasDataTypeInIdentityColumn() {");
				ps.println("        return false; // As specify in NHibernate dialect");
				ps.println("    }");
				ps.println("");
				ps.println("    /*");
				ps.println("  public String appendIdentitySelectToInsert(String insertString) {");
				ps.println(
						"    return new StringBuffer(insertString.length()+30). // As specify in NHibernate dialect");
				ps.println("      append(insertString).");
				ps.println("      append(\"; \").append(getIdentitySelectString()).");
				ps.println("      toString();");
				ps.println("  }");
				ps.println("     */");
				ps.println("    public String getIdentityColumnString() {");
				ps.println("        // return \"integer primary key autoincrement\";");
				ps.println("        return \"integer\";");
				ps.println("    }");
				ps.println("");
				ps.println("    public String getIdentitySelectString() {");
				ps.println("        return \"select last_insert_rowid()\";");
				ps.println("    }");
				ps.println("");
				ps.println("    public boolean supportsLimit() {");
				ps.println("        return true;");
				ps.println("    }");
				ps.println("");
				ps.println("    protected String getLimitString(String query, boolean hasOffset) {");
				ps.println("        return new StringBuffer(query.length() + 20).");
				ps.println("                append(query).");
				ps.println("                append(hasOffset ? \" limit ? offset ?\" : \" limit ?\").");
				ps.println("                toString();");
				ps.println("    }");
				ps.println("");
				ps.println("    public boolean supportsTemporaryTables() {");
				ps.println("        return true;");
				ps.println("    }");
				ps.println("");
				ps.println("    public String getCreateTemporaryTableString() {");
				ps.println("        return \"create temporary table if not exists\";");
				ps.println("    }");
				ps.println("");
				ps.println("    public boolean dropTemporaryTableAfterUse() {");
				ps.println("        return false;");
				ps.println("    }");
				ps.println("");
				ps.println("    public boolean supportsCurrentTimestampSelection() {");
				ps.println("        return true;");
				ps.println("    }");
				ps.println("");
				ps.println("    public boolean isCurrentTimestampSelectStringCallable() {");
				ps.println("        return false;");
				ps.println("    }");
				ps.println("");
				ps.println("    public String getCurrentTimestampSelectString() {");
				ps.println("        return \"select current_timestamp\";");
				ps.println("    }");
				ps.println("");
				ps.println("    public boolean supportsUnionAll() {");
				ps.println("        return true;");
				ps.println("    }");
				ps.println("");
				ps.println("    public boolean hasAlterTable() {");
				ps.println("        return false; // As specify in NHibernate dialect");
				ps.println("    }");
				ps.println("");
				ps.println("    public boolean dropConstraints() {");
				ps.println("        return false;");
				ps.println("    }");
				ps.println("");
				ps.println("    public String getAddColumnString() {");
				ps.println("        return \"add column\";");
				ps.println("    }");
				ps.println("");
				ps.println("    public String getForUpdateString() {");
				ps.println("        return \"\";");
				ps.println("    }");
				ps.println("");
				ps.println("    public boolean supportsOuterJoinForUpdate() {");
				ps.println("        return false;");
				ps.println("    }");
				ps.println("");
				ps.println("    public String getDropForeignKeyString() {");
				ps.println(
						"        throw new UnsupportedOperationException(\"No drop foreign key syntax supported by SQLiteDialect\");");
				ps.println("    }");
				ps.println("");
				ps.println("    public String getAddForeignKeyConstraintString(String constraintName,");
				ps.println("            String[] foreignKey, String referencedTable, String[] primaryKey,");
				ps.println("            boolean referencesPrimaryKey) {");
				ps.println(
						"        throw new UnsupportedOperationException(\"No add foreign key syntax supported by SQLiteDialect\");");
				ps.println("    }");
				ps.println("");
				ps.println("    public String getAddPrimaryKeyConstraintString(String constraintName) {");
				ps.println(
						"        throw new UnsupportedOperationException(\"No add primary key syntax supported by SQLiteDialect\");");
				ps.println("    }");
				ps.println("");
				ps.println("    public boolean supportsIfExistsBeforeTableName() {");
				ps.println("        return true;");
				ps.println("    }");
				ps.println("");
				ps.println("    public boolean supportsCascadeDelete() {");
				ps.println("        return false;");
				ps.println("    }");
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
	 * create pom.xml
	 */
	private void writePom() {
		String dbUrl = Utils.getProp(bundle, "db.url", "");
		Path p = createFile("/pom.xml");
		if (p != null) {
			try (PrintStream ps = new PrintStream(p.toFile())) {
				ps.println("<project xmlns=\"http://maven.apache.org/POM/4.0.0\"");
				ps.println("	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
				ps.println("	xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 ");
				ps.println("		http://maven.apache.org/xsd/maven-4.0.0.xsd\">");
				ps.println("	<modelVersion>4.0.0</modelVersion>");
				ps.println("	<groupId>" + Utils.getProp(bundle, propKey + ".pkg") + "</groupId>");
				ps.println("	<artifactId>" + Utils.getProp(bundle, propKey + ".module") + "</artifactId>");
				ps.println("	<version>0.0.1-SNAPSHOT</version>");
				ps.println("	<packaging>jar</packaging>");
				ps.println("");
				ps.println("	<parent>");
				ps.println("		<groupId>org.springframework.boot</groupId>");
				ps.println("		<artifactId>spring-boot-starter-parent</artifactId>");
				ps.println("		<version>2.1.3.RELEASE</version>");
				ps.println("	</parent>");
				ps.println("");
				ps.println("	<dependencies>");
				ps.println("		<dependency>");
				ps.println("			<groupId>org.springframework.boot</groupId>");
				ps.println("			<artifactId>spring-boot-starter-web</artifactId>");
				ps.println("		</dependency>");
				ps.println("		<dependency>");
				ps.println("			<groupId>org.springframework.boot</groupId>");
				ps.println("			<artifactId>spring-boot-starter-data-jpa</artifactId>");
				ps.println("		</dependency>");
				ps.println("		<dependency>");
				ps.println("			<groupId>org.springframework.boot</groupId>");
				ps.println("			<artifactId>spring-boot-starter-thymeleaf</artifactId>");
				ps.println("		</dependency>");
				ps.println("		<dependency>");
				ps.println("			<groupId>com.fasterxml.jackson.dataformat</groupId>");
				ps.println("			<artifactId>jackson-dataformat-xml</artifactId>");
				ps.println("		</dependency>");
				ps.println("		<dependency>");
				ps.println("			<groupId>org.springframework.boot</groupId>");
				ps.println("			<artifactId>spring-boot-starter-security</artifactId>");
				ps.println("		</dependency>");
				ps.println("");
				ps.println("		<dependency>");
				ps.println("    		<groupId>org.springframework.boot</groupId>");
				ps.println("    		<artifactId>spring-boot-devtools</artifactId>");
				ps.println("		</dependency>");

				// TODO: Spring init adds these but are they needed?
				if (dbUrl.contains("sqlite")) {
					ps.println("		<dependency>");
					ps.println("			<groupId>org.xerial</groupId>");
					ps.println("			<artifactId>sqlite-jdbc</artifactId>");
					ps.println("			<scope>runtime</scope>");
					ps.println("		</dependency>");
				} else if (dbUrl.contains("sqlite")) {
					ps.println("		<dependency>");
					ps.println("			<groupId>mysql</groupId>");
					ps.println("			<artifactId>mysql-connector-java</artifactId>");
					ps.println("			<scope>runtime</scope>");
					ps.println("		</dependency>");
				} else if (dbUrl.contains("sqlserver")) {
					ps.println("		<dependency>");
					ps.println("			<groupId>com.microsoft.sqlserver</groupId>");
					ps.println("			<artifactId>mssql-jdbc</artifactId>");
					ps.println("			<scope>runtime</scope>");
					ps.println("		</dependency>");
				} else if (dbUrl.contains("h2")) {
					ps.println("		<dependency>");
					ps.println("			<groupId>com.h2database</groupId>");
					ps.println("			<artifactId>h2</artifactId>");
					ps.println("		</dependency>");
					ps.println("");
				}
				ps.println("		<dependency>");
				ps.println("			<groupId>org.springframework.boot</groupId>");
				ps.println("			<artifactId>spring-boot-starter-test</artifactId>");
				ps.println("			<scope>test</scope>");
				ps.println("		</dependency>");
				ps.println("");
				ps.println("	</dependencies>");
				ps.println("");
				ps.println("	<build>");
				ps.println("		<plugins>");
				ps.println("			<plugin>");
				ps.println("				<groupId>org.springframework.boot</groupId>");
				ps.println("				<artifactId>spring-boot-maven-plugin</artifactId>");
				ps.println("			</plugin>");
				ps.println("		</plugins>");
				ps.println("	</build>");
				ps.println("</project>");
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
