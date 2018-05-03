/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.node;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.ligoj.app.api.SubscriptionMode;
import org.ligoj.app.model.NodeId;

import lombok.Getter;
import lombok.Setter;

/**
 * A bean accepting parameters and linked to a node.
 */
@Getter
@Setter
public abstract class AbstractParameterizedVo {

	/**
	 * Service (node, ...) identifier.
	 */
	@NotBlank
	@NotNull
	@NodeId
	private String node;

	/**
	 * Defined parameters for this subscription.
	 */
	@Valid
	private List<ParameterValueCreateVo> parameters;

	/**
	 * The restricted subscription mode.
	 */
	@NotNull
	private SubscriptionMode mode = SubscriptionMode.ALL;

}
