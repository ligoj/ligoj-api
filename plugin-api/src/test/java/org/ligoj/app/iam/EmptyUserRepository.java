package org.ligoj.app.iam;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ligoj.app.api.CompanyOrg;
import org.ligoj.app.api.GroupOrg;
import org.ligoj.app.api.UserOrg;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import lombok.Getter;
import lombok.Setter;

/**
 * A mocked user repository. Details of a specific person always succeed but the search of people return an empty list.
 */
public class EmptyUserRepository implements IUserRepository {

	@Getter
	@Setter
	private ICompanyRepository companyRepository;
	
	@Override
	public String getToken(final String login) {
		return login;
	}

	@Override
	public UserOrg findById(final String login) {
		// Always found
		final UserOrg userLdap = new UserOrg();
		userLdap.setId(login);
		userLdap.setFirstName("First");
		userLdap.setLastName("Last");
		userLdap.setMails(Collections.singletonList(login + "@sample.com"));
		userLdap.setDn("CN=" + login);
		userLdap.setCompany("company");
		return userLdap;
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
	public Page<UserOrg> findAll(final Collection<GroupOrg> requiredGroups, final Set<String> companies, final String criteria,
			final Pageable pageable) {
		// No people
		return new PageImpl<>(Collections.emptyList());
	}

	@Override
	public Map<String, UserOrg> findAll() {
		// No people
		return Collections.emptyMap();
	}

	@Override
	public boolean authenticate(final String name, final String password) {
		// Always authenticated
		return true;
	}

	@Override
	public void setPassword(UserOrg userLdap, String password) {
		// Nothing to do
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
