package org.ligoj.app.iam;

/**
 * Password generator extension point.
 */
public interface IPasswordGenerator {

	/**
	 * Set a generated password to given user. The password may be generated and
	 * sent to user and may requires additional steps to be fully available to
	 * this user.
	 * 
	 * @param user
	 *            The user identifier.
	 */
	void generate(String user);

	/**
	 * Set a generated password to given user. The password may be generated and
	 * sent to user and the administrator having reset it and may requires
	 * additional steps to be fully available to this user.
	 * 
	 * @param user
	 *            The user identifier.
	 * @param admin
	 *            The administrator identifier.
	 * @return The generated password.
	 */
	String generate(String user, String admin);
}
