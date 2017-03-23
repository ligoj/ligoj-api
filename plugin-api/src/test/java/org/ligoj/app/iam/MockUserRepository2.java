package org.ligoj.app.iam;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ligoj.app.api.CompanyOrg;
import org.ligoj.app.api.GroupOrg;
import org.ligoj.app.api.UserOrg;
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
	public Page<UserOrg> findAll(Collection<GroupOrg> requiredGroups, Set<String> companies, String criteria, Pageable pageable) {
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
	public void setPassword(UserOrg userLdap, String password) {
		// Nothing to do
	}

	@Override
	public ICompanyRepository getCompanyRepository() {
		return Mockito.mock(ICompanyRepository.class);
	}

	@Override
	public String getPeopleInternalBaseDn() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateUser(UserOrg user) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void move(UserOrg user, CompanyOrg company) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void restore(UserOrg user) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void unlock(UserOrg user) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void isolate(String principal, UserOrg user) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void lock(String principal, UserOrg user) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void delete(UserOrg user) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateMembership(Collection<String> groups, UserOrg user) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public UserOrg create(UserOrg entry) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toDn(UserOrg newUser) {
		// TODO Auto-generated method stub
		return null;
	}

}
