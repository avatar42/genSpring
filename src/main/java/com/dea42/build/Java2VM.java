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
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import com.dea42.common.Utils;

import lombok.extern.slf4j.Slf4j;

/**
 * Converts Java files to and from Velocity templates
 * 
 * @author avata
 *
 */
@Slf4j
public class Java2VM extends CommonMethods {
	public static final String RESOURCE_FOLDER = "src/main/resources";
	public static final String TEMPLATE_ROOT = "base";
	public static final String TEMPLATE_FOLDER = RESOURCE_FOLDER + "/" + TEMPLATE_ROOT;

	private VelocityContext context;

	public Java2VM(String bundleName) throws Exception {
		initVars(bundleName);
	}

	/**
	 * Read config from property files and init needed classes and vars.
	 */
	protected void initVars(String bundleName) throws Exception {
		super.initVars(bundleName);

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
		context.put("idparse", idparse);
		context.put("basePkg", basePkg);
		context.put("baseGroupId", baseGroupId);
		context.put("baseModule", baseModule);
		context.put("genSpringVersion", genSpringVersion);
		context.put("appVersion", appVersion);
		context.put("appName", appName);
		context.put("appDescription", appDescription);
		context.put("thisYear", year);
		context.put("Company", Utils.getProp(bundle, PROPKEY + ".Company", ""));
		context.put("tomcatPort", tomcatPort);

	}

	protected void addContext(String key, String value) {
		context.put(key, value);
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

	/**
	 * Do Velocity.mergeTemplate() on templatefilePathStr outputting to
	 * outFilePathStr, Note if outFilePathStr exists will skip.
	 * 
	 * @param templatefilePathStr
	 * @param outFilePathStr
	 * @throws IOException
	 */
	public void vm2java(String templatefilePathStr, String outFilePathStr) throws IOException {
		Path p = Utils.createFile(baseDir, outFilePathStr);
		if (p != null) {
			log.debug("Converting:" + templatefilePathStr + " to " + p);
			Writer writer = new FileWriter(p.toFile());
			Velocity.mergeTemplate(getResourcePathString(templatefilePathStr), "UTF-8", context, writer);
			writer.flush();
			writer.close();
		} else {
			log.warn(outFilePathStr + " exists. Skipping:" + templatefilePathStr);
		}
	}

	/**
	 * Attempts to convert srcFilePathStr to Velocity template based on know
	 * replacement values and write the converted data to outTemplatefilePathStr
	 * 
	 * @See initVars(String)
	 * 
	 * @param srcFilePathStr
	 * @param outTemplatefilePathStr
	 * @throws IOException
	 */
	public void java2vm(String srcFilePathStr, String outTemplatefilePathStr) throws IOException {
		Path p = Utils.createFile(baseDir, outTemplatefilePathStr);
		if (p != null) {
			Path file = Paths.get(srcFilePathStr);
			String data = new String(Files.readAllBytes(file));
			data = data.replace(basePkg, "${basePkg}");
			if (file.getFileName().endsWith("pom.xml")) {
				data = data.replaceFirst("<groupId>.*?</groupId>", "<groupId>\\${baseGroupId}</groupId>");
				data = data.replaceFirst("<artifactId>.*?</artifactId>",
						"<artifactId>\\${baseArtifactId}</artifactId>");
				data = data.replaceFirst("<version>.*?</version>", "<version>\\${appVersion}-SNAPSHOT</version>");
				data = data.replaceFirst("<description>.*?</description>",
						"<description>\\${appDescription}</description>");
			} else {
				data = data.replace(baseGroupId, "${baseGroupId}");
				data = data.replace(baseModule, "${baseModule}");
				data = data.replace(genSpringVersion, "${genSpringVersion}");
				data = data.replace("@version " + appVersion + "<br>", "@version ${appVersion}<br>");
//			data = data.replaceAll("(" + idPrim + ") ([A-Z,_]*)_ID;", "\\${idPrim} $2_ID;");
				data = Pattern.compile("(" + idPrim + ") ([A-Z,_]*)_ID;").matcher(data)
						.replaceAll("\\${idPrim} $2_ID;");
				data = Pattern.compile("id\", ([0-9])" + idMod + "\\);").matcher(data)
						.replaceAll("id\", $1\\${idMod}\\);");
				// .setId(0${idMod})
				data = Pattern.compile(".setId\\(([0-9])" + idMod + "\\);").matcher(data)
						.replaceAll(".setId\\($1\\${idMod}\\);");
			}
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
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
