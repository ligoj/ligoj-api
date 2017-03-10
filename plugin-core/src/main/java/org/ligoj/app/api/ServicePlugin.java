package org.ligoj.app.api;

/**
 * A plug-in.
 */
public interface ServicePlugin {

	/**
	 * Delete a subscription.
	 * 
	 * @param subscription
	 *            the subscription identifier.
	 * @param deleteRemoteData
	 *            When <code>true</code>, remote data will be also destroyed.
	 */
	void delete(int subscription, boolean deleteRemoteData) throws Exception; // NOSONAR Everything could happen

	/**
	 * Complete the subscription in creation mode. Link data still required.
	 * 
	 * @param subscription
	 *            the subscription identifier is being created.
	 */
	void create(int subscription) throws Exception; // NOSONAR Everything could happen

	/**
	 * Complete the subscription in link mode.
	 * 
	 * @param subscription
	 *            the subscription identifier is being created.
	 */
	void link(int subscription) throws Exception; // NOSONAR Everything could happen

	/**
	 * Return the plug-in key.
	 * 
	 * @return the plug-in key.
	 */
	String getKey();

	/**
	 * Return the plug-in version.
	 * 
	 * @return the plug-in version. Should follow the <a href="http://semver.org/">semantic versioning</a>
	 */
	default String getVersion() {
		return getClass().getPackage().getImplementationVersion();
	}

	/**
	 * Return the plug-in name.
	 * 
	 * @return the plug-in name. May be <code>null</code>.
	 */
	default String getName() {
		return getClass().getPackage().getImplementationTitle();
	}

	/**
	 * Return the plug-in vendor.
	 * 
	 * @return the plug-in vendor. May be <code>null</code>.
	 */
	default String getVendor() {
		return getClass().getPackage().getImplementationVendor();
	}

}
