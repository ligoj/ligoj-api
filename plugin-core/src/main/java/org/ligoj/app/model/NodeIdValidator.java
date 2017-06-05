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
	public static final Pattern ID_PATTERN = Pattern.compile("[a-z]+(:[a-z0-9]+)+");

	@Override
	public void initialize(final NodeId annotation) {
		// Nothing to initialize
	}

	@Override
	public boolean isValid(final String value, final ConstraintValidatorContext context) {
		return ID_PATTERN.matcher(StringUtils.trimToEmpty(value)).matches();
	}
}