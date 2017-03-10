package org.ligoj.app.resource.subscription;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.validator.constraints.NotBlank;

import org.ligoj.app.api.SubscriptionMode;
import org.ligoj.app.resource.node.ParameterValueEditionVo;

/**
 * A subscription data edition.
 */
@Getter
@Setter
public class SubscriptionEditionVo {

	/**
	 * Service (node, ...) identifier.
	 */
	@NotBlank
	private String node;

	/**
	 * Project identifier.
	 */
	@Min(1)
	private int project;

	/**
	 * Subscription identifier (only for update).
	 */
	private Integer id;

	/**
	 * Defined parameters for this subscription.
	 */
	@Valid
	private List<ParameterValueEditionVo> parameters;
	
	@NotNull
	private SubscriptionMode mode = SubscriptionMode.LINK;
}
