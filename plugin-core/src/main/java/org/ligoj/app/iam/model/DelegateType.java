/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.iam.model;

/**
 * Delegate resource type.
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
	 * Tree delegate. Corresponds to any object type. Useful for heterogeneous organization trees.
	 */
	TREE
}
