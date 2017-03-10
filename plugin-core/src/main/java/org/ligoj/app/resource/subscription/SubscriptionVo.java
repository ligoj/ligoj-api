package org.ligoj.app.resource.subscription;

import java.util.Map;

import org.ligoj.bootstrap.core.AuditedBean;
import org.ligoj.app.api.NodeStatus;
import org.ligoj.app.api.NodeVo;
import org.ligoj.app.api.SimpleUser;
import lombok.Getter;
import lombok.Setter;

/**
 * Subscription's details.
 */
@Getter
@Setter
public class SubscriptionVo extends AuditedBean<SimpleUser, Integer> {

	/**
	 * Parameters attached to this subscription.
	 */
	private Map<String, Object> parameters;

	/**
	 * last event attached to this subscription
	 */
	private NodeStatus status;

	/**
	 * Subscribed service : directly a service, a tool or a node inside a a tool.
	 */
	private NodeVo node;

}
