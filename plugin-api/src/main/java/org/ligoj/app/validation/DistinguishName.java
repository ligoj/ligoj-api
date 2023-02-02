/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Distinguish Name constraint.
 */
@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DistinguishNameValidator.class)
public @interface DistinguishName {

	/**
	 * Default Key message
	 *
	 * @return Message key.
	 */
	String message() default "org.ligoj.app.validation.DistinguishName.message";

	/**
	 * JSR-303 requirement.
	 *
	 * @return Empty groups.
	 */
	Class<?>[] groups() default {

	};

	/**
	 * JSR-303 requirement.
	 *
	 * @return Empty payloads.
	 */
	Class<? extends Payload>[] payload() default {

	};
}