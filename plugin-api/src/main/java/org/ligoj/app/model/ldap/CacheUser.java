package org.ligoj.app.model.ldap;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.validator.constraints.Length;

import org.ligoj.bootstrap.core.model.AbstractBusinessEntity;

/**
 * LDAP user cache.
 */
@Entity
@Table(name = "SAAS_CACHE_USER")
@Getter
@Setter
public class CacheUser extends AbstractBusinessEntity<String> {

	/**
	 * SID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * User first name.
	 */
	private String firstName;

	/**
	 * User last name.
	 */
	private String lastName;

	/**
	 * Mails
	 */
	@Length(max = 255)
	private String mails;

	/**
	 * Organizational Unit CN.
	 */
	@ManyToOne
	private CacheCompany company;

}
