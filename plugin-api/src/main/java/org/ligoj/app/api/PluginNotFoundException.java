/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.api;

import lombok.Getter;

/**
 * An exception of a not found plug-in.
 */
@Getter
public class PluginNotFoundException extends PluginException {

	/**
	 * SID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * All arguments constructor.
	 *
	 * @param plugin The requested plug-in of node identifier.
	 */
	public PluginNotFoundException(final String plugin) {
		super(plugin, "Not found");
	}
}
