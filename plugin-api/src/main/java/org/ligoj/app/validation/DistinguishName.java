package org.ligoj.app.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

/**
 * Distinguish Name constraint.
 */
@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DistinguishNameValidator.class)
public @interface DistinguishName {

	/**
	 * Default Key message
	 */
	String message() default "org.ligoj.app.validation.DistinguishName.message";

	/**
	 * JSR-303 requirement.
	 */
	Class<?>[] groups() default {
		
	};

	/**
	 * JSR-303 requirement.
	 */
	Class<? extends Payload>[] payload() default {
		
	};
}