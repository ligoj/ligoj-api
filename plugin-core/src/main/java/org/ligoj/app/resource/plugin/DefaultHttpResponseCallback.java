/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.plugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * The default callback implementation. Stop the execution when a status above 302 is received. Store the last received
 * entity string.
 */
@Slf4j
public class DefaultHttpResponseCallback implements HttpResponseCallback {

	@Override
	public boolean onResponse(final CurlRequest request, final CloseableHttpResponse response) throws IOException {

		// Read the response
		final HttpEntity entity = response.getEntity();
		log.info("{} {}", response.getStatusLine().getStatusCode(), request.getUrl());
		if (entity != null) {

			// Check the status
			if (!acceptResponse(response)) {
				log.error(EntityUtils.toString(entity));
				return false;
			}

			// Save the response as needed
			if (request.isSaveResponse()) {
				request.setResponse(EntityUtils.toString(entity, StandardCharsets.UTF_8));
			}
			entity.getContent().close();
		}
		return true;
	}

	/**
	 * Indicate the response is accepted.
	 * 
	 * @param response
	 *            The received response.
	 * @return <code>true</code> to proceed the next request. <code>false</code> otherwise.
	 */
	protected boolean acceptResponse(final CloseableHttpResponse response) {
		return acceptStatus(response.getStatusLine().getStatusCode());
	}

	/**
	 * Indicate the status is accepted.
	 * 
	 * @param status
	 *            The received status to accept.
	 * @return <code>true</code> to proceed the next request. <code>false</code> otherwise.
	 */
	protected boolean acceptStatus(final int status) {
		return status <= HttpServletResponse.SC_NO_CONTENT;
	}

}
