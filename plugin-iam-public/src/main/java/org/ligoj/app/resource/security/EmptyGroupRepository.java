package org.ligoj.app.resource.security;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

import org.ligoj.app.api.GroupOrg;
import org.ligoj.app.api.UserOrg;
import org.ligoj.app.iam.IGroupRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/**
 * A mocked group repository. Details of a specific group always succeed but the search of groups return an empty
 * list.
 */
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
	public Page<GroupOrg> findAll(Set<GroupOrg> groups, String criteria, Pageable pageable, Map<String, Comparator<GroupOrg>> customComparators) {
		return new PageImpl<>(Collections.emptyList());
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
	public void addUser(UserOrg userLdap, String id) {
		// Nothing to do
	}

	@Override
	public void removeUser(UserOrg userLdap, String id) {
		// Nothing to do
	}

}