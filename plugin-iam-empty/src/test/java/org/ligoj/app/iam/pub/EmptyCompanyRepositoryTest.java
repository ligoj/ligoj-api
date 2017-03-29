package org.ligoj.app.iam.pub;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;
import org.ligoj.app.iam.CompanyOrg;
import org.ligoj.app.iam.pub.EmptyCompanyRepository;

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
		final CompanyOrg companyLdap = new EmptyCompanyRepository().create("Cn=Some", "Name");
		Assert.assertEquals("Cn=Some", companyLdap.getDn());
		Assert.assertEquals("Name", companyLdap.getName());
		Assert.assertEquals("name", companyLdap.getId());
	}

	@Test
	public void getTypeName() {
		Assert.assertEquals("company", new EmptyCompanyRepository().getTypeName());
	}

	@Test
	public void findAllPage() {
		Assert.assertEquals(0, new EmptyCompanyRepository().findAll(Collections.emptySet(), null, null, Collections.emptyMap()).getTotalElements());
	}

}
