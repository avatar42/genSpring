package com.dea42.genspring;

import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;

import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import com.dea42.genspring.controller.AppController;
import com.dea42.genspring.utils.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;

import junit.framework.TestCase;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public abstract class UnitBase extends TestCase {
	private static final String strVal = "ABCDEFGHIJKLMNOPQRSTUVWXYZ01234567890abcdefghijklmnopqrstuvwzyz";
	protected static final Logger LOGGER = LoggerFactory.getLogger(UnitBase.class.getName());

	public static final String SEND_GET = "GET";
	public static final String SEND_POST = "POST";

	protected ResourceBundle appBundle;
	protected ResourceBundle msgEnBundle;

	protected String lang = "en";

	protected MockMvc mockMvc;

	protected Set<String> headless = new HashSet<String>();

	protected static String TEST_USER;
	protected static String TEST_PASS;
	protected static String TEST_ROLE;

	protected static String ADMIN_USER;
	protected static String ADMIN_PASS;
	protected static String ADMIN_ROLE;

	public UnitBase() {
		appBundle = ResourceBundle.getBundle("app");
		msgEnBundle = ResourceBundle.getBundle("messages");

		if (TEST_USER == null) {
			TEST_USER = Utils.getProp(appBundle, "default.user", null);
			TEST_PASS = Utils.getProp(appBundle, "default.userpass", null);
			TEST_ROLE = Utils.getProp(appBundle, "default.userrole", null);
		}
		if (ADMIN_USER == null) {
			ADMIN_USER = Utils.getProp(appBundle, "default.admin", null);
			ADMIN_PASS = Utils.getProp(appBundle, "default.adminpass", null);
			ADMIN_ROLE = Utils.getProp(appBundle, "default.adminrole", null);
		}

		headless.add("/login");
		headless.add("/signup");
		headless.add("/authenticate");
		headless.add(AppController.SIGNUP_VIEW_NAME);
		headless.add(AppController.SIGNIN_VIEW_NAME);
		headless.add(AppController.HOME_SIGNED_VIEW_NAME);
		headless.add(AppController.HOME_NOT_SIGNED_VIEW_NAME);

	}

	/**
	 * Create a max length string for testing
	 * 
	 * @param length
	 * @return
	 */
	protected String getTestString(int length) {
		if (length < strVal.length()) {
			return strVal.substring(0, length);
		}

		StringBuilder sb = new StringBuilder(strVal);
		while (sb.length() < length && length < strVal.length() + sb.length()) {
			sb.append(strVal);
			length -= strVal.length();
		}

		if (length < strVal.length() + sb.length()) {
			sb.append(strVal.substring(0, length));
		}

		return sb.toString();
	}

	/**
	 * get the bundle for the current lang
	 * 
	 * @return
	 */
	protected ResourceBundle getMsgBundle() {
		if ("en".equals(lang))
			return msgEnBundle;

		try {
			return ResourceBundle.getBundle("messages_" + lang);
		} catch (Exception e) {
			LOGGER.warn("messages_" + lang + " not found");
			// return en bundle
		}

		return msgEnBundle;
	}

	/**
	 * convenience wrapper for Utils.getProp(getMsgBundle(), key, key)
	 * 
	 * @param key
	 * @return String from props file or key is not found
	 */
	protected String getMsg(String key) {
		return Utils.getProp(getMsgBundle(), key, key);
	}

	public static String asJsonString(final Object obj) {
		try {
			return new ObjectMapper().writeValueAsString(obj);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
