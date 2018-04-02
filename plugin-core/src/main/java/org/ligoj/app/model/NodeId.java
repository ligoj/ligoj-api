/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

/**
 * Node identifier constraint.
 */
@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NodeIdValidator.class)
public @interface NodeId {

	/**
	 * Default Key message.
	 * @return Message key.
	 */
	String message() default "org.ligoj.app.model.NodeId.message";

	/**
	 * JSR-303 requirement.
	 * @return No groups.
	 */
	Class<?>[] groups() default {
		
	};

	/**
	 * JSR-303 requirement.
	 * @return No payloads.
	 */
	Class<? extends Payload>[] payload() default {
		
	};
}