/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.api;

import lombok.Getter;

/**
 * An exception from plug-in management.
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
	 * @param plugin  The requested plug-in of node identifier.
	 * @param message The related message for this plug-in.
	 */
	public PluginException(final String plugin, String message) {
		this(plugin, message, null);
	}

	/**
	 * All arguments constructor.
	 *
	 * @param plugin  The requested plug-in of node identifier.
	 * @param message The related message for this plug-in.
	 * @param cause   the cause (which is saved for later retrieval by the {@link #getCause()} method). (A
	 *                <code>null</code> value is permitted, and indicates that the cause is nonexistent or unknown.)
	 */
	public PluginException(final String plugin, String message, final Throwable cause) {
		super("Plugin issue for " + plugin + ":" + message, cause);
		this.plugin = plugin;
	}
}
