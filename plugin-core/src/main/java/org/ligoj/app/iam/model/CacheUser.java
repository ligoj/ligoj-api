/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.iam.model;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.validator.constraints.Length;

import org.ligoj.bootstrap.core.model.AbstractBusinessEntity;

/**
 * IAM User cache.
 */
@Entity
@Table(name = "LIGOJ_CACHE_USER")
@Getter
@Setter
public class CacheUser extends AbstractBusinessEntity<String> {

	/**
	 * User first name.
	 */
	private String firstName;

	/**
	 * User last name.
	 */
	private String lastName;

	/**
	 * Mails using ',' as separator.
	 */
	@Length(max = 255)
	private String mails;

	/**
	 * Organizational Unit CN.
	 */
	@ManyToOne
	private CacheCompany company;

}
