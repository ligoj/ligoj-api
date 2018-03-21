/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.node;

import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * Parameter value for create and attached to a node.
 */
@Getter
@Setter
public class ParameterValueNodeVo extends ParameterValueCreateVo {
	
	/**
	 * The related node's identifier.
	 */
	@NotNull
	private String node;
}
