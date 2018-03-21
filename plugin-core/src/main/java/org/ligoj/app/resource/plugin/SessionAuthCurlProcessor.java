/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.plugin;

/**
 * Basic authenticated CURL processor where credentials are only sent one in order to get a session cookie.
 */
public class SessionAuthCurlProcessor extends AuthCurlProcessor {

	/**
	 * Full constructor holding credential and callback.
	 * 
	 * @param username
	 *            the user login.
	 * @param password
	 *            the user password or API token.
	 * @param callback
	 *            Not <code>null</code> {@link HttpResponseCallback} used for each response.
	 */
	public SessionAuthCurlProcessor(final String username, final String password, final HttpResponseCallback callback) {
		super(username, password, callback);
	}

	/**
	 * Constructor using parameters set.
	 *
	 * @param username
	 *            the user login.
	 * @param password
	 *            the user password or API token.
	 */
	public SessionAuthCurlProcessor(final String username, final String password) {
		this(username, password, new DefaultHttpResponseCallback());
	}

	/**
	 * Manage authentication only for the first request.
	 */
	@Override
	protected void addAuthenticationHeader(final CurlRequest request) {
		if (request.getCounter() == 0) {
			// Manage authentication only for the first request.
			super.addAuthenticationHeader(request);
		}
	}

}
