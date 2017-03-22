package org.ligoj.app.resource.security;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ligoj.app.api.GroupOrg;
import org.ligoj.app.api.UserOrg;
import org.ligoj.app.iam.ICompanyRepository;
import org.ligoj.app.iam.IUserRepository;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

class MockUserRepository implements IUserRepository {

	@Override
	public UserOrg findByIdNoCache(String login) {
		return null;
	}

	@Override
	public List<UserOrg> findAllBy(String attribute, String value) {
		return Collections.singletonList(new UserOrg());
	}

	@Override
	public Map<String, UserOrg> findAll() {
		return Collections.singletonMap("some", new UserOrg());
	}

	@Override
	public Page<UserOrg> findAll(Collection<GroupOrg> requiredGroups, Set<String> companies, String criteria, Pageable pageable) {
		return null;
	}

	@Override
	public UserOrg findById(final String login) {
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
	public ICompanyRepository getCompanyRepository() {
		return Mockito.mock(ICompanyRepository.class);
	}

	@Override
	public void setPassword(UserOrg userLdap, String password) {
		// Nothing to do
	}

}
