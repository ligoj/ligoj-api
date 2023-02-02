/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.model;

import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotNull;

import org.ligoj.bootstrap.core.model.ToIdSerializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import lombok.Getter;
import lombok.Setter;

/**
 * Long task status where locked resource is a subscription.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class AbstractLongTaskSubscription extends AbstractLongTask<Subscription, Integer> {

	@ManyToOne
	@NotNull
	@JsonSerialize(using = ToIdSerializer.class)
	private Subscription locked;

}
