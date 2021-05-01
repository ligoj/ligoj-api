/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.api;

/**
 * node status
 */
public enum NodeStatus {
	/**
	 * node is up
	 */
	UP,
	/**
	 * node is down
	 */
	DOWN;

	/**
	 * is status up ?
	 *
	 * @return true if up
	 */
	public boolean isUp() {
		return this == UP;
	}

	/**
	 * get status
	 *
	 * @param isUp is up ?
	 * @return status
	 */
	public static NodeStatus getValue(final boolean isUp) {
		if (isUp) {
			return UP;
		}
		return DOWN;
	}
}
