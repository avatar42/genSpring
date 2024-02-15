package com.dea42.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.SQLException;

import org.junit.jupiter.api.Test;

public class DbTest {

	@Test
	public void testDbStringStringString() throws SQLException {
		String bundleName = "genSpringTest";
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
