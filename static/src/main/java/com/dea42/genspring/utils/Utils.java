package com.dea42.genspring.utils;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {
	private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class.getName());

	/**
	 * Checks if Ajax request
	 * 
	 * @param requestedWith
	 * @return
	 */
	public static boolean isAjaxRequest(String requestedWith) {
		return requestedWith != null ? "XMLHttpRequest".equals(requestedWith) : false;
	}

	/**
	 * 
	 * @param bundle
	 * @param key
	 * @return returns bundle value or null if not found
	 */
	public static String getProp(ResourceBundle bundle, String key) {
		return getProp(bundle, key, null);
	}

	/**
	 * Get a comma separated property as a List<String>
	 * 
	 * @param bundle
	 * @param key
	 * @return
	 */
	public static List<String> getPropList(ResourceBundle bundle, String key) {
		String s = getProp(bundle, key, null);
		if (StringUtils.isBlank(s))
			return null;

		return Arrays.asList(s.split("\\s*,\\s*"));
	}

	/**
	 * 
	 * @param bundle
	 * @param key
	 * @param defaultVal
	 * @return returns bundle value or defaultVal if not found
	 */
	public static String getProp(ResourceBundle bundle, String key, String defaultVal) {
		try {
			String val = bundle.getString(key);
			return new String(val.getBytes("ISO-8859-1"), "UTF-8");
		} catch (MissingResourceException | UnsupportedEncodingException e) {
			LOGGER.warn(key + " undefined in " + bundle.getBaseBundleName() + " using " + defaultVal);
		}

		return defaultVal;
	}

	public static int getProp(ResourceBundle bundle, String key, int defaultVal) {
		try {
			return Integer.parseInt(bundle.getString(key));
		} catch (MissingResourceException e) {
			LOGGER.warn(key + " undefined in " + bundle.getBaseBundleName() + " using " + defaultVal);
		}

		return defaultVal;
	}

	public static boolean getProp(ResourceBundle bundle, String key, boolean defaultVal) {
		try {
			return Boolean.parseBoolean(bundle.getString(key));
		} catch (MissingResourceException e) {
			LOGGER.warn(key + " undefined in " + bundle.getBaseBundleName() + " using " + defaultVal);
		}

		return defaultVal;
	}

}
