package com.dea42.genspring.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Title: AppController <br>
 * Description: Class main web Controller. <br>
 * 
 * @author Gened by com.dea42.build.GenSpring<br>
 * @version 1.0<br>
 */
@Controller
public class AppController {

	@GetMapping("/login")
	public String getLoginPage() {
		return "login";
	}

	@GetMapping("/")
	public String getIndex() {
		return "index";
	}

	@GetMapping("/api/")
	public String getApiIndex() {
		return "api_index";
	}

}
