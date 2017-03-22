package org.ligoj.app.resource.security;

import java.util.Collections;
import java.util.Map;

import org.ligoj.app.api.GroupOrg;
import org.ligoj.app.iam.IGroupRepository;

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

}