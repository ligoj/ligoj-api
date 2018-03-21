/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.model;

import org.springframework.data.domain.Persistable;

/**
 * The plug-in configuration.
 */
public interface PluginConfiguration extends Persistable<Integer> {

	/**
	 * The related subscription of this configuration.
	 * 
	 * @return The related subscription of this configuration.
	 */
	Subscription getSubscription();
}
