package org.ligoj.app.resource.plugin;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import org.ligoj.bootstrap.AbstractDataGeneratorTest;

/**	
 * Test class of {@link CurlProcessor}
 */
public class CurlProcessorTest extends AbstractDataGeneratorTest {

	/**
	 * port used for proxy
	 */
	private static final int PROXY_PORT = 8121;

	@Test
	public void testX509() {
		final CurlProcessor.X509TrustManager x509TrustManager = new CurlProcessor.X509TrustManager();
		x509TrustManager.checkClientTrusted(null, null);
		x509TrustManager.checkServerTrusted(null, null);
		Assert.assertEquals(0, x509TrustManager.getAcceptedIssuers().length);
		Assert.assertNotNull(new CurlProcessor().getHttpClient());
	}

	@Test(expected = IllegalStateException.class)
	public void testX509Failed() {
		CurlProcessor.newSslContext("none");
	}

	@Test
	public void testGet() {
		httpServer.stubFor(get(urlPathEqualTo("/")).willReturn(aResponse().withStatus(HttpStatus.SC_OK).withBody("CONTENT")));
		httpServer.start();

		final CurlProcessor processor = new CurlProcessor();
		final String downloadPage = processor.get("http://localhost:" + MOCK_PORT);
		Assert.assertEquals("CONTENT", downloadPage);
	}

	@Test
	public void testGetFailed() {
		final CurlProcessor processor = new CurlProcessor();
		final CurlRequest curlRequest = new CurlRequest(null, "http://localhost:" + MOCK_PORT, "CONTENT");
		curlRequest.setSaveResponse(true);
		processor.process(curlRequest);
		Assert.assertNull(curlRequest.getResponse());
	}

	@Test
	public void testPost() {
		httpServer.stubFor(post(urlPathEqualTo("/")).willReturn(aResponse().withStatus(HttpStatus.SC_OK).withBody("CONTENT")));
		httpServer.start();

		final CurlProcessor processor = new CurlProcessor();
		final CurlRequest curlRequest = new CurlRequest("POST", "http://localhost:" + MOCK_PORT, "CONTENT");
		curlRequest.setSaveResponse(true);
		Assert.assertTrue(processor.process(curlRequest));
		Assert.assertEquals("CONTENT", curlRequest.getResponse());
	}

	@Test
	public void testHeaders() {
		httpServer.stubFor(get(urlPathEqualTo("/")).willReturn(aResponse().withStatus(HttpStatus.SC_OK).withBody("CONTENT")));
		httpServer.start();

		final CurlProcessor processor = new CurlProcessor();
		Assert.assertEquals("CONTENT", processor.get("http://localhost:" + MOCK_PORT, "Content-Type:text/html"));
	}

	@Test
	public void testGetRedirected() {
		httpServer.stubFor(get(urlPathEqualTo("/"))
				.willReturn(aResponse().withStatus(HttpStatus.SC_MOVED_TEMPORARILY).withHeader("Location", "http://www.google.fr")));
		httpServer.start();

		final CurlProcessor processor = new CurlProcessor();
		final String downloadPage = processor.get("http://localhost:" + MOCK_PORT);
		Assert.assertNull(downloadPage);
	}

	@Test
	public void testProcessNext() {
		httpServer.stubFor(get(urlPathEqualTo("/")).willReturn(aResponse().withStatus(HttpStatus.SC_OK).withBody("CONTENT")));
		httpServer.start();

		final List<CurlRequest> requests = new ArrayList<>();

		final CurlProcessor processor = new CurlProcessor();
		final CurlRequest curlRequest = new CurlRequest("GET", "http://localhost:" + MOCK_PORT, null);
		curlRequest.setSaveResponse(true);
		requests.add(curlRequest);
		requests.add(new CurlRequest("GET", "http://localhost:" + MOCK_PORT, null));
		Assert.assertTrue(processor.process(requests));
		Assert.assertEquals("CONTENT", curlRequest.getResponse());

		// Continue the execution
		processor.setCallback(new DefaultHttpResponseCallback());
		Assert.assertTrue(processor.process(curlRequest));
		Assert.assertEquals("CONTENT", curlRequest.getResponse());
	}

	@Test
	public void testProxy() {
		// set proxy configuration and proxy server
		System.setProperty("https.proxyHost", "localhost");
		System.setProperty("https.proxyPort", String.valueOf(8121));
		final WireMockServer proxyServer = new WireMockServer(8121);
		proxyServer.stubFor(get(WireMock.urlMatching(".*")).willReturn(aResponse().proxiedFrom("http://localhost:" + MOCK_PORT)));
		proxyServer.start();

		// set main http server
		httpServer.stubFor(get(urlPathEqualTo("/")).willReturn(aResponse().withStatus(HttpStatus.SC_OK).withBody("CONTENT")));
		httpServer.start();

		// launch request
		final CurlProcessor processor = new CurlProcessor();
		final String downloadPage = processor.get("http://localhost:" + PROXY_PORT);
		Assert.assertEquals("CONTENT", downloadPage);
		// clean proxy configuration
		System.clearProperty("https.proxyHost");
		System.clearProperty("https.proxyPort");
		proxyServer.stop();
	}

	protected WireMockServer httpServer;

	@Before
	public void prepareMockServer() {
		if (httpServer != null) {
			throw new IllegalStateException("A previous HTTP server was already created");
		}
		httpServer = new WireMockServer(MOCK_PORT);
		System.setProperty("http.keepAlive", "false");
	}

	@After
	public void shutDownMockServer() {
		System.clearProperty("http.keepAlive");
		if (httpServer != null) {
			httpServer.stop();
		}
	}
}
