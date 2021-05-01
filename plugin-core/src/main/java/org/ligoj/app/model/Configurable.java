/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.model;

import java.io.Serializable;

import org.springframework.data.domain.Persistable;

/**
 * The plug-in configuration.
 *
 * @param <T> The common configuration type.
 * @param <K> The identifier type.
 */
public interface Configurable<T extends PluginConfiguration, K extends Serializable> extends Persistable<K> {

	/**
	 * The related configuration.
	 *
	 * @return The related configuration.
	 */
	T getConfiguration();
}
