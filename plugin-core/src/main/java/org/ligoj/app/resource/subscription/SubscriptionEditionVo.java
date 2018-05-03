/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.subscription;

import javax.validation.constraints.Positive;

import org.ligoj.app.resource.node.AbstractParameterizedVo;

import lombok.Getter;
import lombok.Setter;

/**
 * A subscription data edition.
 */
@Getter
public class SubscriptionEditionVo extends AbstractParameterizedVo {

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
