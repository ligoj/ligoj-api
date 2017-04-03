package org.ligoj.app.api;

import java.util.Map;

/**
 * Contract of all tools.
 */
public interface ToolPlugin extends ServicePlugin {

	/**
	 * Return the detected version of the tool given some parameters.
	 * 
	 * @param parameters
	 *            the associated parameter values
	 * @return the String value of the detected version of the tool or <code>null</code> if not available/found.
	 */
	default String getVersion(Map<String, String> parameters) throws Exception { // NOSONAR
		// Not implemented, offline, or private tool
		return null;
	}

	/**
	 * Return the last available version of this tool.
	 * 
	 * @return the String value of the last available version of the tool or <code>null</code> if not found.
	 * @throws Exception
	 *             When the version failed to be read, will also be considered as a <code>null</code> version.
	 */
	default String getLastVersion() throws Exception { // NOSONAR
		// Not implemented, offline, or private tool
		return null;
	}

	/**
	 * Check the status of given node.
	 * 
	 * @param node
	 *            The node identifier. May be <code>null</code>.
	 * @param parameters
	 *            the parameter values of the node.
	 * @return <code>true</code> when the status is UP.
	 */
	boolean checkStatus(String node, Map<String, String> parameters) throws Exception; // NOSONAR

	/**
	 * Check the status of node having the given configuration. In these case, the node is anonymous.
	 * 
	 * @param parameters
	 *            the parameter values of the node.
	 * @return <code>true</code> when the status is UP.
	 */
	default boolean checkStatus(final Map<String, String> parameters) throws Exception {
		return checkStatus(null, parameters);
	}

	/**
	 * Check the status of given subscription configuration. In these case, the subscription's node is anonymous.
	 * 
	 * @param parameters
	 *            the parameter values of the subscription.
	 * @return <code>true</code> when the status is UP.
	 */
	default SubscriptionStatusWithData checkSubscriptionStatus(final Map<String, String> parameters) throws Exception {
		return checkSubscriptionStatus(null, parameters);
	}

	/**
	 * Check the status of given subscription
	 * 
	 * @param node
	 *            The node identifier.
	 * @param parameters
	 *            the parameter values of the subscription.
	 * @return <code>true</code> when the status is UP.
	 */
	SubscriptionStatusWithData checkSubscriptionStatus(String node, Map<String, String> parameters) throws Exception; // NOSONAR
}
