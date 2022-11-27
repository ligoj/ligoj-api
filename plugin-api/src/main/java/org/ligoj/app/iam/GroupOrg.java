/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.iam;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

/**
 * Organizational Group.<br>
 * "id" corresponds to the normalized "Distinguished Name".<br>
 * "name" corresponds to the real "Common Name", not normalized.
 */
@Getter
@Setter
public class GroupOrg extends ContainerOrg {

	/**
	 * SID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Unique user members. Identifier (UID) is used.
	 */
	@JsonIgnore
	private Set<String> members;

	/**
	 * Unique group members. Identifier (normalized CN) is used.
	 */
	@JsonIgnore
	private Set<String> subGroups;

	/**
	 * Parent groups this group is member of. Identifier (normalized CN) is used.
	 */
	@JsonIgnore
	private Set<String> groups;

	/**
	 * All arguments constructor.
	 *
	 * @param dn      "id" corresponds to the "Distinguished Name".
	 * @param name    "name" corresponds to the "Common Name". Will be saved in "name", and in "id" in is normalized
	 *                form.
	 * @param members unique members. UID is used.
	 */
	public GroupOrg(final String dn, final String name, final Set<String> members) {
		super(dn, name);
		setMembers(members);
		setGroups(new HashSet<>());
		setSubGroups(new HashSet<>());
	}

	@Override
	public boolean equals(final Object other) {
		return other instanceof GroupOrg org && getId().equals(org.getId());
	}

	@Override
	public int hashCode() {
		return getId().hashCode();
	}

}
