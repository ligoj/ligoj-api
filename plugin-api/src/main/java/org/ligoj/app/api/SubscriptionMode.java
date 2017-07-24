package org.ligoj.app.api;

/**
 * Subscription mode. Used to define the way the parameters must be read and validated.
 */
public enum SubscriptionMode {

	/**
	 * The parameter is used to maintain the relation between the project and the tool. It will be requested to
	 * establish the relation, so must be generated in creation mode. At the end, this parameter has to provided.
	 */
	LINK,

	/**
	 * The parameter is used to create a new subscription in the target tool, and to generate the parameter of type
	 * {@link #LINK}. Parameter values of this mode still stored in database but not used to retrieve information from
	 * the tool after the link has been established.
	 */
	CREATE,

	/**
	 * The parameter can be used for any mode.
	 */
	ALL,

	/**
	 * This parameter cannot be used.
	 */
	NONE

}
