package com.dea42.genspring.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.time.Instant;

@SuppressWarnings("serial")
@Entity
@Table(name = "account")
public class Account implements java.io.Serializable {

	@Id
	@GeneratedValue
	private Long id;

	@Column(unique = true)
	private String email;

	@JsonIgnore
	private String password;

	private String role = "ROLE_USER";

	private Instant created;

	protected Account() {

	}

	public Account(String email, String password, String role) {
		this.email = email;
		this.password = password;
		this.role = role;
		this.created = Instant.now();
	}

	public Account(String email, String password) {
		this.email = email;
		this.password = password;
	}

	public Long getId() {
		return id;
	}

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

	public String getRole() {
		return role;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public Instant getCreated() {
		return created;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Account [id=").append(id).append(", email=").append(email).append(", password=")
				.append(password).append(", role=").append(role).append(", created=").append(created).append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((created == null) ? 0 : created.hashCode());
		result = prime * result + ((email == null) ? 0 : email.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((password == null) ? 0 : password.hashCode());
		result = prime * result + ((role == null) ? 0 : role.hashCode());
		return result;
	}

	/**
	 * Mainly for mock testing
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Account other = (Account) obj;
//		if (created == null) {
//			if (other.created != null)
//				return false;
//		} else if (!created.equals(other.created))
//			return false;
		if (email == null) {
			if (other.email != null)
				return false;
		} else if (!email.equals(other.email))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (password == null) {
			if (other.password != null)
				return false;
		} else if (!password.equals(other.password))
			return false;
		if (role == null) {
			if (other.role != null)
				return false;
		} else if (!role.equals(other.role))
			return false;
		return true;
	}

}
