package org.ligoj.app.model.ldap;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotBlank;

import org.ligoj.app.model.AbstractDelegate;
import org.ligoj.app.validation.DistinguishName;
import lombok.Getter;
import lombok.Setter;

/**
 * A LDAP right delegation. Grants to a user the right to create, delete and update the entries/membership/tree of
 * corresponding DN with included base DN.<br>
 * "name" attribute corresponds to the normalized form of corresponding CN. For "tree" delegate type, the name is equals
 * to the <code>-</code>, but should be ignored
 */
@Getter
@Setter
@Entity
@Table(name = "SAAS_DELEGATE_LDAP")
public class DelegateLdap extends AbstractDelegate {

	/**
	 * SID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The DN associated to this entry, including base DN to be able to perform SQL filters.
	 */
	@NotNull
	@NotBlank
	@Length(max = 512)
	@DistinguishName
	private String dn;

	/**
	 * The delegate type.
	 */
	@NotNull
	private DelegateLdapType type;

}
