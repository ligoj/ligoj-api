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
public interface ServicePlugin extends FeaturePlugin {

	/**
	 * Delete a subscription.
	 * 
	 * @param subscription
	 *            the subscription identifier.
	 * @param remoteData
	 *            When <code>true</code>, remote data will be also destroyed.
	 */
	default void delete(int subscription, boolean remoteData) throws Exception { // NOSONAR Everything could happen
		// No custom data by default
	}

	/**
	 * Complete the subscription in creation mode. Link data still required.
	 * 
	 * @param subscription
	 *            the subscription identifier is being created.
	 */
	default void create(int subscription) throws Exception { // NOSONAR Everything could happen
		// No custom data by default
	}

	/**
	 * Complete the subscription in link mode.
	 * 
	 * @param subscription
	 *            the subscription identifier is being created.
	 */
	default void link(int subscription) throws Exception { // NOSONAR Everything could happen
		// No custom data by default
	}
}
