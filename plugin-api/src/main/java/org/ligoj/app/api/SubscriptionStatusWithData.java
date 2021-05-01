/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.api;

import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * Subscription status containing status and also optional data remotely got.
 */
@Getter
public class SubscriptionStatusWithData {

	/**
	 * Subscription identifier. Not <code>null</code> only when the status is proceeded during a refresh on a persisted
	 * subscription.
	 */
	@Setter
	private Integer id;

	/**
	 * Node or subscription status.
	 */
	private final NodeStatus status;

	/**
	 * Node's identifier of this subscription.
	 */
	@Setter
	private String node;

	/**
	 * Project's identifier of this subscription. Not <code>null</code> only when the status is proceeded during a
	 * refresh on a persisted subscription.
	 */
	@Setter
	private Integer project;

	/**
	 * Optional data. Can be <code>null</code> or empty depending the availability of remote node and relevant data to
	 * retrieve.
	 */
	private Map<String, Object> data = new HashMap<>();

	/**
	 * Subscription parameter values. Only not secured parameters are there.
	 */
	@Setter
	private Map<String, String> parameters;

	/**
	 * Default constructor with a positive status : {@link NodeStatus#UP}
	 */
	public SubscriptionStatusWithData() {
		// Default constructor
		this(true);
	}

	/**
	 * Default constructor with a status parameter.
	 *
	 * @param status The status value.
	 */
	public SubscriptionStatusWithData(final boolean status) {
		this.status = NodeStatus.getValue(status);
	}

	/**
	 * Put a data along this status.
	 *
	 * @param property The property name.
	 * @param value    The data value.
	 */
	public void put(@NotNull final String property, final Object value) {
		data.put(property, value);
	}

}
