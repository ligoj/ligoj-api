/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.subscription;

import java.io.Serializable;
import java.util.Map;

import org.ligoj.app.api.NodeStatus;
import org.ligoj.app.api.NodeVo;
import org.ligoj.app.iam.SimpleUser;
import org.ligoj.bootstrap.core.AuditedBean;

import lombok.Getter;
import lombok.Setter;

/**
 * Subscription's details.
 */
@Getter
@Setter
public class SubscriptionVo extends AuditedBean<SimpleUser, Integer> implements Serializable {

	/**
	 * SID, for Hazelcast
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Parameters attached to this subscription.
	 */
	private Map<String, Serializable> parameters;

	/**
	 * last event attached to this subscription
	 */
	private NodeStatus status;

	/**
	 * Subscribed service : directly a service, a tool or a node inside a tool.
	 */
	private NodeVo node;

}
