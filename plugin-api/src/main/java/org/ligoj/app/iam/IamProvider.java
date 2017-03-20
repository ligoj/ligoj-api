package org.ligoj.app.iam;

import org.springframework.security.core.Authentication;

/**
 * Identity and Access Management (IAM) provider of the application.
 */
public interface IamProvider {


	/**
	 * Authenticate the given token.
	 * 
	 * @param authentication
	 *            The current authentication to check.
	 * @return The validated authentication. May not be the original parameter. Never <code>null</code>.
	 */
	Authentication authenticate(Authentication authentication);

	/**
	 * IAM global configuration.
	 * 
	 * @return IAM global configuration.
	 */
	IamConfiguration getConfiguration();
}
