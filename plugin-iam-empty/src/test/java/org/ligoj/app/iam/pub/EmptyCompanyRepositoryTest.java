/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.iam.pub;

import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ligoj.app.iam.CompanyOrg;
import org.ligoj.app.iam.empty.EmptyCompanyRepository;

/**
 * Test class of {@link EmptyCompanyRepository}
 */
class EmptyCompanyRepositoryTest {

	@Test
	void findAll() {
		Assertions.assertTrue(new EmptyCompanyRepository().findAll().isEmpty());
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

	@Test
	void getTypeName() {
		Assertions.assertEquals("company", new EmptyCompanyRepository().getTypeName());
	}

	@Test
	void findAllPage() {
		Assertions.assertEquals(0, new EmptyCompanyRepository().findAll(Collections.emptySet(), null, null, Collections.emptyMap()).getTotalElements());
	}

}
