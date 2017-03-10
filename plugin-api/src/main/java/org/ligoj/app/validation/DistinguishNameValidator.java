package org.ligoj.app.validation;

import java.util.regex.Pattern;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;
import org.springframework.ldap.NamingException;

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
			org.springframework.ldap.support.LdapUtils.newLdapName(dn);
			
			// Check against our rules
			return DN_PATTERN.matcher(dn).matches();
		} catch (final NamingException ne) { // NOSONAR
			// Ignore this error, this is a validation error
			return false;
		}
	}
}