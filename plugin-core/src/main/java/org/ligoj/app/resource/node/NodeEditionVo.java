package org.ligoj.app.resource.node;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotBlank;
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

	@NotBlank
	@NotNull
	private String name;

	/**
	 * The node identifier.
	 */
	@NotBlank
	@NotNull
	@NodeId
	String id;

	@Override
	public String getRefined() {
		return getNode();
	}

}
