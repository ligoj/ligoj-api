package org.ligoj.app.api;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 * Node's details.
 */
@Getter
@Setter
public class NodeVo extends AbstractNodeVo {

	/**
	 * Instance of tool proving the expected service.
	 */
	private NodeVo refined;

	/**
	 * The subscription mode. When <code>null</code>, the node cannot be used for any mode.
	 */
	private SubscriptionMode mode;

	/**
	 * Optional CSS classes used to render this node.
	 */
	private String uiClasses;

	/**
	 * Parameter values attached to this node directly of from one of its parent. So some parameters come from the
	 * parent node and are not directly linked to the current node.<br>
	 * Depending on the called service to build this node, sometimes for the performance purpose, the values
	 * are not fetch, and this property will stay <code>null</code>. The values of this {@link Map} are the true
	 * typed. The key is the parameter identifier.
	 */
	private Map<String, Object> parameters;

}
