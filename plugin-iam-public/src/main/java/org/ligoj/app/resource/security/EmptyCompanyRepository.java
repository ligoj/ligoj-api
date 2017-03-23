package org.ligoj.app.resource.security;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

import org.ligoj.app.api.CompanyOrg;
import org.ligoj.app.iam.ICompanyRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * A mocked company repository. Details of a specific company always succeed but the search of companies return an empty
 * list.
 */
public class EmptyCompanyRepository implements ICompanyRepository {

	@Override
	public Map<String, CompanyOrg> findAll() {
		return Collections.emptyMap();
	}

	@Override
	public void delete(final CompanyOrg container) {
		// Not supported
	}

	@Override
	public CompanyOrg create(final String dn, final String name) {
		return new CompanyOrg(dn, name);
	}

	@Override
	public String getTypeName() {
		return "company";
	}

	@Override
	public Page<CompanyOrg> findAll(Set<CompanyOrg> groups, String criteria, Pageable pageable,
			Map<String, Comparator<CompanyOrg>> customComparators) {
		// TODO Auto-generated method stub
		return null;
	}

}
