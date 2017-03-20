package org.ligoj.app.resource.security;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import org.ligoj.app.iam.IUserRepository;
import org.ligoj.app.model.GroupLdap;
import org.ligoj.app.model.UserLdap;

/**
 * A mocked user repository. Details of a specific person always succeed but the search of people return an empty list.
 */
public class EmptyUserRepository implements IUserRepository {

	@Override
	public String getToken(final String login) {
		return login;
	}

	@Override
	public UserLdap findById(final String login) {
		// Always found
		final UserLdap userLdap = new UserLdap();
		userLdap.setId(login);
		userLdap.setFirstName("First");
		userLdap.setLastName("Last");
		userLdap.setMails(Collections.singletonList(login + "@sample.com"));
		userLdap.setDn("CN=" + login);
		return userLdap;
	}

	@Override
	public UserLdap findByIdNoCache(final String login) {
		return findById(login);
	}

	@Override
	public List<UserLdap> findAllBy(final String attribute, final String value) {
		// No people
		return Collections.emptyList();
	}

	@Override
	public Page<UserLdap> findAll(final Collection<GroupLdap> requiredGroups, final Set<String> companies, final String criteria,
			final Pageable pageable) {
		// No people
		return new PageImpl<>(Collections.emptyList());
	}

	@Override
	public Map<String, UserLdap> findAll() {
		// No people
		return Collections.emptyMap();
	}

	@Override
	public boolean authenticate(final String name, final String password) {
		// Always authenticated
		return true;
	}

	@Override
	public void setPassword(UserLdap userLdap, String password) {
		// Nothing to do
	}
}
