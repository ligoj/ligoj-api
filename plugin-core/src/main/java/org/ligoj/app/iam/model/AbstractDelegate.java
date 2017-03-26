package org.ligoj.app.iam.model;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.validator.constraints.NotBlank;
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
	 * SID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The user receiving the delegation.
	 */
	@NotBlank
	@NotNull
	private String receiver;

	/**
	 * The user receiving the delegation.
	 */
	@NotNull
	@Enumerated(EnumType.STRING)
	@ColumnDefault("USER")
	private ReceiverType receiverType = ReceiverType.USER;

	/**
	 * Can write, create or update elements.
	 */
	private boolean canWrite;

	/**
	 * Can manage all delegations within the same scope and propagate this right to another user. This flag is required
	 * to update, create or delete any delegate.
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
