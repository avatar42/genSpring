package com.dea42.genspring.controller;

import static org.hamcrest.CoreMatchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringJUnit4ClassRunner.class)
@WebMvcTest(AppController.class)
public class AppControllerTest {
	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Before()
	public void setup() {
		// Init MockMvc Object and build
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
	}

	/**
	 * Test method for
	 * {@link com.dea42.genspring.controller.AppController#getLoginPage()}.
	 */
	@Test
	public void testGetLoginPage() throws Exception {
		this.mockMvc.perform(get("/login")).andExpect(status().isOk())
				.andExpect(content().string(containsString("Login")))
				.andExpect(content().string(containsString("Password:")))
				.andExpect(content().string(containsString("User Name:")));
	}

	/**
	 * Test method for
	 * {@link com.dea42.genspring.controller.AppController#getIndex()}.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetIndex() throws Exception {
		this.mockMvc.perform(get("/").with(user("user").roles("ADMIN"))).andExpect(status().isOk())
				.andExpect(content().string(containsString("Home")))
				.andExpect(content().string(containsString("Login")))
				.andExpect(content().string(containsString("/api/")))
				.andExpect(content().string(containsString("Networks")))
				.andExpect(content().string(containsString("Shows")))
				.andExpect(content().string(containsString("Roamiosp")))
				.andExpect(content().string(containsString("Cablecard")))
				.andExpect(content().string(containsString("Roamionpl")))
				.andExpect(content().string(containsString("Ota")))
				.andExpect(content().string(containsString("Roamiotodo")));
	}

	/**
	 * Test method for
	 * {@link com.dea42.genspring.controller.ApiController#getApiIndex()}.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetApiIndex() throws Exception {
		this.mockMvc.perform(get("/api/").with(user("user").roles("ADMIN"))).andExpect(status().isOk())
				.andExpect(content().string(containsString("API Home")))
				.andExpect(content().string(containsString("Login")))
				.andExpect(content().string(containsString("networks")))
				.andExpect(content().string(containsString("shows")))
				.andExpect(content().string(containsString("roamiosp")))
				.andExpect(content().string(containsString("cablecard")))
				.andExpect(content().string(containsString("roamionpl")))
				.andExpect(content().string(containsString("ota")))
				.andExpect(content().string(containsString("roamiotodo")));
	}

}

