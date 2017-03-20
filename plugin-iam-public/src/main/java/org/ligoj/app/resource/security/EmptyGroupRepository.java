package org.ligoj.app.resource.security;

import java.util.Collections;
import java.util.Map;

import org.ligoj.app.api.GroupLdap;
import org.ligoj.app.iam.IGroupRepository;

/**
 * A mocked group repository. Details of a specific group always succeed but the search of groups return an empty
 * list.
 */
public class EmptyGroupRepository implements IGroupRepository {

	@Override
	public Map<String, GroupLdap> findAll() {
		return Collections.emptyMap();
	}

	@Override
	public void delete(final GroupLdap container) {
		// Not supported
	}

	@Override
	public GroupLdap create(final String dn, final String cn) {
		// Not supported
		return new GroupLdap(dn, cn, Collections.emptySet());
	}

	@Override
	public GroupLdap findByDepartment(final String department) {
		// Not supported
		return null;
	}

}