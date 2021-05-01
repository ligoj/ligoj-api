/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.node;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;

/**
 * Statistics on node
 */
@Getter
public class NodeStatisticsVo {

	/**
	 * Node identifier.
	 */
	private String node;

	/**
	 * Statistics values
	 */
	private Map<String, Long> values = new HashMap<>();

	/**
	 * Constructor ensuring a provided node.
	 *
	 * @param node Node identifier.
	 */
	public NodeStatisticsVo(final String node) {
		this.node = node;
	}
}
