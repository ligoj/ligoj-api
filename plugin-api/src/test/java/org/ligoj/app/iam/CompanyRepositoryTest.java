package org.ligoj.app.iam;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test class of {@link ICompanyRepository}
 */
public class CompanyRepositoryTest {

	@Test
	public void findAll() {
		Assert.assertTrue(new EmptyCompanyRepository().findAll().isEmpty());
	}

	@Test
	public void delete() {
		new EmptyCompanyRepository().delete(null);
	}

	@Test
	public void create() {
		final CompanyOrg companyLdap = new EmptyCompanyRepository().create("Cn=Some", "Name");
		Assert.assertEquals("Cn=Some", companyLdap.getDn());
		Assert.assertEquals("Name", companyLdap.getName());
		Assert.assertEquals("name", companyLdap.getId());
	}

}
