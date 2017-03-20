package org.ligoj.app.resource.plugin;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.ParseException;
import org.apache.http.client.methods.CloseableHttpResponse;

/**
 * This callback get the header from the response.
 */
public class HeaderHttpResponseCallback extends DefaultHttpResponseCallback {

	/**
	 * The header name to save as response.
	 */
	private String header;

	/**
	 * Simple constructor with header name for response.
	 * 
	 * @param header
	 *            The header name to save as response.
	 */
	public HeaderHttpResponseCallback(final String header) {
		this.header = header;
	}

	@Override
	public boolean onResponse(final CurlRequest request, final CloseableHttpResponse response) throws IOException, ParseException {
		super.onResponse(request, response);
		// Response is pre-checked
		final Header value = response.getFirstHeader(header);
		if (value == null) {
			// Header is not present
			request.setResponse(null);
			return false;
		}
		// Extract the value and save it in the response
		request.setResponse(value.getValue());
		return true;
	}

}
