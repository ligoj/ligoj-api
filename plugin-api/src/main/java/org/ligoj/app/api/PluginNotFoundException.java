package org.ligoj.app.api;

import lombok.Getter;

/**
 * An exception of plug-in management.
 */
@Getter
public class PluginNotFoundException extends RuntimeException {

	/**
	 * SID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The requested plug-in of node identifier.
	 */
	private final String plugin;

	/**
	 * All arguments constructor.
	 * 
	 * @param plugin
	 *            The requested plug-in of node identifier.
	 */
	public PluginNotFoundException(final String plugin) {
		super("No plugin found for " + plugin);
		this.plugin = plugin;
	}
}
