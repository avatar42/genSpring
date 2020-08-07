package com.dea42.genspring.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.dea42.genspring.entity.Account;
import com.dea42.genspring.repo.AccountRepository;

import java.security.Principal;

/**
 * REST interface for basic login actions
 * 
 * @author avata
 *
 */
@RestController
public class LoginController {

	private final AccountRepository accountRepository;

	public LoginController(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

	@GetMapping("account/current")
	@ResponseStatus(value = HttpStatus.OK)
	@Secured({ "ROLE_USER", "ROLE_ADMIN" })
	public Account currentAccount(Principal principal) {
		Assert.notNull(principal, "Check principal null");
		return accountRepository.findOneByEmail(principal.getName());
	}

	@GetMapping("account/{id}")
	@ResponseStatus(value = HttpStatus.OK)
	@Secured("ROLE_ADMIN")
	public Account account(@PathVariable("id") Long id) {
		return accountRepository.getOne(id);
	}
}
