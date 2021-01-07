package com.dea42.genspring;

import java.math.BigDecimal;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.junit4.SpringRunner;

import com.dea42.genspring.entity.Sheet2;
import com.dea42.genspring.search.Sheet2SearchForm;
import com.dea42.genspring.service.Sheet2Services;

import junit.framework.TestCase;
import lombok.extern.slf4j.Slf4j;

/**
 * Title: Sheet2SearchTest <br>
 * Description: Does custom regression tests of Sheet2 search from service to
 * DB. <br>
 * 
 * @author avatar42<br>
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class Sheet2SearchTest extends TestCase {

	@Autowired
	private Sheet2Services sheet2Services;

	private Page<Sheet2> confirmGotResult(Sheet2SearchForm form, int expectedID) {
		Page<Sheet2> list = sheet2Services.listAll(form);
		assertNotNull("Checking return not null", list);
		assertTrue("Checking at least 1 return", list.toList().size() > 0);
		boolean found = false;
		for (Sheet2 s2 : list) {
			if (s2.getId() == expectedID)
				found = true;
			log.info(s2.toString());
		}

		assertTrue("Looking for record ID " + expectedID + " in results", found);
		return list;
	}

	@Test
	public void testSpecifications() {
		BigDecimal decimalMin;
		BigDecimal decimalMax;
		Integer integerMin;
		Integer integerMax;
		String text;
		int id = 3;

		Sheet2SearchForm form = null;
		Page<Sheet2> list = confirmGotResult(form, id);

		// key searches off 3rd record values.
		Sheet2 sheet2c = list.toList().get(id - 1);
		id = sheet2c.getId();
		text = sheet2c.getText();

		decimalMin = sheet2c.getDecimalfield();
		decimalMax = sheet2c.getDecimalfield().add(new BigDecimal(100));
		integerMin = sheet2c.getIntegerfield();
		integerMax = sheet2c.getIntegerfield() + 10;

		form = new Sheet2SearchForm();
		list = confirmGotResult(form, id);

		form.setText(text.substring(0, 6) + "%");
		list = confirmGotResult(form, id);

		form.setText("%" + text.substring(5, 6) + "%");
		list = confirmGotResult(form, id);

		form.setText("Text%");

		form.setDecimalfieldMin(decimalMin);
		form.setDecimalfieldMax(decimalMax);
		list = confirmGotResult(form, id);

		form.setIntegerfieldMin(integerMin);
		form.setIntegerfieldMax(integerMax);
		list = confirmGotResult(form, id);

		log.debug("done - breakpoint");
	}
}
