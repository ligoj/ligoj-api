package org.ligoj.app.iam;

import java.util.ArrayList;
import java.util.List;

import javax.naming.ldap.LdapName;

import lombok.Getter;
import lombok.Setter;

/**
 * An organizational company.
 */
@Getter
@Setter
public class CompanyOrg extends ContainerOrg {

	/**
	 * This company, and the parents.
	 */
	private List<CompanyOrg> companyTree = new ArrayList<>();

	/**
	 * LDAP Name. Is <code>null</code> while not computed.
	 */
	private LdapName ldapName;

	/**
	 * All arguments constructor.
	 * 
	 * @param dn
	 *            Corresponds to the "Distinguished Name". Will be saved in "description".
	 * @param name
	 *            "name" corresponds to the "Organizational Unit". Will be saved in "name", and in "id" in is normalized
	 *            form.
	 */
	public CompanyOrg(final String dn, final String name) {
		super(dn, name);
		companyTree.add(this);
	}

	@Override
	public boolean equals(final Object other) {
		return other instanceof CompanyOrg && getId().equals(((CompanyOrg) other).getId());
	}

	@Override
	public int hashCode() {
		return getId().hashCode();
	}

}
