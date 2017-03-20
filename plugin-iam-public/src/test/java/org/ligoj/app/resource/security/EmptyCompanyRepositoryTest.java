package org.ligoj.app.resource.security;

import org.junit.Assert;
import org.junit.Test;
import org.ligoj.app.model.CompanyLdap;

/**
 * Test class of {@link EmptyCompanyRepository}
 */
public class EmptyCompanyRepositoryTest {

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
		final CompanyLdap companyLdap = new EmptyCompanyRepository().create("Cn=Some", "Name");
		Assert.assertEquals("Cn=Some", companyLdap.getDn());
		Assert.assertEquals("Name", companyLdap.getName());
		Assert.assertEquals("name", companyLdap.getId());
	}

}
