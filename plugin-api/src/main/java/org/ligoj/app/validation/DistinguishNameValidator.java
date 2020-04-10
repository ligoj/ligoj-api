/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.validation;

import java.util.regex.Pattern;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;

/**
 * Distinguish Name (DN) validator
 */
public class DistinguishNameValidator implements ConstraintValidator<DistinguishName, String> {
	
	/**
	 * Pattern reducing Rfc2253
	 */
	private static final String VALUE = "[\\p{L}\\d][\\p{L}\\d\\-:_ ]*";
	private static final String KEY_VALUE = VALUE + "=\\s*" + VALUE;
	private static final Pattern DN_PATTERN = Pattern.compile("(" + KEY_VALUE + "(,\\s*" + KEY_VALUE + ")*)?"); // NOSONAR - Not ReDoS

	@Override
	public void initialize(final DistinguishName annotation) {
		// Nothing to initialize
	}

	@Override
	public boolean isValid(final String value, final ConstraintValidatorContext context) {
		try {
			final String dn = StringUtils.trimToEmpty(value);
			// Check against Rfc2253
			new LdapName(dn).hashCode();
			
			// Check against our rules
			return DN_PATTERN.matcher(dn).matches();
		} catch (final InvalidNameException ne) { // NOSONAR
			// Ignore this error, this is a validation error
			return false;
		}
	}
}