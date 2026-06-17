/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.log;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * A user log business object returned by the GET endpoint.
 */
@Getter
@Setter
public class UserLogVo {

	/**
	 * Log identifier.
	 */
	private Integer id;

	/**
	 * Login of the user owning this log.
	 */
	private String user;

	/**
	 * Date of the error.
	 */
	private Instant date;

	/**
	 * Error message.
	 */
	private String message;

	/**
	 * Path (without domain) of the page where the error occurred.
	 */
	private String url;
}
