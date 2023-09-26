/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.delegate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.ligoj.app.iam.model.DelegateType;
import org.ligoj.app.iam.model.ReceiverType;
import org.ligoj.bootstrap.core.NamedBean;

import lombok.Getter;
import lombok.Setter;

/**
 * Delegation business object for updates. The name corresponds to the human-readable name (CN for LDAP) of the related resource.
 * It will be normalized to be used as identifier.
 */
@Getter
@Setter
public class DelegateOrgEditionVo extends NamedBean<Integer> {

	/**
	 * SID, for Hazelcast
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The one receiving the delegation.
	 */
	@NotBlank
	private String receiver;

	/**
	 * The type of receiver of this delegate.
	 */
	private ReceiverType receiverType = ReceiverType.USER;

	/**
	 * The delegated resource type.
	 */
	@NotNull
	private DelegateType type;

	/**
	 * Can write with this delegate?
	 */
	private boolean canWrite;

	/**
	 * Can manage all delegations within the same scope?
	 */
	private boolean canAdmin;

}
