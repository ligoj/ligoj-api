/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.plugin;

import java.nio.charset.StandardCharsets;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.auth.AUTH;

/**
 * Basic authenticated CURL processor. Credentials are sent in each request.
 *
 * @see SessionAuthCurlProcessor for session based cookie after a Basi authentication.
 * @see AUTH#WWW_AUTH_RESP
 */
public class AuthCurlProcessor extends CurlProcessor {

	private final String username;
	private final String password;
	private static final Base64 BASE64_CODEC = new Base64(0);

	/**
	 * Full constructor holding credential and callback.
	 *
	 * @param username
	 *            the user login. Empty or null login are accepted, but no authentication will be used.
	 * @param password
	 *            the user password or API token. <code>null</code> Password is converted to empty string, and still
	 *            used when user is not empty.
	 * @param callback
	 *            Not <code>null</code> {@link HttpResponseCallback} used for each response.
	 */
	public AuthCurlProcessor(final String username, final String password, final HttpResponseCallback callback) {
		super(callback);
		this.username = StringUtils.trimToNull(username);
		this.password = StringUtils.defaultString(password);
	}

	/**
	 * Constructor using parameters set.
	 *
	 * @param username
	 *            the user login. Empty or null login are accepted, but no authentication will be used.
	 * @param password
	 *            the user password or API token. <code>null</code> Password is converted to empty string, and still
	 *            used when user is not empty.
	 */
	public AuthCurlProcessor(final String username, final String password) {
		this(username, password, new DefaultHttpResponseCallback());
	}

	/**
	 * Process the given request.
	 */
	@Override
	protected boolean process(final CurlRequest request) {
		addAuthenticationHeader(request);
		return super.process(request);
	}

	/**
	 * Add the basic authentication header.
	 * 
	 * @param request
	 *            The request to complete header.
	 */
	protected void addAuthenticationHeader(final CurlRequest request) {
		// Check the authentication is needed or not
		if (username != null) {

			// Build the Basic authentication header
			final String tmp = username + ':' + password;

			// Use the preempted authentication processor
			request.getHeaders().put(AUTH.WWW_AUTH_RESP,
					"Basic " + BASE64_CODEC.encodeToString(tmp.getBytes(StandardCharsets.UTF_8)));
		}
	}

}
