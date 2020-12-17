/**
 * 
 */
package com.dea42.build;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.ResourceBundle;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.dea42.common.Db;
import com.dea42.common.Utils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Common initialization of common variables use by the GenSpring programs
 * 
 * @author avata
 *
 */
@Data
@Slf4j
public class CommonMethods {
	public static final String PROPKEY = "genSpring";
	public static String ACCOUNT_CLASS = "Account";
	public static String ACCOUNT_TABLE = "Account";
	public static String USERID_COLUMN = "userId";
	public static final String ROLE_PREFIX = "ROLE_";
	public static long ONE_DAY_MILS = 86400000l;

	protected long TEST_USER_ID;
	protected String TEST_USER;
	protected String TEST_PASS;
	protected String TEST_ROLE;

	protected long ADMIN_USER_ID;
	protected String ADMIN_USER;
	protected String ADMIN_PASS;
	protected String ADMIN_ROLE;

	// read from pom.xml
	protected String srcId;
	protected String srcGroupId;
	protected String srcArtifactId;
	protected String genSpringVersion;
	protected String srcPkg;
	protected String srcPath;

	protected boolean useDouble = false;
	protected boolean beanToString = false;
	private String bundelName;
	protected ResourceBundle bundle;
	protected ResourceBundle renames;
	protected String baseGroupId;
	protected String baseArtifactId;
	protected String baseModule;
	protected String basePkg;
	protected String basePath;
	protected String baseDir;
	protected String appVersion;
	protected String appName;
	protected String appDescription;
	protected int year = 2001;
	protected String schema = null;
	protected List<String> filteredTables;
	protected String colCreated;
	protected String colLastMod;
	// TODO: make this configurable and add to PasswordConstraintValidator
	protected int maxPassLen = 30;
	protected int tomcatPort = 8089;

	protected Db db;
	protected boolean isSQLite = false;
	protected String idCls = "Long";
	protected String idparse = "parseLong";
	protected String idPrim = "long";
	protected String idMod = "l";
	protected String idField = USERID_COLUMN;
	

	private void getPomValues() throws IOException {
		MavenXpp3Reader reader = new MavenXpp3Reader();
		try {
			Path p = Utils.getPath("pom.xml");
			Model model = reader.read(new FileReader(p.toFile()));
			srcId = model.getId();
			srcGroupId = model.getGroupId();
			srcArtifactId = model.getArtifactId();
			genSpringVersion = model.getVersion();
			if (genSpringVersion.endsWith("-SNAPSHOT"))
				genSpringVersion = genSpringVersion.substring(0, genSpringVersion.length() - 9);
			srcPkg = srcGroupId + '.' + srcArtifactId;
			srcPath = srcPkg.replace('.', '/');
		} catch (FileNotFoundException | XmlPullParserException e) {
			throw new IOException(e);
		}

	}

	protected void initVars(String bundleName) throws Exception {
		GregorianCalendar gc = new GregorianCalendar();
		year = gc.get(Calendar.YEAR);
		getPomValues();
		setBundelName(bundleName);
		try {
			log.debug("loading:" + bundleName);
			bundle = ResourceBundle.getBundle(bundleName);
		} catch (Exception e) {
			log.error("Failed to read:" + bundleName, e);
			throw e;
		}
		try {
			renames = ResourceBundle.getBundle("rename");
		} catch (Exception e) {
			log.error("Failed to read:rename", e);
			throw e;
		}
		tomcatPort = Utils.getProp(bundle, PROPKEY + ".tomcatPort", tomcatPort);
		baseDir = Utils.getProp(bundle, PROPKEY + ".outdir", "target");
		schema = Utils.getProp(bundle, PROPKEY + ".schema");
		baseGroupId = Utils.getProp(bundle, PROPKEY + ".pkg", srcGroupId);
		baseModule = Utils.getProp(bundle, PROPKEY + ".module", srcArtifactId);
		baseArtifactId = Utils.getProp(bundle, PROPKEY + ".artifactId", baseModule);
		basePkg = baseGroupId + '.' + baseModule;
		basePath = basePkg.replace('.', '/');
		appVersion = Utils.getProp(bundle, PROPKEY + ".version", genSpringVersion);
		appName = Utils.getProp(bundle, "app.name", "GenSpring");
		appDescription = Utils.getProp(bundle, "app.description", "");
		beanToString = Utils.getProp(bundle, PROPKEY + ".beanToString", beanToString);
		useDouble = Utils.getProp(bundle, PROPKEY + ".useDouble", useDouble);
		idField = Utils.getProp(bundle, PROPKEY + ".idField", USERID_COLUMN);
		colCreated = Utils.tabToStr(renames, (String) Utils.getProp(bundle, "col.created", null));
		colLastMod = Utils.tabToStr(renames, (String) Utils.getProp(bundle, "col.lastMod", null));

		filteredTables = Utils.getPropList(bundle, PROPKEY + ".filteredTables");
		// SQLite tables to always ignore
		filteredTables.add("hibernate_sequence");
		filteredTables.add("sqlite_sequence");

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

		db = new Db(".initVars()", bundleName);
		if (db.isSQLite()) {
			isSQLite = true;
			idCls = db.getIdTypeCls().getSimpleName();
			idPrim = db.getIdTypePrim();
			idMod = db.getIdTypeMod();
			idparse = "parseInt";
		}

	}

}
