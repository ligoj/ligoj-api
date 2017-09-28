package org.ligoj.app.resource.plugin;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 * HTTP query to execute.
 */
@Getter
public class CurlRequest {

	/**
	 * Is the response has to be saved in "response"
	 */
	@Setter
	private boolean saveResponse;

	/**
	 * Two way relationship.
	 */
	protected CurlProcessor processor;

	/**
	 * The response. May be <code>null</code>.
	 */
	@Setter
	private String response;

	/**
	 * Optional callback handler.
	 */
	private final HttpResponseCallback callback;

	/**
	 * HTTP method to execute, upper case.
	 */
	private String method;

	/**
	 * URL encoded to execute.
	 */
	private String url;

	/**
	 * Nullable encoded entity content to send.
	 */
	private String content;

	/**
	 * Nullable headers
	 */
	private final Map<String, String> headers;

	/**
	 * Counter inside processor. Is updated when the processor execute this
	 * request.
	 */
	protected int counter;

	/**
	 * Optional request timeout in milliseconds.
	 */
	@Getter
	@Setter
	protected Integer timeout;

	/**
	 * All arguments constructor.
	 * 
	 * @param method
	 *            HTTP method to execute, upper case.
	 * @param url
	 *            URL encoded to execute.
	 * @param content
	 *            Nullable encoded entity content to send.
	 * @param callback
	 *            Optional callback handler.
	 * @param headers
	 *            Optional headers <code>name:value</code>.
	 */
	public CurlRequest(final String method, final String url, final String content, final HttpResponseCallback callback,
			final String... headers) {
		this.method = method;
		this.url = url;
		this.content = content;
		this.headers = new HashMap<>();
		this.callback = callback;
		for (final String header : headers) {
			final String[] split = header.split(":");
			this.headers.put(split[0], split[1]);
		}
	}

	/**
	 * All arguments constructor but callback processor.
	 * 
	 * @param method
	 *            HTTP method to execute, upper case.
	 * @param url
	 *            URL encoded to execute.
	 * @param content
	 *            Nullable encoded entity content to send.
	 * @param headers
	 *            Optional headers <code>name:value</code>.
	 */
	public CurlRequest(final String method, final String url, final String content, final String... headers) {
		this(method, url, content, null, headers);
	}
}
