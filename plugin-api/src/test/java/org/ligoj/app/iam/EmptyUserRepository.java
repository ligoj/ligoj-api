/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.iam;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.*;

/**
 * A mocked user repository. Details of a specific person always succeed but the search of people return an empty list.
 */
@SuppressWarnings("unused")
@Getter
@Setter
public class EmptyUserRepository implements IUserRepository {

	private ICompanyRepository companyRepository;

	@Override
	public String getToken(final String login) {
		return login;
	}

	@Override
	public UserOrg findById(final String login) {
		// Always found
		final var user = new UserOrg();
		user.setId(login);
		user.setFirstName("First");
		user.setLastName("Last");
		user.setMails(Collections.singletonList(login + "@sample.com"));
		user.setDn("CN=" + login);
		user.setCompany("company");
		return user;
	}

	@Override
	public UserOrg findByIdNoCache(final String login) {
		return findById(login);
	}

	@Override
	public List<UserOrg> findAllBy(final String attribute, final String value) {
		// No people
		return Collections.emptyList();
	}

	@Override
	public Page<UserOrg> findAll(final Collection<GroupOrg> requiredGroups, final Set<String> companies,
			final String criteria, final Pageable pageable) {
		// No people
		return new PageImpl<>(Collections.emptyList());
	}

	@Override
	public Map<String, UserOrg> findAll() {
		// No people
		return Collections.emptyMap();
	}

	@Override
	public UserOrg authenticate(final String name, final String password) {
		// Always authenticated
		final var user = new UserOrg();
		user.setId(name);
		return user;
	}

	@Override
	public void setPassword(UserOrg user, String password) {
		// Nothing to do
	}

	@Override
	public void setPassword(UserOrg user, String password, String newPassword) {
		// Nothing to do

	}

	@Override
	public String getPeopleInternalBaseDn() {
		return null;
	}

	@Override
	public void updateUser(UserOrg user) {
		// Nothing to do
	}

	@Override
	public void move(UserOrg user, CompanyOrg company) {
		// Nothing to do
	}

	@Override
	public void restore(UserOrg user) {
		// Nothing to do
	}

	@Override
	public void unlock(UserOrg user) {
		// Nothing to do
	}

	@Override
	public void isolate(String principal, UserOrg user) {
		// Nothing to do
	}

	@Override
	public void lock(String principal, UserOrg user) {
		// Nothing to do
	}

	@Override
	public void delete(UserOrg user) {
		// Nothing to do
	}

	@Override
	public UserUpdateResult updateMembership(Collection<String> groups, UserOrg user) {
		return new UserUpdateResult();
	}

	@Override
	public UserOrg create(UserOrg entry) {
		return null;
	}

	@Override
	public String toDn(UserOrg newUser) {
		return null;
	}

}
