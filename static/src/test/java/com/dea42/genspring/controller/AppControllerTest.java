package com.dea42.genspring.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.ResultActions;

import com.dea42.genspring.MockBase;
import com.dea42.genspring.entity.Account;
import com.dea42.genspring.form.SignupForm;
import com.dea42.genspring.utils.Message;
import com.google.common.collect.ImmutableMap;

@RunWith(SpringJUnit4ClassRunner.class)
@WebMvcTest(AppController.class)
public class AppControllerTest extends MockBase {

	/**
	 * Test method for
	 * {@link com.dea42.genspring.controller.AppController#getIndex()}.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testIndex() throws Exception {
		ResultActions result = getAsNoOne("/");
		contentContainsKey(result, "view.index.title", false);
		contentContainsKey(result, "app.name", false);
		contentContainsKey(result, "signin.signup", false);
		result = getAsAdmin("/");
		contentContainsKey(result, "view.index.title", false);
		contentContainsKey(result, "index.greeting", false);
	}

	/**
	 * quick check / maps to /home
	 * 
	 * @throws Exception
	 */
	@Test
	public void testHome() throws Exception {
		ResultActions result = getAsNoOne("/home");
		contentContainsKey(result, "view.index.title", false);
	}

	/**
	 * check text on sign up page
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSignupModelString() throws Exception {
		ResultActions result = getAsNoOne("/signup");
		contentContainsKey(result, "signin.email", false);
		contentContainsKey(result, "signin.password", false);
		contentContainsKey(result, "signin.haveAccount", false);
		contentContainsKey(result, "signin.signin", false);
		contentContainsKey(result, "signin.signup", false);
	}

	/**
	 * Test signup TODO: sort best way to test this
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSignupSignupFormErrorsRedirectAttributes() throws Exception {
		SignupForm signupForm = new SignupForm();
		signupForm.setEmail(ADMIN_USER);
		signupForm.setPassword(ADMIN_PASS);
		signupForm.setSignup(true);
		Account account = signupForm.createAccount();

		given(accountServices.save(account)).willReturn(account);
		given(accountServices.login(account.getEmail(), account.getPassword())).willReturn(true);

		ResultActions ra = send(SEND_POST, "/signup", "signupForm", signupForm, ImmutableMap.of("action", "save"), null,
				"/home");

		Message msg = new Message("signup.success", Message.Type.SUCCESS, new Object[0]);
		ra.andExpect(flash().attribute("message", msg));
	}

	/**
	 * check text on sign in page
	 * 
	 * @throws Exception
	 */
	@Test
	public void testLoginHttpServletRequestSignupFormStringString() throws Exception {
		ResultActions result = getAsNoOne("/login");
		contentContainsKey(result, "signin.email", false);
		contentContainsKey(result, "signin.password", false);
		contentContainsKey(result, "signin.rememberMe", false);
		contentContainsKey(result, "signin.signin", false);
		contentContainsKey(result, "signin.newHere", false);
		contentContainsKey(result, "signin.signup", false);
	}

	/**
	 * Test sign in
	 * 
	 * @throws Exception
	 */
	@Test
	public void testLoginModelSignupFormErrorsRedirectAttributes() throws Exception {
		SignupForm signupForm = new SignupForm();
		signupForm.setEmail(ADMIN_USER);
		signupForm.setPassword(ADMIN_PASS);
		Account account = signupForm.createAccount();

		given(accountServices.save(account)).willReturn(account);
		given(accountServices.login(account.getEmail(), account.getPassword())).willReturn(true);

		ResultActions ra = send(SEND_POST, "/authenticate", "signupForm", signupForm, null, null, "/home");
		expectSuccessMsg(ra, "signin.success");

		signupForm = new SignupForm();
		signupForm.setEmail(ADMIN_USER);
		signupForm.setPassword("bad pass");
		account = signupForm.createAccount();

		ra = send(SEND_POST, "/authenticate", "signupForm", signupForm, null, null, null);
		contentContainsKey(ra, "signin.failed", false);

		signupForm = new SignupForm();
		signupForm.setEmail(ADMIN_USER);
		signupForm.setPassword("");
		account = signupForm.createAccount();

		ra = send(SEND_POST, "/authenticate", "signupForm", signupForm, null, null, null);
		contentContainsKey(ra, "notBlank.message", false);

		signupForm = new SignupForm();
		signupForm.setEmail("");
		signupForm.setPassword("bad pass");
		account = signupForm.createAccount();

		ra = send(SEND_POST, "/authenticate", "signupForm", signupForm, null, null, null);
		contentContainsKey(ra, "notBlank.message", false);

		signupForm = new SignupForm();
		signupForm.setEmail("admin");
		signupForm.setPassword("bad pass");
		account = signupForm.createAccount();

		ra = send(SEND_POST, "/authenticate", "signupForm", signupForm, null, null, null);
		contentContainsKey(ra, "email.message", false);
		contentContainsKey(ra, "notBlank.message", true);
	}

	/**
	 * quick test lang swap
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetInternationalPage() throws Exception {
		getAsAdminRedirectExpected("/international", "/home");
		getAsNoOneRedirectExpected("/international", "/home");
	}

}
