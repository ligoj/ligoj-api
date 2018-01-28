package org.ligoj.app.resource.plugin;

import org.apache.http.auth.AUTH;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test class of {@link AuthCurlProcessor}
 */
public class AuthCurlProcessorTest {

	/**
	 * Process with provided and not empty credentials.
	 */
	@Test
	public void process() {
		CurlRequest request = new CurlRequest("", "", "");
		final CurlProcessor processor = new AuthCurlProcessor("junit", "passwd") {
			@Override
			protected boolean call(final CurlRequest request, final String url) throws Exception {
				return true;
			}
		};
		processor.process(request);
		Assertions.assertEquals("Basic anVuaXQ6cGFzc3dk", request.getHeaders().get(AUTH.WWW_AUTH_RESP));
		request = new CurlRequest("", "", "");
		processor.process(request);
		Assertions.assertEquals("Basic anVuaXQ6cGFzc3dk", request.getHeaders().get(AUTH.WWW_AUTH_RESP));
	}

	/**
	 * Process without provided user.
	 */
	@Test
	public void processNoUser() {
		final CurlRequest request = new CurlRequest("", "", "");
		final CurlProcessor processor = new AuthCurlProcessor("", "any") {
			@Override
			protected boolean call(final CurlRequest request, final String url) throws Exception {
				return true;
			}
		};
		processor.process(request);
		Assertions.assertFalse(request.getHeaders().containsKey(AUTH.WWW_AUTH_RESP));
	}

}
