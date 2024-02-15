/**
 * 
 */
package com.dea42.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.dea42.build.Java2VM;

/**
 * 
 * @author avata
 *
 */
class PomTest {

	/**
	 * A quick check the pom.xml is in sync with genSpringVersion
	 * 
	 * @throws IOException
	 */
	@Test
	void test() throws Exception {
		Path file = Utils.getPath("pom.xml");
		String data = new String(Files.readAllBytes(file));
		Java2VM cm = new Java2VM("genSpringTest");
		assertTrue(data.contains("<groupId>" + cm.getSrcGroupId() + "</groupId>"),
				"Checking pom.xml groupId is " + cm.getSrcGroupId());
		assertTrue(data.contains("<artifactId>" + cm.getSrcArtifactId() + "</artifactId>"),
				"Checking pom.xml artifactId is " + cm.getSrcArtifactId());
		assertTrue(data.contains("<version>" + cm.getGenSpringVersion() + "-SNAPSHOT</version>"),
				"Checking pom.xml version is " + cm.getGenSpringVersion());
		assertEquals(cm.getSrcId(),
				cm.getSrcGroupId() + ":" + cm.getSrcArtifactId() + ":jar:" + cm.getGenSpringVersion() + "-SNAPSHOT",
				"Checking pom.xml id");
	}

}
