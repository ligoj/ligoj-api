/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.log;

import lombok.Getter;
import lombok.Setter;

/**
 * Body of the POST endpoint used by an authenticated user to log a browser side error.
 */
@Getter
@Setter
public class UserLogEditionVo {

	/**
	 * Error message. Truncated to 2000 characters by the server.
	 */
	private String message;

	/**
	 * Path (without domain) of the page where the error occurred.
	 */
	private String url;
}
