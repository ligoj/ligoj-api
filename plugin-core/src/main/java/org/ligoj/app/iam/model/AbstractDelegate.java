/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.iam.model;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.ColumnDefault;
import org.ligoj.bootstrap.core.model.AbstractNamedAuditedEntity;

import lombok.Getter;
import lombok.Setter;

/**
 * An abstract definition of a delegated authorization given to a user.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class AbstractDelegate extends AbstractNamedAuditedEntity<Integer> {

	/**
	 * SID, for Hazelcast
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The resource receiving the delegation.
	 */
	@NotBlank
	private String receiver;

	/**
	 * The user receiving the delegation.
	 */
	@NotNull
	@Enumerated(EnumType.STRING)
	@ColumnDefault("'USER'")
	@Column(length = 10)
	private ReceiverType receiverType = ReceiverType.USER;

	/**
	 * Receiver can create, delete or update the related resource.
	 */
	private boolean canWrite;

	/**
	 * Receiver can create, delete or update all delegates within the same scope and propagate this right to another
	 * receiver. This include this delegate. The 'canWrite' attribute cannot be augmented.
	 */
	private boolean canAdmin;

	/**
	 * Return the explicit reference identifier of delegated object. Only a proxy version of {@link #getName()}
	 * 
	 * @return the explicit reference identifier of delegated object.
	 */
	public String getReferenceID() {
		return getName();
	}

}
