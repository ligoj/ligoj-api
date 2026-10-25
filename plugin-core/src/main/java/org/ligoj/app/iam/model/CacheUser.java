/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.iam.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.Length;
import org.ligoj.bootstrap.core.model.AbstractBusinessEntity;

import lombok.Getter;
import lombok.Setter;

/**
 * IAM User cache. With the attributes the identity providers read, a provider rebuilds its users from this table after
 * a restart, without reading its server. Every attribute column is nullable: a schema update adds it to existing rows.
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
	@org.hibernate.validator.constraints.Length(max = 255)
	private String mails;

	/**
	 * Organizational Unit CN.
	 */
	@ManyToOne
	private CacheCompany company;

	/**
	 * The DN of this user in its identity provider.
	 */
	@org.hibernate.validator.constraints.Length(max = 1024)
	private String dn;

	/**
	 * All mails, in their order, as a JSON array. Unbounded text, still searchable: an account may have many aliases.
	 */
	@Column(length = Length.LONG32)
	private String mailList;

	/**
	 * When <code>true</code>, the user has a password.
	 */
	private Boolean secured;

	/**
	 * The lock date, <code>null</code> when not locked.
	 */
	private Instant locked;

	/**
	 * The principal who locked this user.
	 */
	private String lockedBy;

	/**
	 * The company before an isolation, <code>null</code> when not isolated.
	 */
	private String isolated;

	/**
	 * The department.
	 */
	private String department;

	/**
	 * The local identifier.
	 */
	private String localId;

	/**
	 * The custom attribute values, as a JSON object. Unbounded text, still searchable: a value may be long text.
	 */
	@Column(length = Length.LONG32)
	private String customAttributes;

}
