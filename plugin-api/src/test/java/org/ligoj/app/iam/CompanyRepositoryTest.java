/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.iam;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test class of {@link ICompanyRepository}
 */
public class CompanyRepositoryTest {

	@Test
	public void findAll() {
		Assertions.assertTrue(new EmptyCompanyRepository().findAll().isEmpty());
	}

	@Test
	public void delete() {
		new EmptyCompanyRepository().delete(null);
	}

	@Test
	public void create() {
		final CompanyOrg companyLdap = new EmptyCompanyRepository().create("Cn=Some", "Name");
		Assertions.assertEquals("Cn=Some", companyLdap.getDn());
		Assertions.assertEquals("Name", companyLdap.getName());
		Assertions.assertEquals("name", companyLdap.getId());
	}

}
