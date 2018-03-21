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
	 * Database JDBC URL key
	 */
	public static final String PARAMETER_JDBC_URL = KEY + ":jdbc-url";

	/**
	 * Database JDBC Driver
	 */
	public static final String PARAMETER_JDBC_DRIVER = KEY + ":jdbc-driver";

	/**
	 * Database user name
	 */
	public static final String PARAMETER_JDBC_USER = KEY + ":jdbc-user";

	/**
	 * Database password
	 */
	public static final String PARAMETER_JDBC_PASSSWORD = KEY + ":jdbc-password";

	/**
	 * Web site URL
	 */
	public static final String PARAMETER_URL = KEY + ":url";

	/**
	 * JIRA internal identifier.
	 */
	public static final String PARAMETER_PROJECT = KEY + ":project";

	/**
	 * JIRA external string identifier, AKA pkey.
	 */
	public static final String PARAMETER_PKEY = KEY + ":pkey";

	/**
	 * JIRA user name able to perform index.
	 */
	public static final String PARAMETER_ADMIN_USER = KEY + ":user";

	/**
	 * JIRA user password able to perform index.
	 */
	public static final String PARAMETER_ADMIN_PASSWORD = KEY + ":password";

	/**
	 * Parameter corresponding to the associated version. Not yet saved in database, only in-memory to save request
	 * during status checks.
	 */
	protected static final String PARAMETER_CACHE_VERSION = KEY + ":version";

	@Override
	public String getKey() {
		return KEY;
	}

}
