package com.dea42.genspring.controller;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.dea42.genspring.entity.Account;
import com.dea42.genspring.form.AccountForm;
import com.dea42.genspring.service.AccountServices;
import com.dea42.genspring.utils.MessageHelper;
import com.dea42.genspring.utils.Utils;

/**
 * Title: AccountController <br>
 * Description: AccountController. <br>
 * Copyright: Copyright (c) 2001-2020<br>
 * Company: RMRR<br>
 * 
 * @author Gened by com.dea42.build.GenSpring version 0.2.3<br>
 * @version 1.0<br>
 * 
 */
@Controller
@RequestMapping("/accounts")
public class AccountController {
	private static final Logger LOGGER = LoggerFactory.getLogger(AccountController.class.getName());

	@Autowired
	private AccountServices accountService;

	@GetMapping
	public String getAllAccounts(Model model) {
		model.addAttribute("accounts", this.accountService.listAll());
		return "accounts";
	}

	@GetMapping("/new")
	public String showNewAccountPage(Model model,
			@RequestHeader(value = "X-Requested-With", required = false) String requestedWith) {
		model.addAttribute(new AccountForm());
		if (Utils.isAjaxRequest(requestedWith)) {
			return "edit_account".concat(" :: accountForm");
		}

		return "edit_account";
	}

	@PostMapping(value = "/save")
	public String saveAccount(@Valid @ModelAttribute AccountForm accountForm, Errors errors, RedirectAttributes ra,
			@RequestParam(value = "action", required = true) String action) {
		if (action.equals("save")) {
			if (errors.hasErrors()) {
				return "edit_account";
			}

			Account account = new Account(accountForm.getId(), accountForm.getEmail(), accountForm.getPassword(),
					accountForm.getRole(), accountForm.getCreated());
			try {
				account = accountService.save(account);
			} catch (Exception e) {
				LOGGER.error("Failed saving:" + accountForm, e);
			}

			if (account == null) {
				MessageHelper.addErrorAttribute(ra, "db.failed");
			} else {
				MessageHelper.addSuccessAttribute(ra, "save.success");
			}
		} else {
			MessageHelper.addSuccessAttribute(ra, "save.cancelled");
		}
		return "redirect:/accounts";
	}

	@GetMapping("/edit/{id}")
	public ModelAndView showEditAccountPage(@PathVariable(name = "id") Long id) {
		ModelAndView mav = new ModelAndView("edit_account");
		Account account = accountService.get(id);
		mav.addObject("accountForm", AccountForm.getInstance(account));

		return mav;
	}

	@GetMapping("/delete/{id}")
	public String deleteAccount(@PathVariable(name = "id") Long id) {
		accountService.delete(id);
		return "redirect:/accounts";
	}
}
