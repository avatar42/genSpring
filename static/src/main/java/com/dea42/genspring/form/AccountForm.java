package com.dea42.genspring.form;

import java.sql.Timestamp;
import java.util.List;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

import com.dea42.genspring.controller.FieldMatch;
import com.dea42.genspring.controller.UniqueEmail;
import com.dea42.genspring.controller.ValidatePassword;
import com.dea42.genspring.entity.Account;
import com.dea42.genspring.utils.MessageHelper;

@FieldMatch.List({
		@FieldMatch(fieldName = "password", secondFieldName = "passwordConfirm", message = "password.mismatch") })
@UniqueEmail.List({ @UniqueEmail(message = "email.unique") })
public class AccountForm {

	private Long id = 0l;

	@NotBlank(message = MessageHelper.NOT_BLANK_MESSAGE)
	@Email(message = MessageHelper.EMAIL_MESSAGE)
	private String email;

	@ValidatePassword(fieldName = "password")
	private String password;

	@ValidatePassword(fieldName = "passwordConfirm")
	private String passwordConfirm;

	private String role = "ROLE_USER";

	private Timestamp created = new Timestamp(System.currentTimeMillis());

	private String referer;

	private List<String> roles;

	public AccountForm() {

	}

	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(Long id) {
		this.id = id;
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
	 * @return the passwordConfirm
	 */
	public String getPasswordConfirm() {
		return passwordConfirm;
	}

	/**
	 * @param passwordConfirm the passwordConfirm to set
	 */
	public void setPasswordConfirm(String passwordConfirm) {
		this.passwordConfirm = passwordConfirm;
	}

	/**
	 * @return the role
	 */
	public String getRole() {
		return role;
	}

	/**
	 * @param role the role to set
	 */
	public void setRole(String role) {
		this.role = role;
	}

	/**
	 * @return the created
	 */
	public Timestamp getCreated() {
		return created;
	}

	/**
	 * @param created the created to set
	 */
	public void setCreated(Timestamp created) {
		this.created = created;
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
		return id == 0;
	}

	/**
	 * @return the roles
	 */
	public List<String> getRoles() {
		return roles;
	}

	/**
	 * @param roles the roles to set
	 */
	public void setRoles(List<String> roles) {
		this.roles = roles;
	}

	/**
	 * Clones account into form
	 * 
	 * @param account
	 */
	public static AccountForm getInstance(Account account) {
		AccountForm a = new AccountForm();
		a.setId(account.getId());
		a.setEmail(account.getEmail());
		a.setPassword(account.getPassword());
		a.setPasswordConfirm(account.getPassword());
		a.setRole(account.getRole());
		a.setCreated(account.getCreated());

		return a;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AccountForm [id=").append(id).append(", email=").append(email).append(", password=")
				.append(password).append(", passwordConfirm=").append(passwordConfirm).append(", role=").append(role)
				.append(", created=").append(created).append(", referer=").append(referer).append(", roles=")
				.append(roles).append("]");
		return builder.toString();
	}

}
