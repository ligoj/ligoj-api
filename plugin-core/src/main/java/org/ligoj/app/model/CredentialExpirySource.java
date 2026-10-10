/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.model;

/**
 * Origin of the credential expiry of a node.
 */
public enum CredentialExpirySource {
	/**
	 * Entered by an administrator.
	 */
	MANUAL,

	/**
	 * Read from the tool by its plug-in during the status check. Wins over a manual value.
	 */
	DETECTED
}
