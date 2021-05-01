/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.iam.pub;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ligoj.app.iam.CompanyOrg;
import org.ligoj.app.iam.GroupOrg;
import org.ligoj.app.iam.ICompanyRepository;
import org.ligoj.app.iam.IUserRepository;
import org.ligoj.app.iam.UserOrg;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

class MockUserRepository2 implements IUserRepository {

	@Override
	public UserOrg findByIdNoCache(String login) {
		return null;
	}

	@Override
	public List<UserOrg> findAllBy(String attribute, String value) {
		return Collections.emptyList();
	}

	@Override
	public Map<String, UserOrg> findAll() {
		return Collections.emptyMap();
	}

	@Override
	public Page<UserOrg> findAll(Collection<GroupOrg> requiredGroups, Set<String> companies, String criteria,
			Pageable pageable) {
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
	public void setPassword(UserOrg user, String password) {
		// Nothing to do
	}

	@Override
	public void setPassword(UserOrg user, String password, String newPassword) {
		// Nothing to do

	}

	@Override
	public ICompanyRepository getCompanyRepository() {
		return Mockito.mock(ICompanyRepository.class);
	}

	@Override
	public String getPeopleInternalBaseDn() {
		// Nothing to do
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
	public void updateMembership(Collection<String> groups, UserOrg user) {
		// Nothing to do
	}

	@Override
	public UserOrg create(UserOrg entry) {
		// Nothing to do
		return null;
	}

	@Override
	public String toDn(UserOrg newUser) {
		// Nothing to do
		return null;
	}
}
