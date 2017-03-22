package org.ligoj.app.iam;

import org.ligoj.app.api.GroupOrg;

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
}
