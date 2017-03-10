package org.ligoj.app.model.ldap;

import java.util.Collection;

import lombok.Getter;
import lombok.Setter;

/**
 * LDAP User
 */
@Getter
@Setter
public class UserLdap extends SimpleUserLdap implements LdapElement {

	private static final long serialVersionUID = 1L;

	/**
	 * User DN
	 */
	private String dn;

	/**
	 * Membership, normalized groups identifiers.
	 */
	private Collection<String> groups;

}
