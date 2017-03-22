package org.ligoj.app.api;

/**
 * An organizational resource : a DN and an unique identifier. This element is the base for all security management with
 * delegates.
 */
public interface ResourceOrg {

	/**
	 * The distinguished name (DN) of this element.
	 * 
	 * @return the distinguished name (DN) of this element.
	 */
	String getDn();

	/**
	 * The unique identifier also known as RDN.
	 * 
	 * @return the unique identifier also known as RDN.
	 */
	String getId();
}
