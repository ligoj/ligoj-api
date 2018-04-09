/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.project;

import java.util.List;

import org.ligoj.app.resource.subscription.SubscriptionVo;

import lombok.Getter;
import lombok.Setter;

/**
 * A fully described project.
 */
@Getter
@Setter
public class ProjectVo extends BasicProjectVo {

	/**
	 * SID, for Hazelcast
	 */
	private static final long serialVersionUID = 1L;

	private List<SubscriptionVo> subscriptions;

	/**
	 * Indicates the current user can manage the subscriptions of this project.
	 */
	private boolean manageSubscriptions;
}
