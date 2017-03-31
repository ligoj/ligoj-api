package org.ligoj.app.api;

import lombok.Getter;

/**
 * An exception of plug-in management.
 */
@Getter
public class PluginException extends RuntimeException {

	/**
	 * SID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The related plug-in identifier.
	 */
	private final String plugin;

	/**
	 * All arguments constructor.
	 * 
	 * @param plugin
	 *            The requested plug-in of node identifier.
	 */
	public PluginException(final String plugin, String message) {
		super("Plugin issue for " + plugin + ":" + message);
		this.plugin = plugin;
	}
}
