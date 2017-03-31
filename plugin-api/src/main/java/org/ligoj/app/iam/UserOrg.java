package org.ligoj.app.iam;

import java.util.Collection;

import lombok.Getter;
import lombok.Setter;

/**
 * Organization user with groups
 */
@Getter
@Setter
public class UserOrg extends SimpleUserOrg implements ResourceOrg {

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
