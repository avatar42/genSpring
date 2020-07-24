/**
 * 
 */
package com.dea42.genspring;

import static org.hamcrest.CoreMatchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import javax.servlet.Filter;

import org.apache.commons.lang3.StringUtils;
import org.apache.tools.ant.UnsupportedAttributeException;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.dea42.genspring.repo.AccountRepository;
import com.dea42.genspring.repo.CablecardRepository;
import com.dea42.genspring.repo.NetworksRepository;
import com.dea42.genspring.repo.OtaRepository;
import com.dea42.genspring.repo.RoamionplRepository;
import com.dea42.genspring.repo.RoamiospRepository;
import com.dea42.genspring.repo.RoamiotodoRepository;
import com.dea42.genspring.repo.ShowsRepository;
import com.dea42.genspring.service.AccountService;
import com.dea42.genspring.service.CablecardServices;
import com.dea42.genspring.service.NetworksServices;
import com.dea42.genspring.service.OtaServices;
import com.dea42.genspring.service.RoamionplServices;
import com.dea42.genspring.service.RoamiospServices;
import com.dea42.genspring.service.RoamiotodoServices;
import com.dea42.genspring.service.ShowsServices;
import com.dea42.genspring.utils.Message;
import com.dea42.genspring.utils.Utils;

/**
 * @author avata
 *
 */
public class MockBase extends UnitBase {
	@MockBean
	protected AccountService accountService;
	@MockBean
	protected AccountRepository accountRepository;
	@MockBean
	protected NetworksServices networksServices;
	@MockBean
	protected ShowsServices showsServices;
	@MockBean
	protected RoamiospServices roamiospServices;
	@MockBean
	protected CablecardServices cablecardServices;
	@MockBean
	protected RoamionplServices roamionplServices;
	@MockBean
	protected OtaServices otaServices;
	@MockBean
	protected RoamiotodoServices roamiotodoServices;

	@MockBean
	protected NetworksRepository networksRepository;
	@MockBean
	protected ShowsRepository showsRepository;
	@MockBean
	protected RoamiospRepository roamiospRepository;
	@MockBean
	protected CablecardRepository cablecardRepository;
	@MockBean
	protected RoamionplRepository roamionplRepository;
	@MockBean
	protected OtaRepository otaRepository;
	@MockBean
	protected RoamiotodoRepository roamiotodoRepository;

	@Autowired
	protected WebApplicationContext webApplicationContext;

	@Autowired
	private Filter springSecurityFilterChain;

	@Before()
	public void setup() {
		// Init MockMvc Object and build
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
				.apply(SecurityMockMvcConfigurers.springSecurity()).addFilters(springSecurityFilterChain).build();
	}

