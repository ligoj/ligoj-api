package org.ligoj.app.resource.node;

import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Parameter value for create.
 */
@Getter
@Setter
@ToString(of = "parameter")
public class ParameterValueCreateVo extends BasicParameterValueVo {
	
	/**
	 * Parameter's identifier.
	 */
	@NotNull
	private String parameter;
	
	/**
	 * When <code>true</code>,  the values is considered as set to its old value.
	 */
	private boolean untouched;
}
