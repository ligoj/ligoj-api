package org.ligoj.app.resource.security;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ligoj.app.api.GroupLdap;
import org.ligoj.app.api.UserLdap;
import org.ligoj.app.iam.IUserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

class MockUserRepository implements IUserRepository {

	@Override
	public UserLdap findByIdNoCache(String login) {
		return null;
	}

	@Override
	public List<UserLdap> findAllBy(String attribute, String value) {
		return Collections.singletonList(new UserLdap());
	}

	@Override
	public Map<String, UserLdap> findAll() {
		return Collections.singletonMap("some", new UserLdap());
	}

	@Override
	public Page<UserLdap> findAll(Collection<GroupLdap> requiredGroups, Set<String> companies, String criteria, Pageable pageable) {
		return null;
	}

	@Override
	public UserLdap findById(final String login) {
		return null;
	}

	@Override
	public boolean authenticate(String name, String password) {
		return false;
	}

	@Override
	public String getToken(String login) {
		return null;
	}

	@Override
	public void setPassword(UserLdap userLdap, String password) {
		// Nothing to do
	}

}
