/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.node.sample;

/**
 * Basic JIRA business features
 */
public class JiraBaseResource extends AbstractToolPluginResource {

	/**
	 * Plug-in key.
	 */
	public static final String URL = BugTrackerResource.SERVICE_URL + "/jira";

	/**
	 * Plug-in key.
	 */
	public static final String KEY = URL.replace('/', ':').substring(1);

	/**
	 * Database password
	 */
	public static final String PARAMETER_JDBC_PASSWORD = KEY + ":jdbc-password";

	/**
	 * JIRA internal identifier.
	 */
	public static final String PARAMETER_PROJECT = KEY + ":project";

	/**
	 * JIRA external string identifier, AKA pkey.
	 */
	public static final String PARAMETER_PKEY = KEY + ":pkey";

	@Override
	public String getKey() {
		return KEY;
	}

}
