package org.ligoj.app.resource.security;

import java.util.Collections;
import java.util.Map;

import org.ligoj.app.iam.ICompanyRepository;
import org.ligoj.app.model.ldap.CompanyLdap;

/**
 * A mocked company repository. Details of a specific company always succeed but the search of companies return an empty
 * list.
 */
public class EmptyCompanyRepository implements ICompanyRepository {

	@Override
	public Map<String, CompanyLdap> findAll() {
		return Collections.emptyMap();
	}

	@Override
	public void delete(final CompanyLdap container) {
		// Not supported
	}

	@Override
	public CompanyLdap create(final String dn, final String name) {
		return new CompanyLdap(dn, name);
	}

}
