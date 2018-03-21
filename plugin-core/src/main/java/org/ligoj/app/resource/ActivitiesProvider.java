/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource;

import java.util.Collection;
import java.util.Map;

import org.ligoj.app.iam.Activity;

/**
 * Collect activities of users.
 */
@FunctionalInterface
public interface ActivitiesProvider {

	/**
	 * Return activities of given users
	 * 
	 * @param subscription
	 *            the subscription's identifier.
	 * @param users
	 *            User to collect activities.
	 * @return activities. User without activity are not in this result.
	 */
	Map<String, Activity> getActivities(int subscription, Collection<String> users) throws Exception; // NOSONAR
}
