/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.subscription;

import java.util.Collection;

import lombok.Getter;
import lombok.Setter;

/**
 * List of subscriptions, where structure is optimized for huge amount of subscriptions. Nodes are separated from the
 * subscribed projects.
 */
@Getter
@Setter
public class SubscriptionListVo {

	/**
	 * All related nodes.
	 */
	private Collection<SubscribedNodeVo> nodes;

	/**
	 * All visible projects.
	 */
	private Collection<SubscribingProjectVo> projects;

	/**
	 * All subscriptions of visible projects.
	 */
	private Collection<SubscriptionLightVo> subscriptions;

}
