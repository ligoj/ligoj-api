package org.ligoj.app.iam;

import java.util.Collection;
import java.util.Map;

/**
 * Group repository
 */
public interface IGroupRepository extends IContainerRepository<GroupOrg> {

	/**
	 * Return the first group having a matching department.
	 * 
	 * @param department
	 *            The {@link GroupOrg} linked to the given department.
	 * @return The {@link GroupOrg} linked to the given department or <code>null</code>.
	 */
	GroupOrg findByDepartment(String department);

	/**
	 * Add attributes to the given DN.
	 * 
	 * @param dn
	 *            The target DN.
	 * @param attribute
	 *            The attribute name.
	 * @param values
	 *            The values to add. My be empty.
	 */
	void addAttributes(String dn, String attribute, Collection<String> values);

	/**
	 * Add a group to another group. Cache is updated.
	 * 
	 * @param subGroup
	 *            {@link GroupOrg} to add to a parent group.
	 * @param toGroup
	 *            CN of the parent group to update.
	 */
	void addGroup(GroupOrg subGroup, String toGroup);

	/**
	 * Empty the group. All users from the group will not be anymore associated to this group, and the members of the
	 * group will be emptied. Not that the sub groups are not removed, only users are concerned.
	 * 
	 * @param group
	 *            The group to empty.
	 * @param users
	 *            All known users could be removed from this group.
	 */
	void empty(GroupOrg group, Map<String, UserOrg> users);

	/**
	 * Add a user to given group. Cache is updated.
	 * 
	 * @param user
	 *            {@link UserOrg} to add.
	 * @param group
	 *            CN of the group to update.
	 */
	void addUser(UserOrg user, String group);

	/**
	 * Remove a user from a given group. Cache is updated.
	 * 
	 * @param user
	 *            {@link UserOrg} to remove.
	 * @param group
	 *            CN of the group to update.
	 */
	void removeUser(UserOrg user, String group);
}
