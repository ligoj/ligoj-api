/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.model;

import java.util.regex.Pattern;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;

/**
 * Simple regular expression extension to validate a node identifier.
 */
public class NodeIdValidator implements ConstraintValidator<NodeId, String> {

	/**
	 * A valid node identifier pattern.
	 */
	public static final Pattern ID_PATTERN = Pattern.compile("[a-z]{1,100}(:[a-z0-9]{1,50}(-[a-z0-9]{1,50}){0,10}){1,5}");

	@Override
	public void initialize(final NodeId annotation) {
		// Nothing to initialize
	}

	@Override
	public boolean isValid(final String value, final ConstraintValidatorContext context) {
		return ID_PATTERN.matcher(StringUtils.trimToEmpty(value)).matches();
	}
}