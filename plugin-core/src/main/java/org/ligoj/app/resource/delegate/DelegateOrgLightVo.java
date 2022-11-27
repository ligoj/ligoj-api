/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.delegate;

import org.ligoj.app.iam.SimpleUserOrg;
import org.ligoj.app.iam.model.DelegateType;
import org.ligoj.app.iam.model.ReceiverType;
import org.ligoj.bootstrap.core.INamableBean;
import org.ligoj.bootstrap.core.NamedAuditedBean;

import lombok.Getter;
import lombok.Setter;

/**
 * Delegation business object for display. The name corresponds to the human-readable name (CN for LDAP) of the target.
 */
@Getter
@Setter
public class DelegateOrgLightVo extends NamedAuditedBean<SimpleUserOrg, Integer> {

	/**
	 * SID, for Hazelcast
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The people receiving the delegate.
	 */
	private INamableBean<String> receiver;

	/**
	 * The type of people receiving this delegate.
	 */
	private ReceiverType receiverType;

	/**
	 * The delegate type.
	 */
	private DelegateType type;

	/**
	 * Can write with this delegate.
	 */
	private boolean canWrite;

	/**
	 * Can manage all delegates within the same scope.
	 */
	private boolean canAdmin;

	/**
	 * This entry can be managed by the current user.
	 */
	private boolean managed;

}
