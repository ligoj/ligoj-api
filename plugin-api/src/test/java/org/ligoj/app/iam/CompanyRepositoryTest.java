/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.iam;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test class of {@link ICompanyRepository}
 */
class CompanyRepositoryTest {

	@Test
	void findAll() {
		Assertions.assertTrue(new EmptyCompanyRepository().findAll().isEmpty());
	}

	@Test
	void findAllNoCache() {
		Assertions.assertTrue(new EmptyCompanyRepository().findAllNoCache().isEmpty());
	}

	@Test
	void delete() {
		final var repository = new EmptyCompanyRepository();
		final var companyLdap = repository.create("Cn=Some", "Name");
		repository.delete(companyLdap);
		// Deletion was ignored
		Assertions.assertTrue(repository.findAll().isEmpty());
	}

	@Test
	void create() {
		final var repository = new EmptyCompanyRepository();
		final var companyLdap = repository.create("Cn=Some", "Name");
		Assertions.assertEquals("Cn=Some", companyLdap.getDn());
		Assertions.assertEquals("Name", companyLdap.getName());
		Assertions.assertEquals("name", companyLdap.getId());

		// Create was ignored
		Assertions.assertTrue(repository.findAll().isEmpty());
	}

}
