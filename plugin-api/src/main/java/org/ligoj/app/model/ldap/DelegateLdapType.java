package org.ligoj.app.model.ldap;

/**
 * Delegate type.
 */
public enum DelegateLdapType {

	/**
	 * Membership delegate. Corresponds to "groupOfUniqueNames" LDAP type.
	 */
	GROUP,

	/**
	 * Company members. Corresponds to "organizationalUnit" LDAP type.
	 */
	COMPANY,

	/**
	 * LDAP tree delegate. Corresponds to any LDAP type.
	 */
	TREE
}
