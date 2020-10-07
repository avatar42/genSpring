/**
 * 
 */
package com.dea42.build;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import com.dea42.common.Db;
import com.dea42.common.Utils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Converts Java files to and from Velocity templates
 * 
 * @author avata
 *
 */
@Slf4j
@Data
public class Java2VM {
	public static final String RESOURCE_FOLDER = "src/main/resources";
	public static final String TEMPLATE_ROOT = "base";
	public static final String TEMPLATE_FOLDER = RESOURCE_FOLDER + "/" + TEMPLATE_ROOT;

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

	private String idCls = "Long";
	private String idPrim = "long";
	private String idMod = "l";

	private VelocityContext context;

	public Java2VM(String bundleName) throws IOException {
		this.bundleName = bundleName;
		initVars();
	}

	protected void initVars() throws IOException {
		GregorianCalendar gc = new GregorianCalendar();
		year = gc.get(Calendar.YEAR);

		bundle = ResourceBundle.getBundle(bundleName);
		renames = ResourceBundle.getBundle("rename");
		baseDir = Utils.getProp(bundle, GenSpring.PROPKEY + ".outdir");
		schema = Utils.getProp(bundle, GenSpring.PROPKEY + ".schema");
		baseGroupId = Utils.getProp(bundle, GenSpring.PROPKEY + ".pkg");
		baseModule = Utils.getProp(bundle, GenSpring.PROPKEY + ".module");
		baseArtifactId = Utils.getProp(bundle, GenSpring.PROPKEY + ".artifactId", baseModule);
		basePkg = baseGroupId + '.' + baseModule;
		basePath = basePkg.replace('.', '/');
		appVersion = Utils.getProp(bundle, GenSpring.PROPKEY + ".version", "1.0");
		useDouble = Utils.getProp(bundle, GenSpring.PROPKEY + ".useDouble", useDouble);
		colCreated = Utils.tabToStr(renames, (String) Utils.getProp(bundle, "col.created", null));
		colLastMod = Utils.tabToStr(renames, (String) Utils.getProp(bundle, "col.lastMod", null));

		filteredTables = Utils.getPropList(bundle, GenSpring.PROPKEY + ".filteredTables");
		// SQLite tables to always ignore
		filteredTables.add("hibernate_sequence");
		filteredTables.add("sqlite_sequence");

		srcPkg = srcGroupId + '.' + srcArtifactId;
		srcPath = srcPkg.replace('.', '/');

		TEST_USER_ID = Utils.getProp(bundle, "default.userid", 1l);
		TEST_USER = Utils.getProp(bundle, "default.user", "user@dea42.com");
		TEST_PASS = Utils.getProp(bundle, "default.userpass", "ChangeMe");
		TEST_ROLE = Sheets2DB.ROLE_PREFIX + Utils.getProp(bundle, "default.userrole", "USER");
		ADMIN_USER_ID = Utils.getProp(bundle, "default.adminid", 2l);
		ADMIN_USER = Utils.getProp(bundle, "default.admin", "admin@dea42.com");
		ADMIN_PASS = Utils.getProp(bundle, "default.adminpass", "ChangeMe");
		ADMIN_ROLE = Sheets2DB.ROLE_PREFIX + Utils.getProp(bundle, "default.adminrole", "ADMIN");

		Db db = new Db(".initVars()", bundleName, Utils.getProp(bundle, GenSpring.PROPKEY + ".outdir", "."));
		if (db.isSQLite()) {
			idCls = db.getIdTypeCls().getSimpleName();
			idPrim = db.getIdTypePrim();
			idMod = db.getIdTypeMod();
		}

		Properties p = new Properties();
		p.setProperty("resource.loader", "class");
		p.setProperty("class.resource.loader.class",
				"org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
		Velocity.init(p);

		context = new VelocityContext();
		// replace ${className} in file with className value
		context.put("basePkg", basePkg);
		context.put("baseArtifactId", baseArtifactId);
		context.put("idCls", idCls);
		context.put("idPrim", idPrim);
		context.put("idMod", idMod);
		context.put("basePkg", basePkg);
		context.put("baseGroupId", baseGroupId);
		context.put("baseModule", baseModule);
		context.put("genSpringVersion", GenSpring.genSpringVersion);
		context.put("appVersion", appVersion);

	}

	/**
	 * Get resource path relative to RESOURCE_FOLDER. Note automatically deals with
	 * running in target vs top folder.
	 * 
	 * @param path
	 * @param more
	 * @return
	 * @throws IOException
	 */
	public String getResourcePathString(final String path, final String... more) throws IOException {
		// toAbsolutePath() required for getParent() to work
		Path folder = Paths.get(RESOURCE_FOLDER).toAbsolutePath().normalize();
		Path resource = Paths.get(path, more).toAbsolutePath().normalize();
		String fpath = folder.toString().replace('\\', '/').replace("/target/", "/");
		String rpath = resource.toString().replace('\\', '/');
		if (rpath.startsWith(fpath)) {
			return rpath.substring(fpath.length() + 1);
		}

		throw new IOException(fpath + " is not the parent of " + rpath);
	}

	public void vm2java(String file, String relPath) throws IOException {
		Path p = Utils.createFile(baseDir, relPath);
		if (p != null) {
			Writer writer = new FileWriter(p.toFile());
			Velocity.mergeTemplate(getResourcePathString(file), "UTF-8", context, writer);
			writer.flush();
			writer.close();
		}
	}

	public void java2vm(String inpath, String outFile) throws IOException {
		Path file = Paths.get(inpath);
		String data = new String(Files.readAllBytes(file));
		data = data.replace(basePkg, "${basePkg}");
		if (file.getFileName().endsWith("pom.xml")) {
			data = data.replace("<groupId>" + baseGroupId + "</groupId>", "<groupId>${baseGroupId}</groupId>");
			data = data.replace("<artifactId>" + baseArtifactId + "</artifactId>",
					"<artifactId>${baseArtifactId}</artifactId>");
			data = data.replace("<version>" + appVersion + "-SNAPSHOT</version>",
					"<version>${appVersion}-SNAPSHOT</version>");
		} else {
			data = data.replace(baseGroupId, "${baseGroupId}");
			data = data.replace(baseModule, "${baseModule}");
			data = data.replace(GenSpring.genSpringVersion, "${genSpringVersion}");
			data = data.replace("@version " + appVersion + "<br>", "@version ${appVersion}<br>");
//			data = data.replaceAll("(" + idPrim + ") ([A-Z,_]*)_ID;", "\\${idPrim} $2_ID;");
			data = Pattern.compile("(" + idPrim + ") ([A-Z,_]*)_ID;").matcher(data).replaceAll("\\${idPrim} $2_ID;");
			data = Pattern.compile("id\", ([0-9])" + idMod + "\\);").matcher(data).replaceAll("id\", $1\\${idMod}\\);");
			// .setId(0${idMod})
			data = Pattern.compile(".setId\\(([0-9])" + idMod + "\\);").matcher(data)
					.replaceAll(".setId\\($1\\${idMod}\\);");
		}
		Path p = Utils.createFile(baseDir, outFile);
		if (p != null) {
			try {
				Files.write(p, data.getBytes(), StandardOpenOption.APPEND);
				log.warn("Wrote:" + p.toString());
			} catch (Exception e) {
				log.error("failed to create " + p, e);
			}
		}

	}

	/**
	 * regen template files in target/base so can diff and pull in changes
	 * 
	 * @throws IOException
	 */
	public void genTemplates() throws IOException {
		Utils.deletePath(Paths.get("target", "base"));
		Path staticPath = Utils.getPath(TEMPLATE_FOLDER);
		Files.walkFileTree(staticPath, new FileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				return FileVisitResult.CONTINUE;
			}

			/**
			 * Copy file into new tree converting package / paths as needed TODO: change to
			 * use velocityGenerator()
			 */
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				String name = file.getFileName().toString();
				String rfile = getResourcePathString(file.toString()).substring(4);
				if (name.endsWith(".vm")) {
					rfile = rfile.substring(0, rfile.length() - 3);
				}
				String srcFile = getBaseDir() + rfile.replace(getBasePath(), "com/dea42/genspring");
				String outFile = "target/base" + rfile;
				if (name.endsWith(".java") || name.endsWith("pom.xml") || name.endsWith(".vm")) {
					log.info("Converting:" + file);
					outFile = outFile + ".vm";
					java2vm(srcFile, outFile);
				} else if (name.endsWith(".gitignore")) {
					log.info("Skipping:" + file);
				} else {
					log.info("Copying:" + file);
					Path p = Utils.createFile(baseDir, outFile);
					if (p != null) {
						try {
							Files.copy(file, p, StandardCopyOption.REPLACE_EXISTING);
							log.warn("Wrote:" + p.toString());
						} catch (Exception e) {
							log.error("failed to create " + p, e);
						}
					}
				}

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
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			Java2VM j = new Java2VM("genSpringTest");
			j.genTemplates();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
