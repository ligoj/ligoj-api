package org.ligoj.app.resource.plugin;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

/**
 * Test class of {@link OnlyRedirectHttpResponseCallback}
 */
public class OnlyRedirectHttpResponseCallbackTest {

	@Test
	public void testLocation() {
		Assertions.assertFalse(new OnlyRedirectHttpResponseCallback().acceptLocation(null));
		Assertions.assertTrue(new OnlyRedirectHttpResponseCallback().acceptLocation("/"));
	}

	@Test
	public void testStatus() {
		Assertions.assertFalse(new OnlyRedirectHttpResponseCallback().acceptStatus(HttpServletResponse.SC_ACCEPTED));
		Assertions.assertTrue(new OnlyRedirectHttpResponseCallback().acceptStatus(HttpServletResponse.SC_MOVED_TEMPORARILY));
	}

	@Test
	public void testResponseNotMoved() {
		final CloseableHttpResponse response = Mockito.mock(CloseableHttpResponse.class);
		final StatusLine statusLine = Mockito.mock(StatusLine.class);
		Mockito.when(response.getStatusLine()).thenReturn(statusLine);
		Mockito.when(statusLine.getStatusCode()).thenReturn(HttpServletResponse.SC_OK);
		Assertions.assertFalse(new OnlyRedirectHttpResponseCallback().acceptResponse(response));
	}

	@Test
	public void testResponseNoLocation() {
		final CloseableHttpResponse response = Mockito.mock(CloseableHttpResponse.class);
		final StatusLine statusLine = Mockito.mock(StatusLine.class);
		Mockito.when(response.getStatusLine()).thenReturn(statusLine);
		Mockito.when(statusLine.getStatusCode()).thenReturn(HttpServletResponse.SC_MOVED_TEMPORARILY);
		Assertions.assertFalse(new OnlyRedirectHttpResponseCallback().acceptResponse(response));
	}

	@Test
	public void testResponseEmptyLocation() {
		final CloseableHttpResponse response = Mockito.mock(CloseableHttpResponse.class);
		final StatusLine statusLine = Mockito.mock(StatusLine.class);
		final Header header = Mockito.mock(Header.class);
		Mockito.when(response.getFirstHeader(ArgumentMatchers.eq("location"))).thenReturn(header);
		Mockito.when(response.getStatusLine()).thenReturn(statusLine);
		Mockito.when(statusLine.getStatusCode()).thenReturn(HttpServletResponse.SC_MOVED_TEMPORARILY);
		Assertions.assertFalse(new OnlyRedirectHttpResponseCallback().acceptResponse(response));
	}

	@Test
	public void testResponse() {
		final CloseableHttpResponse response = Mockito.mock(CloseableHttpResponse.class);
		final StatusLine statusLine = Mockito.mock(StatusLine.class);
		final Header header = Mockito.mock(Header.class);
		Mockito.when(response.getFirstHeader(ArgumentMatchers.eq("location"))).thenReturn(header);
		Mockito.when(header.getValue()).thenReturn("/");
		Mockito.when(response.getStatusLine()).thenReturn(statusLine);
		Mockito.when(statusLine.getStatusCode()).thenReturn(HttpServletResponse.SC_MOVED_TEMPORARILY);
		Assertions.assertTrue(new OnlyRedirectHttpResponseCallback().acceptResponse(response));
	}
}
