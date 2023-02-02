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
 * Delegation business object for updates. The name corresponds to the human-readable name (CN for LDAP) of the target.
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
	 * The people receiving the delegation.
	 */
	@NotBlank
	private String receiver;

	/**
	 * The type of people receiving this delegate.
	 */
	private ReceiverType receiverType = ReceiverType.USER;

	/**
	 * The delegate type.
	 */
	@NotNull
	private DelegateType type;

	/**
	 * Can write with this delegate.
	 */
	private boolean canWrite;

	/**
	 * Can manage all delegation within the same scope.
	 */
	private boolean canAdmin;

}
