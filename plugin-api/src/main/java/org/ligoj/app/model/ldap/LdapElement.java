package org.ligoj.app.model.ldap;

/**
 * Any LDAP element with a DN and an unique identifier.
 */
public interface LdapElement {

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
