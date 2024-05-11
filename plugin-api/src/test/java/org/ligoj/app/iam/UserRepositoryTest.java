/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.iam;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Test class of {@link IUserRepository}
 */
class UserRepositoryTest {

	@Test
	void updateMembership() {
		final var repository = new IUserRepository() {

			IGroupRepository groupRepository;

			@Override
			public IGroupRepository getGroupRepository() {
				return groupRepository == null ? IUserRepository.super.getGroupRepository():groupRepository;
			}

			@Override
			public void updateUser(UserOrg user) {
			}

			@Override
			public void unlock(UserOrg user) {
			}

			@Override
			public String toDn(UserOrg newUser) {
				return null;
			}

			@Override
			public void setPassword(UserOrg user, String password, String newPassword) {
			}

			@Override
			public void setPassword(UserOrg user, String password) {
			}

			@Override
			public void restore(UserOrg user) {
			}

			@Override
			public void move(UserOrg user, CompanyOrg company) {
			}

			@Override
			public void lock(String principal, UserOrg user) {
			}

			@Override
			public void isolate(String principal, UserOrg user) {
			}

			@Override
			public String getToken(String id) {
				return null;
			}

			@Override
			public String getPeopleInternalBaseDn() {
				return null;
			}

			@Override
			public UserOrg findByIdNoCache(String id) {
				return null;
			}

			@Override
			public List<UserOrg> findAllBy(String attribute, String value) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Page<UserOrg> findAll(Collection<GroupOrg> requiredGroups, Set<String> companies, String criteria,
					Pageable pageable) {
				return null;
			}

			@Override
			public Map<String, UserOrg> findAll() {
				return null;
			}

			@Override
			public void delete(UserOrg user) {
			}

			@Override
			public UserOrg create(UserOrg entry) {
				return null;
			}

			@Override
			public UserOrg authenticate(String name, String password) {
				return null;
			}
		};
		Assertions.assertNull(repository.getCompanyRepository());
		Assertions.assertNull(repository.getGroupRepository());
		final var user = new UserOrg();
		repository.groupRepository = Mockito.mock(IGroupRepository.class);
		user.setGroups(List.of("group1", "group2"));
		final var result = repository.updateMembership(List.of("group1", "group3"), user);
		Assertions.assertEquals(List.of("group3"), result.getAddedGroups());
		Assertions.assertEquals(List.of("group2"), result.getRemovedGroups());
	}

	@Test
	void findAll() {
		new EmptyUserRepository().setPassword(null, null);
		new EmptyUserRepository().setPassword(null, null, null);
		Assertions.assertTrue(new EmptyUserRepository().findAll().isEmpty());
	}

	@Test
	void findAllNoCache() {
		Assertions.assertTrue(new EmptyUserRepository().findAllNoCache(null).isEmpty());
	}

	@Test
	void findAll2() {
		Assertions.assertTrue(new EmptyUserRepository().findAll(null, null, null, null).getContent().isEmpty());
	}

	@Test
	void findAllBy() {
		Assertions.assertTrue(new EmptyUserRepository().findAllBy("any", "any").isEmpty());
	}

	@Test
	void authenticate() {
		Assertions.assertNotNull(new EmptyUserRepository().authenticate("any", "any"));
	}

	@Test
	void findByIdNoCache() {
		Assertions.assertEquals("some", new EmptyUserRepository().findByIdNoCache("some").getId());
	}

	@Test
	void getToken() {
		Assertions.assertEquals("some", new EmptyUserRepository().getToken("some"));
	}

	@Test
	void toUser() {
		final var configuration = new EmptyIamProvider().getConfiguration();
		Assertions.assertNotNull(configuration);
		Assertions.assertNotNull(configuration.getUserRepository());
		Assertions.assertEquals("login", configuration.getUserRepository().toUser("login").getId());
	}

	@Test
	void toUserNull() {
		Assertions.assertNull(new EmptyIamProvider().getConfiguration().getUserRepository().toUser(null));
	}

	@Test
	void toUserNotExists() {
		Assertions.assertEquals("login", new MockUserRepository().toUser("login").getId());
	}

	@Test
	void checkLockStatus() {
		// Nothing by default
		new MockUserRepository().checkLockStatus(null);
	}

	@Test
	void findByIdExpectedNotFound() {
		final var repository = new MockUserRepository();
		Assertions.assertThrows(ValidationJsonException.class, () -> repository.findByIdExpected("user1", "user2"));
	}

	@Test
	void findByIdExpectedCompanyNotExists() {
		final var emptyUserRepository = new EmptyUserRepository();
		emptyUserRepository.setCompanyRepository(Mockito.mock(ICompanyRepository.class));
		Assertions.assertThrows(ValidationJsonException.class,
				() -> emptyUserRepository.findByIdExpected("user1", "user2"));
	}

	@Test
	void findByIdExpected() {
		final var emptyUserRepository = new EmptyUserRepository();
		final var companyRepository = Mockito.mock(ICompanyRepository.class);
		Mockito.when(companyRepository.findById("user1", "company")).thenReturn(new CompanyOrg("", ""));
		emptyUserRepository.setCompanyRepository(companyRepository);
		Assertions.assertEquals("user2", emptyUserRepository.findByIdExpected("user1", "user2").getId());
	}

	@Test
	void getCustomAttributes() {
		Assertions.assertEquals(0, new EmptyUserRepository().getCustomAttributes().length);
	}
}
