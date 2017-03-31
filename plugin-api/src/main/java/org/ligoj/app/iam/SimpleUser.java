package org.ligoj.app.iam;

import java.io.Serializable;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.NotBlank;

import org.ligoj.bootstrap.core.INamableBean;
import org.ligoj.bootstrap.core.validation.LowerCase;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Abstract LDAP entry
 */
@Getter
@Setter
@ToString(of = "id")
@EqualsAndHashCode(of = "id")
public class SimpleUser implements INamableBean<String>, Serializable {

	/**
	 * SID
	 */
	private static final long serialVersionUID = 1L;

	public static final String USER_PATTERN = "[a-z0-9](?:-?[\\-a-z0-9])+";

	public static final String USER_PATTERN_WRAPPER = "^" + USER_PATTERN + "$";

	/**
	 * Property representing the parent alias name.
	 */
	public static final String COMPANY_ALIAS = "company";

	/**
	 * User first name.
	 */
	@NotNull
	@NotBlank
	@Size(max = 50)
	private String firstName;

	/**
	 * User last name.
	 */
	@NotNull
	@NotBlank
	@Size(max = 50)
	private String lastName;

	/**
	 * User name/login/UID.
	 */
	@NotNull
	@NotBlank
	@LowerCase
	@Size(max = 50)
	@Pattern(regexp = USER_PATTERN_WRAPPER)
	private String id;

	/**
	 * Normalized Organizational Unit.
	 */
	@NotNull
	@NotBlank
	@Size(max = 100)
	@Pattern(regexp = USER_PATTERN_WRAPPER)
	private String company;

	/**
	 * Department identifier.
	 */
	private String department;

	/**
	 * Local identifier. Is not unique among the the users.
	 */
	private String localId;

	@Override
	public String getName() {
		return getId();
	}

	@Override
	public void setName(final String name) {
		setId(name);
	}

	/**
	 * Copy all non secured data.
	 * 
	 * @param to
	 *            the target object.
	 */
	public void copy(final SimpleUser to) {
		to.setId(getId());
		to.setFirstName(getFirstName());
		to.setLastName(getLastName());
		to.setCompany(getCompany());
		to.setDepartment(getDepartment());
		to.setLocalId(getLocalId());
	}

}
