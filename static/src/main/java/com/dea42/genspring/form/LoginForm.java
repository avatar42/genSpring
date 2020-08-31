package com.dea42.genspring.form;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

import com.dea42.genspring.utils.MessageHelper;

/**
 * Form for logging in only
 * 
 * @author avata
 *
 */
public class LoginForm {

	@NotBlank(message = MessageHelper.NOT_BLANK_MESSAGE)
	@Email(message = MessageHelper.EMAIL_MESSAGE)
	private String email;

	@NotBlank(message = MessageHelper.NOT_BLANK_MESSAGE)
	private String password;

	private String referer;

	public LoginForm() {

	}

	/**
	 * @return the email
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * @param email the email to set
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @param password the password to set
	 */
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

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("LoginForm [email=").append(email).append(", password=").append(password).append(", referer=")
				.append(referer).append("]");
		return builder.toString();
	}

}
