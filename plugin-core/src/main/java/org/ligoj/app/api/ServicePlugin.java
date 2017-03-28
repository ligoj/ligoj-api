package org.ligoj.app.api;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.ligoj.app.model.Node;

/**
 * A plug-in. The plug-in behavior is massively based on naming convention. The key of the plug-in determines the
 * required parent and the namespace for web assets. For sample the key "service:s:t" means :
 * <ul>
 * <li>"service" is a constant part, and required. And will be published inside the "/service" REST namespace.</li>
 * <li>"s" is the first level of service. Lower case part, following the pattern [a-z\\d]+</li>
 * <li>"t" is the first level of service. Lower case part, following the pattern [a-z\\d]+</li>
 * </ul>
 */
public interface ServicePlugin extends Comparable<ServicePlugin> {

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
	 * Return the plug-in name. By default the name is computed from the <code>MANIFEST.MF#Implementation-Title</code>
	 * attribute. When <code>null</code>, the capitalized part of plug-in's key is used.
	 * 
	 * @return the plug-in name. Never <code>null</code>.
	 */
	default String getName() {
		return StringUtils.defaultIfBlank(getClass().getPackage().getImplementationTitle(),
				StringUtils.capitalize(getKey().substring(getKey().lastIndexOf(':') + 1).replace(':', ' ')));
	}

	/**
	 * Return the plug-in vendor.
	 * 
	 * @return the plug-in vendor. May be <code>null</code>.
	 */
	default String getVendor() {
		return getClass().getPackage().getImplementationVendor();
	}

	/**
	 * Callback used to persist some additional when the plug-in is being installed.
	 * 
	 * @param node
	 *            The current state of the node being persisted during the installation of this plug-in.
	 */
	default void install(Node node) {
		// Nothing to do
	}

	/**
	 * Callback used to persist some additional when the plug-in is being installed.
	 * 
	 * @param node
	 *            The the node being removed along the un-installation of this plug-in.
	 */
	default void uninstall(final Node node) {
		// Nothing to do
	}

	@Override
	default int compareTo(final ServicePlugin o) {
		// Compare the plug-in by their key
		return getKey().compareTo(o.getKey());
	}

	/**
	 * Return entities class to be persisted during the installation from CSV files located in the "csv" folder of this
	 * plug-in. CsvForJpa component will be used. Order is important. First {@link Class} will be associated to the
	 * right CSV file and persisted in the database, then the next one. When empty, or not containing the "Node.class"
	 * value, a default Node will be inserted by default.
	 * 
	 * @return Entities class to be persisted during the installation from CSV files
	 */
	default List<Class<?>> getInstalledEntities() {
		return Collections.emptyList();
	}
}
