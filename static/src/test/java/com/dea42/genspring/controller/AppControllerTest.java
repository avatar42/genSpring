package com.dea42.genspring.controller;

import static org.mockito.BDDMockito.given;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.ResultActions;

import com.dea42.genspring.MockBase;
import com.dea42.genspring.entity.Account;
import com.dea42.genspring.form.AccountForm;
import com.dea42.genspring.form.LoginForm;
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
		contentContainsKey(result, "view.index.title");
		contentContainsKey(result, "app.name");
		contentContainsKey(result, "signin.signup");
		result = getAsAdmin("/");
		contentContainsKey(result, "view.index.title");
		contentContainsKey(result, "index.greeting");
	}

	/**
	 * quick check / maps to /home
	 * 
	 * @throws Exception
	 */
	@Test
	public void testHome() throws Exception {
		ResultActions result = getAsNoOne("/home");
		contentContainsKey(result, "view.index.title");
	}

	/**
	 * check text on sign up page
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSignupModelString() throws Exception {
		ResultActions result = getAsNoOne("/signup");
		contentContainsKey(result, "signin.email");
		contentContainsKey(result, "signin.password");
		contentContainsKey(result, "signin.haveAccount");
		contentContainsKey(result, "signin.signin");
		contentContainsKey(result, "signin.signup");
	}

	/**
	 * used only for AppController.login() testing
	 * 
	 * @param email
	 * @param password
	 */
	public LoginForm getLoginInstance(String email, String password) {
		LoginForm a = new LoginForm();
		a.setEmail(email);
		a.setPassword(password);
		return a;
	}

	/**
	 * Test signup TODO: sort best way to test this
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSignupAccountFormErrorsRedirectAttributes() throws Exception {
		Account account = new Account(TEST_USER, TEST_PASS, TEST_ROLE);

		given(accountServices.save(account)).willReturn(account);
		given(accountServices.login(account.getEmail(), account.getPassword())).willReturn(true);
		given(accountServices.isEmailAlreadyInUse(ADMIN_USER)).willReturn(true);
		given(accountServices.isEmailAlreadyInUse(TEST_USER)).willReturn(false);

		AccountForm accountForm = AccountForm.getInstance(account);
		ResultActions ra = send(SEND_POST, "/signup", "accountForm", accountForm, ImmutableMap.of("action", "save"),
				null, "/home");
		expectSuccessMsg(ra, "signup.success");

		accountForm = AccountForm.getInstance(account);
		accountForm.setEmail("admin");
		accountForm.setPasswordConfirm("bad password");
		ra = send(SEND_POST, "/signup", "accountForm", accountForm, null, null, null);
		contentContainsKey(ra, "INSUFFICIENT_UPPERCASE", "1");
		contentContainsKey(ra, "INSUFFICIENT_DIGIT", "1");
		contentContainsKey(ra, "INSUFFICIENT_SPECIAL", "1");

		accountForm.setPasswordConfirm("BAD_PASSWORD");
		ra = send(SEND_POST, "/signup", "accountForm", accountForm, null, null, null);
		contentContainsKey(ra, "INSUFFICIENT_LOWERCASE", "1");
		contentContainsKey(ra, "INSUFFICIENT_DIGIT", "1");

		accountForm.setPasswordConfirm("P@$$w1rd");
		ra = send(SEND_POST, "/signup", "accountForm", accountForm, null, null, null);
		contentContainsKey(ra, "password.mismatch");

		accountForm.setPasswordConfirm("P@$$w1r");
		ra = send(SEND_POST, "/signup", "accountForm", accountForm, null, null, null);
		contentContainsKey(ra, "password.mismatch");

		accountForm = AccountForm.getInstance(account);
		accountForm.setEmail("admin");
		ra = send(SEND_POST, "/signup", "accountForm", accountForm, null, null, null);
		contentContainsKey(ra, "email.message");

		accountForm = AccountForm.getInstance(account);
		accountForm.setEmail("");
		ra = send(SEND_POST, "/signup", "accountForm", accountForm, null, null, null);
		contentContainsKey(ra, "notBlank.message");

		accountForm = AccountForm.getInstance(account);
		accountForm.setPassword(" ");
		accountForm.setPasswordConfirm(" ");
		ra = send(SEND_POST, "/signup", "accountForm", accountForm, null, null, null);
		contentContainsKey(ra, "password.mismatch");

		accountForm = AccountForm.getInstance(account);
		accountForm.setEmail(ADMIN_USER);
		ra = send(SEND_POST, "/signup", "accountForm", accountForm, null, null, null);
		contentContainsKey(ra, "email.unique");
	}

	/**
	 * check text on sign in page
	 * 
	 * @throws Exception
	 */
	@Test
	public void testLoginHttpServletRequestAccountFormStringString() throws Exception {
		ResultActions result = getAsNoOne("/login");
		contentContainsKey(result, "signin.email");
		contentContainsKey(result, "signin.password");
		contentContainsKey(result, "signin.rememberMe");
		contentContainsKey(result, "signin.signin");
		contentContainsKey(result, "signin.newHere");
		contentContainsKey(result, "signin.signup");
	}

	/**
	 * Test sign in
	 * 
	 * @throws Exception
	 */
	@Test
	public void testLoginModelAccountFormErrorsRedirectAttributes() throws Exception {
		// set up
		Account account = new Account(ADMIN_USER, ADMIN_PASS);
		given(accountServices.save(account)).willReturn(account);
		given(accountServices.login(account.getEmail(), account.getPassword())).willReturn(true);

		// happy path test
		LoginForm loginForm = getLoginInstance(ADMIN_USER, ADMIN_PASS);
		ResultActions ra = send(SEND_POST, "/authenticate", "loginForm", loginForm, null, null, "/home");
		expectSuccessMsg(ra, "signin.success");
		contentNotContainsKey(ra, "form.errors");

		// failure tests
		loginForm = getLoginInstance(ADMIN_USER, "bad pass");
		ra = send(SEND_POST, "/authenticate", "loginForm", loginForm, null, null, null);
		contentContainsKey(ra, "signin.failed");

		loginForm = getLoginInstance(ADMIN_USER, "");
		ra = send(SEND_POST, "/authenticate", "loginForm", loginForm, null, null, null);
		contentContainsKey(ra, "notBlank.message");

		loginForm = getLoginInstance("", "bad pass");
		ra = send(SEND_POST, "/authenticate", "loginForm", loginForm, null, null, null);
		contentContainsKey(ra, "notBlank.message");

		loginForm = getLoginInstance(ADMIN_USER, " ");
		ra = send(SEND_POST, "/authenticate", "loginForm", loginForm, null, null, null);
		contentContainsKey(ra, "notBlank.message");

		loginForm = getLoginInstance("	", "bad pass");
		ra = send(SEND_POST, "/authenticate", "loginForm", loginForm, null, null, null);
		contentContainsKey(ra, "notBlank.message");

		loginForm = getLoginInstance("admin", "bad pass");
		ra = send(SEND_POST, "/authenticate", "loginForm", loginForm, null, null, null);
		contentContainsKey(ra, "email.message");
		contentNotContainsKey(ra, "notBlank.message");
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
