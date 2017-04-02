package org.ligoj.app;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Test;

import com.github.tomakehurst.wiremock.client.WireMock;

/**
 * Test class of {@link AbstractServerTest}
 */
public class TestAbstractServerTest extends AbstractServerTest {

	/**
	 * Only there fore coverage, no Spring involved.
	 */
	@Test
	public void startAutoStop() throws IOException {
		httpServer.stubFor(WireMock.get(WireMock.urlPathEqualTo("/")).willReturn(WireMock.aResponse().withStatus(HttpStatus.SC_OK).withBody("ok")));
		httpServer.start();
		Assert.assertEquals("ok", IOUtils.toString(new URL("http://localhost:" + MOCK_PORT + "/"), StandardCharsets.UTF_8.name()));
	}

	/**
	 * Only there fore coverage, no Spring involved.
	 */
	@Test
	public void startStop() throws IOException {
		httpServer.stubFor(WireMock.get(WireMock.urlPathEqualTo("/")).willReturn(WireMock.aResponse().withStatus(HttpStatus.SC_OK).withBody("ok")));
		httpServer.start();
		Assert.assertEquals("ok", IOUtils.toString(new URL("http://localhost:" + MOCK_PORT + "/"), StandardCharsets.UTF_8.name()));
		httpServer.stop();
		httpServer = null;
	}

	/**
	 * Only there fore coverage, no Spring involved.
	 */
	@Test
	public void startStop2() throws IOException {
		httpServer.stubFor(WireMock.get(WireMock.urlPathEqualTo("/")).willReturn(WireMock.aResponse().withStatus(HttpStatus.SC_OK).withBody("ok")));
		httpServer.start();
		Assert.assertEquals("ok", IOUtils.toString(new URL("http://localhost:" + MOCK_PORT + "/"), StandardCharsets.UTF_8.name()));
		httpServer.stop();
	}

	/**
	 * Only there fore coverage, no Spring involved.
	 */
	@Test(expected = IllegalStateException.class)
	public void prepareMockServerDuplicate() {
		prepareMockServer();
	}
}
