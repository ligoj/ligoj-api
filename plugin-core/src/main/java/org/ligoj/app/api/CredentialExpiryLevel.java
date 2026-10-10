/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.api;

/**
 * Urgency of a credential expiry, see the threshold of <code>CredentialExpiryPolicy</code>.
 */
public enum CredentialExpiryLevel {
	/**
	 * The credentials expire within the warning threshold.
	 */
	WARNING,

	/**
	 * The credentials have expired.
	 */
	EXPIRED
}
