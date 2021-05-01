/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.api;

/**
 * A configurable plug-in manage some extra configuration.
 */
@FunctionalInterface
public interface ConfigurablePlugin {

	/**
	 * Return the configuration of given subscription.
	 *
	 * @param subscription the subscription attached to a configurable service or tool.
	 * @return the configuration of given subscription.
	 * @throws Exception Configuration build failed. This error is caught at higher level.
	 */
	Object getConfiguration(int subscription) throws Exception; // NOSONAR Every thing could happen

}
