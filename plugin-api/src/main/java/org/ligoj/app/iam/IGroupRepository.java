package org.ligoj.app.iam;

import org.ligoj.app.model.GroupLdap;

/**
 * Group repository
 */
public interface IGroupRepository extends ContainerLdapRepository<GroupLdap> {

	/**
	 * Return the first group having a matching department.
	 * 
	 * @param department
	 *            The {@link GroupLdap} linked to the given department.
	 * @return The {@link GroupLdap} linked to the given department or <code>null</code>.
	 */
	GroupLdap findByDepartment(String department);
}
