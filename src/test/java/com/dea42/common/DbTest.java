package com.dea42.common;

import static org.junit.Assert.assertEquals;

import java.util.ResourceBundle;

import org.junit.Test;

import com.dea42.build.CommonMethods;

public class DbTest {

	@Test
	public void testDbStringStringString() {
		String bundleName = "genSpringTest";
		ResourceBundle bundle = ResourceBundle.getBundle(bundleName);
		String outdir = Utils.getProp(bundle, CommonMethods.PROPKEY + ".outdir", ".");
		Db db = new Db("Sheet2AppTest", bundleName);
		String cwd = System.getProperty("user.dir").toString().replace('\\', '/');
		int g = cwd.lastIndexOf("/genSpring");
		if (g > 0)
			cwd = cwd.substring(0, g);

		String fileStr = cwd + "/" + bundleName + "/" + bundleName + "DB.sqlite";

		String expected = "jdbc:sqlite:" + fileStr.replace('\\', '/');
		String url = db.getDbUrl();
		assertEquals("Chech db.url empty", expected, url);
	}

}
