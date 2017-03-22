package org.ligoj.app.resource.plugin;

import java.io.IOException;

import org.apache.http.client.methods.CloseableHttpResponse;

/**
 * {@link CloseableHttpResponse} callback used for each response whatever the status.
 */
@FunctionalInterface
public interface HttpResponseCallback {

	/**
	 * Called when a response is received.
	 * 
	 * @param request
	 *            the original request.
	 * @param response
	 *            the received response.
	 * @return <code>true</code> to proceed the next request. <code>false</code> otherwise.
	 */
	boolean onResponse(CurlRequest request, CloseableHttpResponse response) throws IOException;
}
