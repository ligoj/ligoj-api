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
		new EmptyCompanyRepository().delete(null);
	}

	@Test
	void create() {
		final CompanyOrg companyLdap = new EmptyCompanyRepository().create("Cn=Some", "Name");
		Assertions.assertEquals("Cn=Some", companyLdap.getDn());
		Assertions.assertEquals("Name", companyLdap.getName());
		Assertions.assertEquals("name", companyLdap.getId());
	}

}
