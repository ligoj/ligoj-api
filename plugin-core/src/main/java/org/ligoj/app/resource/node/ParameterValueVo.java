/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.node;

import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;

import org.ligoj.app.api.NodeVo;

import lombok.Getter;
import lombok.Setter;

/**
 * Parameter value details.
 */
@Getter
@Setter
public class ParameterValueVo extends BasicParameterValueVo {

	/**
	 * Parameter definition
	 */
	@NotNull
	@ManyToOne
	private ParameterVo parameter;

	/**
	 * Optional linked node providing this value. When <code>null</code>, the value is provided by the current
	 * subscription.
	 */
	private NodeVo node;
}
