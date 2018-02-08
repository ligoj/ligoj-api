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
	 * Tree delegate. Corresponds to any object type. Useful for non homogeneous organization trees.
	 */
	TREE
}
