/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.iam;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.ligoj.bootstrap.core.INamableBean;
import org.ligoj.bootstrap.core.validation.LowerCase;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

/**
 * Abstract LDAP entry
 */
@Getter
@Setter
@ToString(of = "id")
@EqualsAndHashCode(of = "id")
public class SimpleUser implements INamableBean<String> {

	/**
	 * SID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * User identifier pattern.
	 */
	public static final String USER_PATTERN = "[a-z0-9](?:-?[\\-a-z\\.@_0-9]){1,100}";

	/**
	 * User identifier pattern in REST path.
	 */
	public static final String USER_PATTERN_WRAPPER = "^" + USER_PATTERN + "$";

	/**
	 * Property representing the parent alias name.
	 */
	public static final String COMPANY_ALIAS = "company";

	/**
	 * User first name.
	 */
	@NotBlank
	@Size(max = 50)
	private String firstName;

	/**
	 * User last name.
	 */
	@NotBlank
	@Size(max = 50)
	private String lastName;

	/**
	 * Username/login/UID.
	 */
	@NotBlank
	@LowerCase
	@Size(max = 50)
	@Pattern(regexp = USER_PATTERN_WRAPPER)
	private String id;

	/**
	 * Normalized Organizational Unit.
	 */
	@NotBlank
	@Size(max = 100)
	@Pattern(regexp = USER_PATTERN_WRAPPER)
	private String company;

	/**
	 * Department identifier.
	 */
	private String department;

	/**
	 * Local identifier. Is not unique among all the users, but unique within the original source repository.
	 */
	private String localId;

	@JsonIgnoreProperties
	@Override
	public String getName() {
		return getId();
	}

	@Override
	public void setName(final String name) {
		setId(name);
	}

	/**
	 * Optional custom attributes.
	 */
	private Map<String, String> customAttributes;

	/**
	 * Copy all non secured data.
	 *
	 * @param to the target object.
	 */
	public void copy(final SimpleUser to) {
		to.setId(getId());
		to.setFirstName(getFirstName());
		to.setLastName(getLastName());
		to.setCompany(getCompany());
		to.setDepartment(getDepartment());
		to.setLocalId(getLocalId());
		to.setCustomAttributes(getCustomAttributes());
	}

}
