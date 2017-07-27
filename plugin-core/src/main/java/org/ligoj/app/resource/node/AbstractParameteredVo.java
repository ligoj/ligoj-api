package org.ligoj.app.resource.node;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotBlank;
import org.ligoj.app.api.SubscriptionMode;
import org.ligoj.app.model.NodeId;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class AbstractParameteredVo {

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
