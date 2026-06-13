/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.api;

import java.io.Serializable;
import java.util.Map;

import org.ligoj.app.model.Refining;

import lombok.Getter;
import lombok.Setter;

/**
 * Node's details.
 */
@Getter
@Setter
public class NodeVo extends AbstractNodeVo implements Refining<NodeVo> {

	/**
	 * SID, for Hazelcast
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Instance of tool proving the expected service.
	 */
	private NodeVo refined;

	/**
	 * The subscription mode.
	 */
	private SubscriptionMode mode = SubscriptionMode.ALL;

	/**
	 * Optional CSS classes used to render this node.
	 */
	private String uiClasses;

	/**
	 * Optional preferred CSS color (e.g. {@code "#0052CC"}) used to accent this
	 * node in the UI. Sourced from {@link org.ligoj.app.model.Node#getUiColor()};
	 * {@code null} when none is set, in which case the UI is expected to fall
	 * back to its own derivation.
	 */
	private String uiColor;

	/**
	 * Parameter values attached to this node directly of from one of its parent. So some parameters come from the
	 * parent node and are not directly linked to the current node.<br>
	 * Depending on the called service to build this node, sometimes for the performance purpose, the values are not
	 * fetch, and this property will stay <code>null</code>. The values of this {@link Map} are the true typed. The key
	 * is the parameter identifier.
	 */
	private Map<String, Serializable> parameters;

	/**
	 * When <code>false</code>, this node is considered as unavailable because of a missing resource, such as the
	 * plug-in. When <code>null</code>, the state has not been resolved.
	 */
	private Boolean enabled;
}
