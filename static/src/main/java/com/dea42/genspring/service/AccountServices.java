package com.dea42.genspring.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dea42.genspring.entity.Account;
import com.dea42.genspring.repo.AccountRepository;
import com.dea42.genspring.utils.Utils;

@Service
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class AccountServices implements UserDetailsService {
	private static final Logger LOGGER = LoggerFactory.getLogger(AccountServices.class.getName());
	public static final String ROLE_PREFIX = "ROLE_";

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private String encryptPrefix = null;

	private String encrypt(String s) {
		if (StringUtils.isAllBlank(s)) {
			return null;
		}
		// Avoid double encrypting
		if (s.startsWith(encryptPrefix)) {
			return s;
		}

		String rtn = passwordEncoder.encode(s);

		LOGGER.trace("Encrypted:'" + s + "' to '" + rtn + "'");
		return rtn;
	}

	/**
	 * reset default users. Comment out once done testing
	 */
	@PostConstruct
	protected void initialize() {
		ResourceBundle bundle = ResourceBundle.getBundle("app");
		boolean doinit = Utils.getProp(bundle, "init.default.users", true);
		// Avoid double encrypting
		encryptPrefix = passwordEncoder.encode("password").substring(0, 3);
		if (doinit) {

			String user = Utils.getProp(bundle, "default.user", null);
			if (!StringUtils.isBlank(user)) {
				long id = Utils.getProp(bundle, "default.userid", 1l);
				String userpass = Utils.getProp(bundle, "default.userpass", null);
				String userrole = ROLE_PREFIX + Utils.getProp(bundle, "default.userrole", null);
				update(id, user, userpass, userrole);
			}

			user = Utils.getProp(bundle, "default.admin", null);
			if (!StringUtils.isBlank(user)) {
				long id = Utils.getProp(bundle, "default.adminid", 2l);
				String userpass = Utils.getProp(bundle, "default.adminpass", null);
				String userrole = ROLE_PREFIX + Utils.getProp(bundle, "default.adminrole", null);
				update(id, user, userpass, userrole);
			}
		}
	}

	@Transactional
	public Account save(Account account) {
		Account a = null;
		if (account.getId() > 0) {
			Optional<Account> o = accountRepository.findById(account.getId());
			if (o.isPresent())
				a = o.get();
		}
		if (a == null) {
			a = account;
		}
		a.setEmail(account.getEmail());
		a.setRole(account.getRole());

		if (!StringUtils.isBlank(account.getPassword())) {
			a.setPassword(encrypt(account.getPassword()));
		}

		accountRepository.save(a);
		LOGGER.debug("Saved:" + a);
		return a;
	}

	@Transactional
	public Account update(Long id, String email, String password, String role) {
		Account a = new Account(email, password, role);
		a.setId(id);

		return save(a);
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		Account account = accountRepository.findOneByEmail(username);
		if (account == null) {
			return null;
			// throw new UsernameNotFoundException("user not found");
		}
		return createUser(account);
	}

	public boolean isEmailAlreadyInUse(String username) throws UsernameNotFoundException {
		if (username == null)
			return false;

		Account account = accountRepository.findOneByEmail(username);
		if (account == null)
			return false;

		return true;
	}

	public boolean login(String email, String password) {
		Account account = accountRepository.findOneByEmail(email);
		if (account == null) {
			LOGGER.warn("User email " + email + " not found in DB");
			return false;
		}

		if (passwordEncoder.matches(password, account.getPassword())) {
			SecurityContextHolder.getContext().setAuthentication(genAuthToken(account));
			// TODO: add last logged in date/time.
			return true;
		}

		LOGGER.warn("User " + email + " password did not match");
		return false;

	}

	/**
	 * Gen token.
	 * 
	 * @param account
	 * @return
	 */
	private Authentication genAuthToken(Account account) {
		LOGGER.debug("authing:" + account);
		Authentication a = new UsernamePasswordAuthenticationToken(createUser(account), null,
				Collections.singleton(createAuthority(account)));
		LOGGER.debug("result in:" + a);
		return a;
	}

	private User createUser(Account account) {
		return new User(account.getEmail(), account.getPassword(), Collections.singleton(createAuthority(account)));
	}

	private GrantedAuthority createAuthority(Account account) {
		LOGGER.debug("logged in:" + account);
		return new SimpleGrantedAuthority(account.getRole());
	}

	public List<Account> listAll() {
		return (List<Account>) accountRepository.findAll();
	}

	public Account get(Long id) {
		return accountRepository.findById(id).get();
	}

	public void delete(Long id) {
		accountRepository.deleteById(id);
	}

}
