/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.subscription;

import jakarta.validation.constraints.Positive;

import org.ligoj.app.resource.node.AbstractParameterizedVo;

import com.fasterxml.jackson.annotation.JsonProperty;

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
	 * Subscription identifier, never read from the input: a creation cannot target an existing subscription.
	 */
	@Positive
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	private Integer id;
}
