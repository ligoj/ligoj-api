/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.node;

import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * Parameter value for update. The related node or the related subscription
 * cannot be changed.
 */
@Getter
@Setter
public class ParameterValueNodeUpdateVo extends ParameterValueCreateVo {

	/**
	 * parameter value identifier.
	 */
	@NotNull
	private Integer id;
}
