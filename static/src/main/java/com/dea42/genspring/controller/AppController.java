package com.dea42.genspring.controller;

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.dea42.genspring.entity.Account;
import com.dea42.genspring.form.SignupForm;
import com.dea42.genspring.service.AccountService;
import com.dea42.genspring.utils.Utils;
import com.dea42.genspring.utils.MessageHelper;

/**
 * Title: AppController <br>
 * Description: Class main web Controller. <br>
 * 
 * @author Gened by com.dea42.build.GenSpring version genSpringVersion<br>
 * @version 1.0<br>
 */
@Controller
public class AppController {
	private static final Logger LOGGER = LoggerFactory.getLogger(AppController.class.getName());

	public static final String SIGNUP_VIEW_NAME = "home/signup";
	public static final String SIGNIN_VIEW_NAME = "home/signin";
	public static final String HOME_SIGNED_VIEW_NAME = "home/homeSignedIn";
	public static final String HOME_NOT_SIGNED_VIEW_NAME = "home/homeNotSignedIn";

	@Autowired
	private AccountService accountService;

	@ModelAttribute("module")
	String module() {
		return "home";
	}

	@GetMapping("/")
	String index(Principal principal) {
		return principal != null ? HOME_SIGNED_VIEW_NAME : HOME_NOT_SIGNED_VIEW_NAME;
	}

	@GetMapping("/home")
	String home(Principal principal) {
		return principal != null ? HOME_SIGNED_VIEW_NAME : HOME_NOT_SIGNED_VIEW_NAME;
	}

	@GetMapping("signup")
	String signup(Model model, @RequestHeader(value = "X-Requested-With", required = false) String requestedWith) {
		model.addAttribute(new SignupForm());
		if (Utils.isAjaxRequest(requestedWith)) {
			return SIGNUP_VIEW_NAME.concat(" :: signupForm");
		}
		return SIGNUP_VIEW_NAME;
	}

	@PostMapping("signup")
	public String signup(@Valid @ModelAttribute SignupForm signupForm, Errors errors, RedirectAttributes ra) {
		if (errors.hasErrors()) {
			return SIGNUP_VIEW_NAME;
		}
		Account account = accountService.save(signupForm.createAccount());
		if (account == null) {
			MessageHelper.addErrorAttribute(ra, "db.failed");
			return "redirect:/home";
		}
		if (accountService.login(account.getEmail(), account.getPassword())) {
			// see messages.properties and homeSignedIn.html
			MessageHelper.addSuccessAttribute(ra, "signup.success");
		} else {
			MessageHelper.addErrorAttribute(ra, "signup.failed");
		}
		return "redirect:/home";
	}

	@RequestMapping("login")
	String login(HttpServletRequest request, @ModelAttribute SignupForm signupForm,
			@RequestHeader(value = "X-Requested-With", required = false) String requestedWith,
			@RequestHeader(value = "Referer", required = false) String ref) {
		// deal with loop backs and lost attributes
		if (!StringUtils.isAllBlank(ref) && !ref.contains("/login")) {
			signupForm.setReferer(ref);
		}

		if (Utils.isAjaxRequest(requestedWith)) {
			return SIGNIN_VIEW_NAME.concat(" :: signupForm");
		}
		return SIGNIN_VIEW_NAME;
	}

	@PostMapping("authenticate")
	public String login(Model model, @Valid @ModelAttribute SignupForm signupForm, Errors errors,
			RedirectAttributes ra) {
		if (errors.hasErrors()) {
			return SIGNIN_VIEW_NAME;
		}
		if (accountService.login(signupForm.getEmail(), signupForm.getPassword())) {
			// see messages.properties and homeSignedIn.html
			MessageHelper.addSuccessAttribute(ra, "signin.success");
			String referer = signupForm.getReferer();
			if (StringUtils.isAllBlank(referer)) {
				// TODO: add /?lang=en to set lang to preferred / last selected.
				referer = "/home";
			}
			LOGGER.info("Login passed. Redirecting to " + referer);
			return "redirect:" + referer;
		}
		MessageHelper.addErrorAttribute(model, "signin.failed");
		return SIGNIN_VIEW_NAME;
	}

	@GetMapping("/international")
	public String getInternationalPage(HttpServletRequest request) {
		// TODO: add save to account info or cookie
		String referer = request.getHeader("Referer");
		if (StringUtils.isAllBlank(referer)) {
			referer = "/home";
		}
		return "redirect:" + referer;
	}

}
