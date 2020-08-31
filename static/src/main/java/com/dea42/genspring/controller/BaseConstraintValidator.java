package com.dea42.genspring.controller;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.ResourceBundle;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Based on
 * https://github.com/StackAbuse/learning-spring/blob/master/password-strength/src/main/java/com/mynotes/spring/mvc/passwordstrength/constraint/PasswordConstraintValidator.java
 *
 */
public class BaseConstraintValidator<A extends Annotation, T> implements ConstraintValidator<A, T> {
	protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass().getName());
	protected String fieldName;
	protected String secondFieldName;
	// Assumed to be and int or long and 0 means new record
	protected String idfieldName;
	// Note key is assumed but passed text is used if key not found in messages
	// properties
	protected String msgKey;
	protected ResourceBundle bundle;

	public void initialize(String fieldName, String secondFieldName, String idfieldName, String msgKey) {
		this.fieldName = fieldName;
		this.secondFieldName = secondFieldName;
		this.idfieldName = idfieldName;
		this.msgKey = msgKey;
		bundle = ResourceBundle.getBundle("messages");
	}

	/**
	 * Get data from field from both raw field and bean with multiple fields data.
	 * 
	 * @param data
	 * @param fieldName
	 * @return
	 */
	protected String getStringField(final Object data, String fieldName) {
		String rtn = null;
		if (data != null) {
			if (data instanceof String) {
				rtn = (String) data;
			} else {
				try {
					rtn = BeanUtils.getProperty(data, fieldName);
				} catch (IllegalAccessException e) {
					LOGGER.error(fieldName, e);
				} catch (InvocationTargetException | NoSuchMethodException e) {
					// mainly to help debug code
					rtn = "" + data;
				}
			}
		}
		return rtn;
	}

	/**
	 * if id field == 0 return true else false Move to common class
	 * 
	 * @param data
	 * @param context
	 * @return
	 */
	protected boolean isNew(final Object data, final ConstraintValidatorContext context) {
		try {
			Long id = Long.parseLong(BeanUtils.getProperty(data, idfieldName));
			return (id != null && id == 0);
		} catch (InvocationTargetException | NoSuchMethodException e) {
			// not here / accessible so assume not new
		} catch (Exception e) {
			LOGGER.warn(fieldName, e);
		}
		return false;
	}

	@Override
	public boolean isValid(Object value, ConstraintValidatorContext context) {
		// Should always be overridden by child but needs to be here for
		// ConstraintValidator
		return false;
	}

	/**
	 * Put error(s) into context for page to display
	 * 
	 * @param context
	 * @param messageTemplate
	 */
	protected void storeErrors(final ConstraintValidatorContext context, String messageTemplate) {
		context.buildConstraintViolationWithTemplate(messageTemplate).addConstraintViolation()
				.disableDefaultConstraintViolation();

	}
}