package org.ligoj.app.resource;

import org.junit.After;
import org.junit.Before;
import org.ligoj.app.AbstractJpaTest;

import com.github.tomakehurst.wiremock.WireMockServer;

/**
 * Test using mock http server.
 */
public abstract class AbstractServerTest extends AbstractJpaTest {

	protected WireMockServer httpServer;

	/**
	 * Prepare the server mock and check it is not already started.
	 */
	@Before
	public void prepareMockServer() {
		if (httpServer != null) {
			throw new IllegalStateException("A previous HTTP server was already created");
		}
		httpServer = new WireMockServer(MOCK_PORT);
		System.setProperty("http.keepAlive", "false");
	}

	/**
	 * Shutdown the server and clear the keep alive settings.
	 */
	@After
	public void shutDownMockServer() {
		System.clearProperty("http.keepAlive");
		if (httpServer != null) {
			httpServer.stop();
		}
	}

	@Override
	protected String getAuthenticationName() {
		return "fdaugan";
	}

}
