package com.dea42.genspring.controller;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import com.dea42.genspring.controller.ValidatePassword.List;

/**
 * Based on
 * https://github.com/StackAbuse/learning-spring/blob/master/password-strength/src/main/java/com/mynotes/spring/mvc/passwordstrength/constraint/ValidPassword.java
 *
 */
@Documented
@Constraint(validatedBy = PasswordConstraintValidator.class)
@Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE })
@Retention(RUNTIME)
@Repeatable(List.class)
public @interface ValidatePassword {

	String message() default "Invalid Password";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

	String fieldName() default "password";

	String idFieldName() default "id";

	/**
	 * Defines several <code>@ValidatePassword</code> annotations on the same
	 * element
	 *
	 * @see ValidatePassword
	 */
	@Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE })
	@Retention(RUNTIME)
	@Documented
	@interface List {
		ValidatePassword[] value();
	}

}