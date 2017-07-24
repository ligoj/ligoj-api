package org.ligoj.app.resource.subscription;

import javax.validation.constraints.Min;

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
	@Min(1)
	@Setter
	private int project;

	/**
	 * Subscription identifier (only for update).
	 */
	private Integer id;
}
