/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;
import org.ligoj.bootstrap.core.model.AbstractPersistable;

import java.time.Instant;

/**
 * A browser side error logged by an authenticated user. This is a user-scoped log (not node-scoped): each entry is
 * owned by the user that produced it.
 */
@Getter
@Setter
@Entity
@Table(name = "LIGOJ_USER_LOG")
public class UserLog extends AbstractPersistable<Integer> {

	/**
	 * Login of the user owning this log. Filled by the server from the security context.
	 * <p>
	 * The column is named {@code user_login} since {@code USER} is a reserved keyword on several databases (H2,
	 * PostgreSQL, ...).
	 */
	@Length(max = 100)
	@Column(name = "user_login")
	private String user;

	/**
	 * Date of the error. Filled by the server.
	 */
	private Instant date;

	/**
	 * Error message. Truncated to 2000 characters by the server.
	 */
	@Column(length = 2000)
	private String message;

	/**
	 * Path (without domain) of the page where the error occurred.
	 */
	@Length(max = 512)
	private String url;
}
