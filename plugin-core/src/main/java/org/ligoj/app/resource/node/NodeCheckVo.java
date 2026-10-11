/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.node;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Result of the connection check of a node configuration before it is saved. It never holds a parameter value.
 */
@Getter
@Setter
public class NodeCheckVo {

	/**
	 * <code>true</code> when the tool accepted the connection.
	 */
	private boolean up;

	/**
	 * The validation errors naming the faulty parameters, as in a 400 response: by parameter identifier, the errors with
	 * their <code>rule</code> and optional <code>parameters</code>. <code>null</code> without validation error.
	 */
	private Map<String, List<Map<String, Serializable>>> errors;

	/**
	 * The business error code, or <code>node-check-failed</code> when the tool failed without a known reason.
	 * <code>null</code> without such error.
	 */
	private String message;
}
