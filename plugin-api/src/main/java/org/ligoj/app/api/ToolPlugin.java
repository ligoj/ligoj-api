/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.api;

import java.util.Map;

/**
 * Contract of all tools.
 */
public interface ToolPlugin extends ServicePlugin {

	/**
	 * Return the detected version of the tool given some parameters.
	 *
	 * @param parameters The associated parameter values
	 * @return the String value of the detected version of the tool or <code>null</code> if not available/found.
	 * @throws Exception Version cannot be retrieved. This error is caught at higher level.
	 */
	default String getVersion(Map<String, String> parameters) throws Exception { // NOSONAR
		// Not implemented, offline, or private tool
		return null;
	}

	/**
	 * Return the last available version of this tool.
	 *
	 * @return The String value of the last available version of the tool or <code>null</code> if not found.
	 * @throws Exception When the version failed to be read, will also be considered as a <code>null</code> version.
	 */
	default String getLastVersion() throws Exception { // NOSONAR
		// Not implemented, offline, or private tool
		return null;
	}

	/**
	 * Check the status of node having the given configuration. The related node exists but is anonymous.
	 *
	 * @param parameters The current parameter values of the node.
	 * @return <code>true</code> when the status is UP. By default, return <code>true</code> when not implemented.
	 * @throws Exception Status cannot be retrieved. This error is caught at higher level.
	 */
	default boolean checkStatus(final Map<String, String> parameters) throws Exception { // NOSONAR
		return true;
	}

	/**
	 * Check the status of given node.
	 *
	 * @param node       The node identifier. May be <code>null</code> for anonymous case.
	 * @param parameters The actual parameter values of the node.
	 * @return <code>true</code> when the status is UP.
	 * @see #checkStatus(Map)
	 * @throws Exception Status cannot be retrieved. This error is caught at higher level.
	 */
	default boolean checkStatus(String node, Map<String, String> parameters) throws Exception { // NOSONAR
		return checkStatus(parameters);
	}

	/**
	 * Check the status of given subscription configuration. In these case, the subscription's node is anonymous.
	 *
	 * @param parameters The parameter values of the subscription.
	 * @return <code>true</code> when the status is UP. By default, return <code>true</code> when not implemented.
	 * @throws Exception Status cannot be retrieved. This error is caught at higher level.
	 */
	default SubscriptionStatusWithData checkSubscriptionStatus(final Map<String, String> parameters) throws Exception { // NOSONAR
		return new SubscriptionStatusWithData();
	}

	/**
	 * Check the status of given configuration. Note the subscription may not exist yet, but all required parameters are
	 * given.
	 *
	 * @param node       The related node identifier.
	 * @param parameters The parameter values of the subscription.
	 * @return <code>true</code> when the status is UP.
	 * @see #checkSubscriptionStatus(Map)
	 * @throws Exception Status cannot be retrieved. This error is caught at higher level.
	 */
	default SubscriptionStatusWithData checkSubscriptionStatus(String node, Map<String, String> parameters)
			throws Exception { // NOSONAR
		return checkSubscriptionStatus(parameters);
	}

	/**
	 * Check the status of given subscription. Note this subscription is existing and persisted in database, and not
	 * being created.
	 *
	 * @param subscription Current subscription.
	 * @param node         The related node identifier.
	 * @param parameters   The parameter values of the subscription.
	 * @return <code>true</code> when the status is UP.
	 * @see #checkSubscriptionStatus(String, Map)
	 * @throws Exception Status cannot be retrieved. This error is caught at higher level.
	 */
	default SubscriptionStatusWithData checkSubscriptionStatus(int subscription, String node,
			Map<String, String> parameters) throws Exception { // NOSONAR
		return checkSubscriptionStatus(node, parameters);
	}
}
