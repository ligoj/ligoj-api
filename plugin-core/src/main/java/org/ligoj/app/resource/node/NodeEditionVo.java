/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.node;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;

import org.ligoj.app.model.NodeId;
import org.ligoj.app.model.Refining;
import org.ligoj.bootstrap.core.INamableBean;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * Node object for edition.
 */
@Getter
@Setter
public class NodeEditionVo extends AbstractParameterizedVo implements INamableBean<String>, Refining<String> {

	/**
	 * SID, for Hazelcast
	 */
	private static final long serialVersionUID = 1L;

	@NotBlank
	private String name;

	/**
	 * When <code>true</code> the previous parameters are not updated.
	 */
	private boolean untouchedParameters;

	/**
	 * Optional expiry of the node's credentials entered by an administrator. Ignored when the tool plug-in detects it.
	 */
	@Setter(AccessLevel.NONE)
	private Instant credentialExpiry;

	/**
	 * Whether {@link #credentialExpiry} was sent, even as <code>null</code> to remove it. A client without this feature,
	 * such as an older UI or ligoj-cli, does not send it: the stored expiry is then kept.
	 */
	@Setter(AccessLevel.NONE)
	private boolean credentialExpirySet;

	/**
	 * Set the expiry of the node's credentials entered by an administrator.
	 *
	 * @param credentialExpiry The expiry, <code>null</code> to remove it.
	 */
	public void setCredentialExpiry(final Instant credentialExpiry) {
		this.credentialExpiry = credentialExpiry;
		this.credentialExpirySet = true;
	}

	/**
	 * The node identifier.
	 */
	@NotBlank
	@NodeId
	private String id;

	@Override
	public String getRefined() {
		return getNode();
	}

}
