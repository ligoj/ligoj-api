/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.node.sample;

import org.springframework.stereotype.Component;

/**
 * LDAP resource.
 */
@Component
public class LdapPluginResource extends AbstractToolPluginResource {

	/**
	 * Plug-in key.
	 */
	public static final String URL = IdentityResource.SERVICE_URL + "/ldap";

	/**
	 * Plug-in key.
	 */
	public static final String KEY = URL.replace('/', ':').substring(1);

	@Override
	public String getKey() {
		return KEY;
	}

	@Override
	public void create(final int subscription) {
		// Nothing to do
	}
}
