/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.iam;

import jakarta.ws.rs.core.Response.ResponseBuilder;

import org.springframework.security.core.Authentication;

/**
 * Login contributor. The unique function is called just after a successful authentication.
 */
@FunctionalInterface
public interface IAuthenticationContributor {

	/**
	 * Call to contribute to the final response after the authentication.
	 *
	 * @param response       The current {@link ResponseBuilder}. May have already been visited.
	 * @param authentication The current successful authentication, to be used while building the response.
	 */
	void accept(ResponseBuilder response, Authentication authentication);
}
