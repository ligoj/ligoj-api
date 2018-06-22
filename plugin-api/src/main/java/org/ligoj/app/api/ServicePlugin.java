/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.api;

/**
 * A plug-in. The plug-in behavior is massively based on naming convention. The rules are an extension of the ones of
 * {@link FeaturePlugin} for the key of the plug-in determines the required parent and the namespace for web assets. For
 * sample the key <code>service:s:t</code> means :
 * <ul>
 * <li>"service" is a constant part, and required. And will be published inside the "/service" REST namespace.</li>
 * <li>"s" is the first level of service. Lower case part, following the pattern [a-z\\d]+</li>
 * <li>"t" is the first level of service. Lower case part, following the pattern [a-z\\d]+</li>
 * </ul>
 */
public interface ServicePlugin extends org.ligoj.bootstrap.core.plugin.FeaturePlugin {

	/**
	 * Delete a subscription.
	 *
	 * @param subscription
	 *            the subscription identifier.
	 * @param remoteData
	 *            When <code>true</code>, remote data will be also destroyed.
	 * @throws Exception
	 *             Deletion failed. This error is caught at higher level.
	 */
	default void delete(int subscription, boolean remoteData) throws Exception { // NOSONAR Everything could happen
		// No custom data by default
	}

	/**
	 * Complete the subscription in creation mode. Link data still required.
	 *
	 * @param subscription
	 *            the subscription identifier is being created.
	 * @throws Exception
	 *             Create failed. This error is caught at higher level.
	 */
	default void create(int subscription) throws Exception { // NOSONAR Everything could happen
		// No custom data by default
	}

	/**
	 * Complete the subscription in link mode.
	 *
	 * @param subscription
	 *            the subscription identifier is being created.
	 * @throws Exception
	 *             Link failed. This error is caught at higher level.
	 */
	default void link(int subscription) throws Exception { // NOSONAR Everything could happen
		// No custom data by default
	}

	/**
	 * Delete a node.
	 *
	 * @param node
	 *            The node identifier.
	 * @param remoteData
	 *            When <code>true</code>, remote data will be also destroyed.
	 * @throws Exception
	 *             Delete failed. This error is caught at higher level.
	 */
	default void delete(String node, boolean remoteData) throws Exception { // NOSONAR Everything could happen
		// No custom data by default
	}
}
