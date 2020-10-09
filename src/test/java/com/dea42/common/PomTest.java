/**
 * 
 */
package com.dea42.common;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

import com.dea42.build.CommonMethods;

/**
 * 
 * @author avata
 *
 */
public class PomTest {

	/**
	 * A quick check the pom.xml is in sync with genSpringVersion
	 * 
	 * @throws IOException
	 */
	@Test
	public void test() throws IOException {
		Path file = Utils.getPath("pom.xml");
		String data = new String(Files.readAllBytes(file));
		assertTrue("Checking pom.xml version is " + CommonMethods.genSpringVersion,
				data.contains("<version>" + CommonMethods.genSpringVersion + "-SNAPSHOT</version>"));
	}

}
