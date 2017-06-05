package org.ligoj.app.resource.node;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotBlank;
import org.ligoj.app.api.SubscriptionMode;
import org.ligoj.app.model.NodeId;
import org.ligoj.app.model.Refining;
import org.ligoj.bootstrap.core.INamableBean;

import lombok.Getter;
import lombok.Setter;

/**
 * Node object for edition.
 */
@Getter
@Setter
public class NodeEditionVo implements INamableBean<String>, Refining<String> {

	/**
	 * Identifier of this node. Must be defined even for the creation mode.
	 */
	@NotBlank
	@NotNull
	@NodeId
	private String id;

	@NotBlank
	@NotNull
	private String name;

	/**
	 * The refined node. Must be an existing tool.
	 */
	@NotBlank
	@NotNull
	@NodeId
	String refined;

	/**
	 * The restricted subscription mode. When <code>null</code>, the node cannot
	 * be used for any mode.
	 */
	private SubscriptionMode mode;
}
