package org.ligoj.app.model;

import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

/**
 * Long task status where locked resource is a subscription.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class AbstractLongTaskSubscription extends AbstractLongTask<Subscription, Integer> {

	private static final long serialVersionUID = 1L;

	@ManyToOne
	@JsonIgnore
	@NotNull
	private Subscription locked;

}
