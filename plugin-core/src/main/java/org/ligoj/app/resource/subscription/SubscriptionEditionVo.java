package org.ligoj.app.resource.subscription;

import javax.validation.constraints.Positive;

import org.ligoj.app.resource.node.AbstractParameteredVo;

import lombok.Getter;
import lombok.Setter;

/**
 * A subscription data edition.
 */
@Getter
public class SubscriptionEditionVo extends AbstractParameteredVo {

	/**
	 * Project identifier.
	 */
	@Positive
	@Setter
	private int project;

	/**
	 * Subscription identifier (only for update).
	 */
	@Positive
	private Integer id;
}
