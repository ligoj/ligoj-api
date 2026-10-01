/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.iam.empty;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

import org.ligoj.app.iam.CompanyOrg;
import org.ligoj.app.iam.ICompanyRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/**
 * A mocked company repository holding no company: the search of companies returns an empty list and a lookup by
 * identifier returns <code>null</code>. Modifications are not supported.
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
	public Page<CompanyOrg> findAll(final Set<CompanyOrg> groups, final String criteria, final Pageable pageable,
			final Map<String, Comparator<CompanyOrg>> customComparators) {
		return new PageImpl<>(Collections.emptyList());
	}

}
