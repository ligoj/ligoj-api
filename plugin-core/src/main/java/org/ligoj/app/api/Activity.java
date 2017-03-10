package org.ligoj.app.api;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

/**
 * Recorded activity.
 */
@Getter
@Setter
public class Activity {

	/**
	 * Last known connection. May be <code>null</code>.
	 */
	private Date lastConnection;
}
