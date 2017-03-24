package org.ligoj.app.api;

/**
 * Password generator extension point.
 */
@FunctionalInterface
public interface IPasswordGenerator {

	/**
	 * Set a generated password to given user. The password may be generated and sent to user and may requires additional steps to be fully available to this user.
	 * 
	 * @param user
	 *            The user identifier.
	 */
	void generate(String user);
}
