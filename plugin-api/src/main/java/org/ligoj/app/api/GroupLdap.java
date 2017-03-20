package org.ligoj.app.api;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

/**
 * LDAP Group.<br>
 * "id" corresponds to the normalized "Distinguished Name".<br>
 * "name" corresponds to the real "Common Name", not normalized.
 */
@Getter
@Setter
public class GroupLdap extends ContainerLdap {

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
	 * Parent parent groups this group is member of. Identifier (normalized CN) is used.
	 */
	@JsonIgnore
	private Set<String> groups;

	/**
	 * All arguments constructor.
	 * 
	 * @param dn
	 *            "id" corresponds to the "Distinguished Name".
	 * @param name
	 *            "name" corresponds to the "Common Name". Will be saved in "name", and in "id" in is normalized form.
	 * @param members
	 *            unique members. UID is used.
	 */
	public GroupLdap(final String dn, final String name, final Set<String> members) {
		super(dn, name);
		setMembers(members);
		setGroups(new HashSet<>());
		setSubGroups(new HashSet<>());
	}

	@Override
	public boolean equals(final Object other) {
		return other instanceof GroupLdap && getId().equals(((GroupLdap) other).getId());
	}

	@Override
	public int hashCode() {
		return getId().hashCode();
	}

}
