package org.ligoj.app.iam;

/**
 * Password generator extension point.
 */
@FunctionalInterface
public interface IPasswordGenerator {

	/**
	 * Set a generated password to given user. The password may be generated and
	 * sent to user and may require additional steps to be fully available to
	 * this user.
	 * 
	 * @param user
	 *            The user identifier.
	 * @return The generated password.
	 */
	String generate(String user);
}
