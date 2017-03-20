package org.ligoj.app.resource.plugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;

/**
 * The default callback implementation. Stop the execution when a status above 302 is received. Store the last received
 * entity string.
 */
@Slf4j
public class DefaultHttpResponseCallback implements HttpResponseCallback {

	@Override
	public boolean onResponse(final CurlRequest request, final CloseableHttpResponse response) throws IOException, ParseException {

		// Read the response
		final HttpEntity entity = response.getEntity();
		log.info(response.getStatusLine().getStatusCode() + " " + request.getUrl());
		if (entity != null) {

			// Check the status
			if (!acceptResponse(response)) {
				final String responseString = EntityUtils.toString(entity);
				log.error(responseString);
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
	 */
	protected boolean acceptResponse(final CloseableHttpResponse response) {
		return acceptStatus(response.getStatusLine().getStatusCode());
	}

	/**
	 * Indicate the status is accepted.
	 */
	protected boolean acceptStatus(final int status) {
		return status <= HttpServletResponse.SC_NO_CONTENT;
	}

}
