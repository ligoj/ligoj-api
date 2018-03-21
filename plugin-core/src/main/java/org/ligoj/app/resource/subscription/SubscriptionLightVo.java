/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.subscription;

import lombok.Getter;
import lombok.Setter;

/**
 * Light subscription's details.
 */
@Getter
@Setter
public class SubscriptionLightVo {

	/**
	 * Subscription identifier.
	 */
	private int id;
	
	/**
	 * Subscribed node.
	 */
	private String node;
	
	/**
	 * Subscribing project.
	 */
	private int project;

}
