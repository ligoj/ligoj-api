/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.iam;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * A mocked group repository. Details of a specific group always succeed but the search of groups return an empty list.
 */
@SuppressWarnings("unused")
public class EmptyGroupRepository implements IGroupRepository {

	@Override
	public Map<String, GroupOrg> findAll() {
		return Collections.emptyMap();
	}

	@Override
	public void delete(final GroupOrg container) {
		// Not supported
	}

	@Override
	public GroupOrg create(final String dn, final String cn) {
		// Not supported
		return new GroupOrg(dn, cn, Collections.emptySet());
	}

	@Override
	public GroupOrg findByDepartment(final String department) {
		// Not supported
		return null;
	}

	@Override
	public String getTypeName() {
		return "group";
	}

	@Override
	public Page<GroupOrg> findAll(Set<GroupOrg> groups, String criteria, Pageable pageable,
			Map<String, Comparator<GroupOrg>> customComparators) {
		return null;
	}

	@Override
	public void addAttributes(String newDn, String string, Collection<String> assistants) {
		// Nothing to do
	}

	@Override
	public void addGroup(GroupOrg groupLdap, String normalize) {
		// Nothing to do
	}

	@Override
	public void empty(GroupOrg container, Map<String, UserOrg> findAll) {
		// Nothing to do
	}

	@Override
	public void addUser(UserOrg user, String id) {
		// Nothing to do
	}

	@Override
	public void removeUser(UserOrg user, String id) {
		// Nothing to do
	}

}