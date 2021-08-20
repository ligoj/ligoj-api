/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.iam.pub;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ligoj.app.iam.UserOrg;
import org.ligoj.app.iam.empty.EmptyIamProvider;
import org.ligoj.app.iam.empty.EmptyUserRepository;

/**
 * Test class of {@link EmptyUserRepository}
 */
class EmptyUserRepositoryTest {

	@Test
	void findAll() {
		new EmptyUserRepository().setPassword(null, null);
		Assertions.assertTrue(new EmptyUserRepository().findAll().isEmpty());
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
		Assertions.assertTrue(new EmptyUserRepository().authenticate("any", "any"));
	}

	@Test
	void authenticateKoNull() {
		Assertions.assertFalse(new EmptyUserRepository().authenticate(null, "any"));
	}

	@Test
	void authenticateKoEmpty() {
		Assertions.assertFalse(new EmptyUserRepository().authenticate("", "any"));
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
	void getCompanyRepository() {
		Assertions.assertNotNull(new EmptyIamProvider().getConfiguration().getUserRepository().getCompanyRepository());
	}

	@Test
	void getPeopleInternalBaseDn() {
		Assertions.assertEquals("", new EmptyUserRepository().getPeopleInternalBaseDn());
	}

	@Test
	void updateUser() {
		new EmptyUserRepository().updateUser(null);
	}

	@Test
	void move() {
		new EmptyUserRepository().move(null, null);
	}

	@Test
	void restore() {
		new EmptyUserRepository().restore(null);
	}

	@Test
	void unlock() {
		new EmptyUserRepository().unlock(null);
	}

	@Test
	void isolate() {
		new EmptyUserRepository().isolate(null, null);
	}

	@Test
	void lock() {
		new EmptyUserRepository().lock(null, null);
	}

	@Test
	void delete() {
		new EmptyUserRepository().delete(null);
	}

	@Test
	void updateMembership() {
		new EmptyUserRepository().updateMembership(null, null);
	}

	@Test
	void create() {
		final var entry = new UserOrg();
		Assertions.assertSame(entry, new EmptyUserRepository().create(entry));
	}

	@Test
	void toDn() {
		Assertions.assertEquals("", new EmptyUserRepository().toDn(null));
	}

	@Test
	void setPassword() {
		// Does nothing
		new EmptyUserRepository().setPassword(null, null, null);
	}
}
