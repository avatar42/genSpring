package com.dea42.genspring.controller;

import static org.mockito.BDDMockito.given;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.ResultActions;
import com.google.common.collect.ImmutableMap;

import com.dea42.genspring.MockBase;
import com.dea42.genspring.entity.Account;
import com.dea42.genspring.form.AccountForm;

/**
 * Title: AccountControllerTest <br>
 * Description: AccountController. <br>
 * Copyright: Copyright (c) 2001-2020<br>
 * Company: RMRR<br>
 * 
 * @author Gened by com.dea42.build.GenSpring version 0.2.3<br>
 * @version 1.0<br>
 *          null
 */
@WebMvcTest(AccountController.class)
public class AccountControllerTest extends MockBase {
	private Account getAccount(Long id) {
		Account o = new Account();
		o.setId(id);
		o.setRole(getTestString(25));
		o.setEmail(getTestEmailString(254));
		return o;
	}

	/**
	 * Test method for
	 * {@link com.dea42.genspring.controller.AccountController#getAllAccounts(org.springframework.ui.Model)}.
	 */
	@Test
	public void testGetAllAccounts() throws Exception {
		List<Account> list = new ArrayList<>();
		Account o = getAccount(1l);
		list.add(o);

		given(accountServices.listAll()).willReturn(list);

		ResultActions ra = getAsAdmin("/accounts");
		contentContainsMarkup(ra, "<h1>" + getMsg("class.Account") + " " + getMsg("edit.list") + "</h1>");
		contentContainsMarkup(ra, getTestString(25));
		contentContainsMarkup(ra, getMsg("Account.role"));
		contentContainsMarkup(ra, getMsg("Account.created"));
		contentContainsMarkup(ra, getMsg("Account.id"));
		contentContainsMarkup(ra, getTestString(254));
		contentContainsMarkup(ra, getMsg("Account.email"));
	}

	/**
	 * Test method for
	 * {@link com.dea42.genspring.controller.AccountController#showNewAccountPage(org.springframework.ui.Model)}.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testShowNewAccountPage() throws Exception {
		ResultActions ra = getAsAdmin("/accounts/new");
		contentContainsMarkup(ra, "<legend>" + getMsg("edit.new") + " " + getMsg("class.Account") + "</legend>");
		contentContainsMarkup(ra, "created");
		contentContainsMarkup(ra, "email");
		contentContainsMarkup(ra, "id");
		contentContainsMarkup(ra, "password");
		contentContainsMarkup(ra, "role");
	}

	/**
	 * Test method for
	 * {@link com.dea42.genspring.controller.AccountController#saveAccount(com.dea42.genspring.entity.Account, java.lang.String)}.
	 */
	@Test
	public void testSaveAccountCancel() throws Exception {
		Account o = getAccount(1l);

		ResultActions ra = send(SEND_POST, "/accounts/save", "account", o, ImmutableMap.of("action", "cancel"),
				ADMIN_USER, "/accounts");
		expectSuccessMsg(ra, "save.cancelled");
		contentNotContainsKey(ra, "form.errors");
	}

	/**
	 * Test method for
	 * {@link com.dea42.genspring.controller.AccountController#saveAccount(com.dea42.genspring.entity.Account, java.lang.String)}.
	 */
	@Test
	public void testSaveAccountSave() throws Exception {
		Account o = getAccount(0l);
		AccountForm accountForm = AccountForm.getInstance(o);
		accountForm.setPassword(getTestPasswordString(25));
		accountForm.setPasswordConfirm(accountForm.getPassword());
		LOGGER.debug(accountForm.toString());
		send(SEND_POST, "/accounts/save", "accountForm", accountForm, ImmutableMap.of("action", "save"), ADMIN_USER,
				"/accounts");
	}

	/**
	 * Test method for
	 * {@link com.dea42.genspring.controller.AccountController#showEditAccountPage(java.lang.Integer)}.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testShowEditAccountPage() throws Exception {
		Account o = getAccount(1l);

		given(accountServices.get(1l)).willReturn(o);

		ResultActions ra = getAsAdmin("/accounts/edit/1");
		contentContainsMarkup(ra, "created");
		contentContainsMarkup(ra, o.getEmail());
		contentContainsMarkup(ra, "email");
		contentContainsMarkup(ra, "id");
//		contentContainsMarkup(ra, o.getPassword());
		contentContainsMarkup(ra, "password");
		contentContainsMarkup(ra, "passwordConfirm");
		contentContainsMarkup(ra, o.getRole());
		contentContainsMarkup(ra, "role");
	}

	/**
	 * Test method for
	 * {@link com.dea42.genspring.controller.AccountController#deleteAccount(java.lang.Integer)}.
	 */
	@Test
	public void testDeleteAccount() throws Exception {
		getAsAdminRedirectExpected("/accounts/delete/1", "/accounts");
	}

}
