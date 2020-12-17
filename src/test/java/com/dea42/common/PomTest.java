/**
 * 
 */
package com.dea42.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

import com.dea42.build.Java2VM;

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
	public void test() throws Exception {
		Path file = Utils.getPath("pom.xml");
		String data = new String(Files.readAllBytes(file));
		Java2VM cm = new Java2VM("genSpringTest");
		assertTrue("Checking pom.xml groupId is " + cm.getSrcGroupId(),
				data.contains("<groupId>" + cm.getSrcGroupId() + "</groupId>"));
		assertTrue("Checking pom.xml artifactId is " + cm.getSrcArtifactId(),
				data.contains("<artifactId>" + cm.getSrcArtifactId() + "</artifactId>"));
		assertTrue("Checking pom.xml version is " + cm.getGenSpringVersion(),
				data.contains("<version>" + cm.getGenSpringVersion() + "-SNAPSHOT</version>"));
		assertEquals("Checking pom.xml id", cm.getSrcId(),
				cm.getSrcGroupId() + ":" + cm.getSrcArtifactId() + ":jar:" + cm.getGenSpringVersion() + "-SNAPSHOT");
	}

}
