package org.ligoj.app.resource.node.sample;

import org.springframework.stereotype.Component;

/**
 * GIT resource. jGit is used to perform validations.
 */
@Component
public class GitPluginResource extends AbstractToolPluginResource {

	/**
	 * Plug-in key.
	 */
	public static final String URL = ScmResource.SERVICE_URL + "/git";

	/**
	 * Plug-in key.
	 */
	public static final String KEY = URL.replace('/', ':').substring(1);

	@Override
	public String getKey() {
		return KEY;
	}
}
