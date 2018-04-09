/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.node;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

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
public class NodeEditionVo extends AbstractParameteredVo implements INamableBean<String>, Refining<String> {

	/**
	 * SID, for Hazelcast
	 */
	private static final long serialVersionUID = 1L;

	@NotBlank
	@NotNull
	private String name;
	
	/**
	 * When <code>true</code> the previous parameters are not updated.
	 */
	private boolean untouchedParameters;

	/**
	 * The node identifier.
	 */
	@NotBlank
	@NotNull
	@NodeId
	private String id;

	@Override
	public String getRefined() {
		return getNode();
	}

}