	/**
	 * Send request and if not expecting request check header for nav
	 * 
	 * @param type
	 * @param relURL
	 * @param modelName
	 * @param model
	 * @param params
	 * @param login
	 * @param redirectedUrl
	 * @return ResultActions object for further checks.
	 * @throws Exception
	 */
	protected ResultActions send(String type, String relURL, String modelName, Object model, Map<String, String> params,
			String login, String redirectedUrl) throws Exception {
		MockHttpServletRequestBuilder req = null;
		if (SEND_GET.equals(type)) {
			req = get(relURL);
		} else if (SEND_POST.equals(type)) {
			req = post(relURL);
		} else {
			throw new UnsupportedAttributeException("Send type not supported", type);
		}
		if (!StringUtils.isAllBlank(modelName)) {
			req = req.flashAttr(modelName, model);
		}
		if (params != null && !params.isEmpty()) {
			for (String key : params.keySet()) {
				req = req.param(key, params.get(key));
			}
		}
		if (!StringUtils.isAllBlank(login)) {
			UserRequestPostProcessor urpp = user(login);
			if (ADMIN_USER.equals(login)) {
				urpp = urpp.roles(ADMIN_ROLE);
			} else {
				urpp = urpp.roles(TEST_ROLE);
			}
			req = req.with(urpp);
		} else {
			req = req.with(anonymous());
		}

		ResultActions result = this.mockMvc.perform(req);
		if (redirectedUrl != null) {
			// If redirect then just check right one
			result.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(redirectedUrl));
		} else if (headless.contains(relURL)) {
			// For pops just check status
			result.andExpect(status().isOk());
		} else {
			// else do full header check
			checkHeader(result, login);
		}
		return result;
	}

	/**
	 * Check header is on page and complete. TODO: add active module check
	 * 
	 * @param result
	 * @throws Exception
	 */
	public void checkHeader(ResultActions result, String user) throws Exception {
		result.andExpect(status().isOk());
		contentContainsKey(result, "app.name", false);
		// GUI menu
		contentContainsKey(result, "header.gui", false);
		contentContainsKey(result, "class.Networks", false);
		contentContainsKey(result, "class.Shows", false);
		contentContainsKey(result, "class.Roamiosp", false);
		contentContainsKey(result, "class.Cablecard", false);
		contentContainsKey(result, "class.Roamionpl", false);
		contentContainsKey(result, "class.Ota", false);
		contentContainsKey(result, "class.Roamiotodo", false);
// REST menu
		contentContainsKey(result, "header.restApi", false);
		contentContainsMarkup(result, "/api/networkss", false);
		contentContainsMarkup(result, "/api/showss", false);
		contentContainsMarkup(result, "/api/roamiosps", false);
		contentContainsMarkup(result, "/api/cablecards", false);
		contentContainsMarkup(result, "/api/roamionpls", false);
		contentContainsMarkup(result, "/api/otas", false);
		contentContainsMarkup(result, "/api/roamiotodos", false);
// Login / out
		contentContainsKey(result, "lang.eng", false);
		contentContainsKey(result, "lang.fr", false);
		contentContainsKey(result, "lang.de", false);

		if (user == null)
			contentContainsKey(result, "signin.signin", false);
		else
			contentContainsKey(result, "signin.logout", false);

	}

	/**
	 * Do mock get as admin user, check the nav header then return the handle.
	 * 
	 * @param relURL
	 * @return
	 * @throws Exception
	 */
	protected ResultActions getAsAdmin(String relURL) throws Exception {
		return send(SEND_GET, relURL, null, null, null, ADMIN_USER, null);
	}

	/**
	 * Do mock get as admin user, check for redirect. Wrapper for send().
	 * 
	 * @param relURL
	 * @throws Exception
	 */
	protected void getAsAdminRedirectExpected(String relURL, String redirectedUrl) throws Exception {
		send(SEND_GET, relURL, null, null, null, ADMIN_USER, redirectedUrl);
	}

	/**
	 * Do mock get not logged in, check for redirect. Wrapper for send().
	 * 
	 * @param relURL
	 * @throws Exception
	 */
	protected void getAsNoOneRedirectExpected(String relURL, String redirectedUrl) throws Exception {
		send(SEND_GET, relURL, null, null, null, ADMIN_USER, redirectedUrl);
	}

	/**
	 * Do mock get not logged in, check the nav header then return the handle.
	 * Wrapper for send().
	 * 
	 * @param relURL
	 * @throws Exception
	 */
	protected ResultActions getAsNoOne(String relURL) throws Exception {
		return send(SEND_GET, relURL, null, null, null, null, null);
	}

	/**
	 * Confirm text of key is in content
	 * 
	 * @param result
	 * @param key
	 * @param failIfExists flip to fail if there
	 */
	public void contentContainsKey(ResultActions result, String key, boolean failIfExists) {
		String expectedText = Utils.getProp(getMsgBundle(), key);
		contentContainsMarkup(result, expectedText, failIfExists);
	}

	/**
	 * Check to see if htmlString is in HTML content of result
	 * 
	 * @param result
	 * @param htmlString
	 */
	public void contentContainsMarkup(ResultActions result, String htmlString) {
		contentContainsMarkup(result, htmlString, false);
	}

	/**
	 * Check to see if htmlString is in HTML content of result
	 * 
	 * @param result
	 * @param htmlString   if null or blank String then just returns
	 * @param failIfExists flip to fail if exists instead of when missing.
	 */
	public void contentContainsMarkup(ResultActions result, String htmlString, boolean failIfExists) {
		// if
		if (StringUtils.isBlank(htmlString))
			return;

		try {
			result.andExpect(content().string(containsString(htmlString)));
			if (failIfExists) {
				LOGGER.error("Found '" + htmlString + "' in " + content());
				fail("Found '" + htmlString + "' in content");
			}
		} catch (Throwable e) {
			if (!failIfExists) {
				LOGGER.error("Did not find '" + htmlString + "' in " + content(), e);
				fail("Did not find '" + htmlString + "' in content");
			}
		}
	}

	public void expectSuccessMsg(ResultActions ra, String msgKey) throws Exception {
		expectSuccessMsg(ra, msgKey, new Object[0]);
	}

	public void expectSuccessMsg(ResultActions ra, String msgKey, Object... args) throws Exception {
		Message msg = new Message(msgKey, Message.Type.SUCCESS, args);
		// Compares type, key and params.
		ra.andExpect(flash().attribute(Message.MESSAGE_ATTRIBUTE, msg));
	}

	public void expectErrorMsg(ResultActions ra, String msgKey) throws Exception {
		expectErrorMsg(ra, msgKey, new Object[0]);
	}

	public void expectErrorMsg(ResultActions ra, String msgKey, Object... args) throws Exception {
		Message msg = new Message(msgKey, Message.Type.DANGER, args);
		// Compares type, key and params.
		ra.andExpect(flash().attribute(Message.MESSAGE_ATTRIBUTE, msg));
	}

	/**
	 * @deprecated see send()
	 * @return
	 */
	protected RequestPostProcessor getMockTestUser() {
		LOGGER.debug("Using the user:" + TEST_USER + " with role:ROLE_" + TEST_ROLE);
		UserRequestPostProcessor rtn = user(TEST_USER).roles(TEST_ROLE);
		LOGGER.debug("Returning:" + rtn);
		return rtn;
	}

	/**
	 * @deprecated see send()
	 * @return
	 */
	protected RequestPostProcessor getMockTestAdmin() {
		LOGGER.debug("Using the user:" + ADMIN_USER + " with role:ROLE_" + ADMIN_ROLE);
		UserRequestPostProcessor rtn = user(ADMIN_USER).roles(ADMIN_ROLE);
		LOGGER.debug("Returning:" + rtn);
		return rtn;
	}
}
