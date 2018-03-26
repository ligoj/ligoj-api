/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

/**
 * Test using mock http server.
 */
public abstract class AbstractServerTest extends AbstractAppTest {

	protected WireMockServer httpServer;

	/**
	 * Prepare the server mock and check it is not already started.
	 */
	@BeforeEach
	public void prepareMockServer() {
		if (httpServer != null) {
			throw new IllegalStateException("A previous HTTP server was already created");
		}
		
		// See https://github.com/tomakehurst/wiremock/issues/710
		httpServer = new WireMockServer(WireMockConfiguration.options().port(MOCK_PORT).jettyStopTimeout(10000L));
		System.setProperty("http.keepAlive", "false");
	}

	/**
	 * Shutdown the server and clear the keep alive settings.
	 */
	@AfterEach
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
