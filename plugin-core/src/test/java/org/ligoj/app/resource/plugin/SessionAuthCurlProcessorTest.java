package org.ligoj.app.resource.plugin;

import org.apache.http.auth.AUTH;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test class of {@link SessionAuthCurlProcessor}
 */
public class SessionAuthCurlProcessorTest {

	/**
	 * First request means authentication token sent.
	 */
	@Test
	public void processFirstRequest() {
		final CurlRequest request = new CurlRequest("", "", "");
		Assert.assertTrue(new SessionAuthCurlProcessor("junit", "passwd") {
			@Override
			protected boolean call(final CurlRequest request, final String url) throws Exception {
				return true;
			}
		}.process(request));
		Assert.assertEquals("Basic anVuaXQ6cGFzc3dk", request.getHeaders().get(AUTH.WWW_AUTH_RESP));
	}

	/**
	 * Not first request means the authentication token is not sent again.
	 */
	@Test
	public void process() {
		final CurlRequest request = new CurlRequest("", "", "");
		request.counter = 1;
		Assert.assertTrue(new SessionAuthCurlProcessor("junit", "passwd") {
			@Override
			protected boolean call(final CurlRequest request, final String url) throws Exception {
				return true;
			}
		}.process(request));
		Assert.assertTrue(request.getHeaders().isEmpty());
	}

}
