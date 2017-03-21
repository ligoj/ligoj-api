package org.ligoj.app.model;

/**
 * Delegate type.
 */
public enum DelegateType {

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
