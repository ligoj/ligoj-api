/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.api;

import org.ligoj.app.model.Node;

/**
 * Flag a class related to a {@link Node}.
 */
public interface NodeScoped {

	/**
	 * Return the related node.
	 * 
	 * @return The related node.
	 */
	Node getNode();

	/**
	 * Return the identifier of this relation source.
	 * 
	 * @return The identifier of this relation source.
	 */
	Integer getId();
}
