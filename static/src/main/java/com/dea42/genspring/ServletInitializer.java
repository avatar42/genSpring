package com.dea42.genspring;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Title: ServletInitializer <br>
 * Description: Class for configuring app in war. <br>
 * 
 * @author Gened by com.dea42.build.GenSpring version 0.2.3<br>
 * @version 1.0<br>
 */
public class ServletInitializer extends SpringBootServletInitializer {

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(WebAppApplication.class);
	}

}
