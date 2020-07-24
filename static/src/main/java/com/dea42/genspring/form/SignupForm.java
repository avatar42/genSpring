package com.dea42.genspring.form;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

import com.dea42.genspring.entity.Account;

public class SignupForm {

	private static final String NOT_BLANK_MESSAGE = "{notBlank.message}";
	private static final String EMAIL_MESSAGE = "{email.message}";

	@NotBlank(message = SignupForm.NOT_BLANK_MESSAGE)
	@Email(message = SignupForm.EMAIL_MESSAGE)
	private String email;

	@NotBlank(message = SignupForm.NOT_BLANK_MESSAGE)
	private String password;

	private String referer;

	private boolean signup = false;

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * @return the referer
	 */
	public String getReferer() {
		return referer;
	}

	/**
	 * @param referer the referer to set
	 */
	public void setReferer(String referer) {
		this.referer = referer;
	}

	/**
	 * @return the signup
	 */
	public boolean isSignup() {
		return signup;
	}

	/**
	 * @param signup the signup to set
	 */
	public void setSignup(boolean signup) {
		this.signup = signup;
	}

	public Account createAccount() {
		return new Account(getEmail(), getPassword(), "ROLE_USER");
	}
}
