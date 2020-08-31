package com.dea42.genspring.controller;

import javax.validation.ConstraintValidatorContext;

import org.apache.commons.beanutils.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.dea42.genspring.service.AccountServices;
import com.dea42.genspring.utils.Utils;

/**
 * Check to see if email is already used by a user if used on form instead of
 * field then ignores if idFieldName > 0
 * 
 * @author avata
 *
 */
public class UniqueEmailValidator extends BaseConstraintValidator<UniqueEmail, Object> {

	@Autowired
	private AccountServices accountServices;

	@Override
	public void initialize(final UniqueEmail constraintAnnotation) {
		initialize(constraintAnnotation.fieldName(), null, constraintAnnotation.idFieldName(),
				constraintAnnotation.message());
	}

	@Override
	public boolean isValid(final Object formBean, final ConstraintValidatorContext context) {
		String messageTemplate;
		try {
			if (!isNew(formBean, context))
				return true;

			if (!accountServices.isEmailAlreadyInUse(BeanUtils.getProperty(formBean, fieldName)))
				return true;

			messageTemplate = Utils.getProp(bundle, msgKey, msgKey);
		} catch (final Exception e) {
			LOGGER.error(e.getMessage(), e);
			messageTemplate = e.getLocalizedMessage();
		}
		storeErrors(context, messageTemplate);
		return false;
	}

}