/**
 * 
 */
package com.dea42.common;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.tika.parser.txt.CharsetDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author avata
 *
 */
public class Utf8Fix {
	protected static final Logger LOGGER = LoggerFactory.getLogger(Utf8Fix.class.getName());
	private CharsetDetector detector = new CharsetDetector();
	public static final String FRENCH = "français";

	/**
	 * Look for properly UTF8 encoded FRENCH in data String
	 * 
	 * @param label
	 * @param data
	 * @return
	 */
	public boolean chkData(String label, String data) {
		boolean rtn = data.contains(FRENCH);
		if (rtn) {
			LOGGER.info(label + " data looks good");
		} else {
			LOGGER.error(label + " data looks bad");
			int i = data.indexOf("lang.fr =");
			if (i > -1) {
				String fr = data.substring(i + 9, i + 19).trim();
				diffData(FRENCH, fr);
				LOGGER.error(label + " \"" + FRENCH + "\" read as \"" + fr);
			} else {
				LOGGER.error(label + " did not find \"lang.fr =\" in data");
			}
		}

		return rtn;
	}

	/**
	 * List the chars that changed
	 * 
	 * @param orgdata
	 * @param data
	 */
	public void diffData(String orgdata, String data) {

		for (int i = 0; i < orgdata.length(); i++) {
			if (orgdata.charAt(i) != data.charAt(i)) {
				LOGGER.error("char " + i + " changed from " + orgdata.charAt(i) + "(" + (int) orgdata.charAt(i)
						+ ") to " + data.charAt(i) + "(" + (int) data.charAt(i) + ")");
			}
		}
	}

	/**
	 * Try using CharsetDetector to convert file to UTF8
	 * 
	 * @param file
	 */
	public void convert(Path file) {
		try {
			LOGGER.info("File:" + file.toString());
			String orgdata = new String(Files.readAllBytes(file));
			chkData("Orginal file read with Files.readAllBytes(file)", orgdata);
			detector.setText(orgdata.getBytes());
			LOGGER.info("Current encoding is:" + detector.detect());

			String data = detector.getString(orgdata.getBytes(), "utf-8");
			detector.setText(data.getBytes());
			LOGGER.info("Updated encoding is:" + detector.detect());
			if (!chkData("Converted with CharsetDetector", data)) {
				diffData(orgdata, data);
			} else {
				LOGGER.info("Data was not changed.");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Utf8Fix u = new Utf8Fix();
		for (String arg : args) {
			u.convert(new File(arg).toPath());
		}

	}

}
