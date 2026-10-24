/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ligoj.app.api.NodeScoped;
import org.ligoj.app.api.SubscriptionMode;
import org.ligoj.bootstrap.core.INamableBean;
import org.ligoj.bootstrap.core.model.AbstractStringKeyEntity;

import java.time.Instant;

/**
 * Node definition. Node#key is used as additional business key.
 */
@Getter
@Setter
@Entity
@ToString(callSuper = true)
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "name"), name = "LIGOJ_NODE")
public class Node extends AbstractStringKeyEntity implements Refining<Node>, NodeScoped<String>, INamableBean<String> {

	/**
	 * SID, for Hazelcast
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Maximal length of {@link #version}.
	 */
	public static final int VERSION_LENGTH = 255;

	@NotBlank
	private String name;

	/**
	 * Instance of tool proving the expected service.
	 */
	@ManyToOne
	private Node refined;

	/**
	 * The subscription mode.
	 */
	@Enumerated(EnumType.STRING)
	@Column(length = 10)
	@NotNull
	private SubscriptionMode mode = SubscriptionMode.ALL;

	/**
	 * Optional CSS classes used to render this node.
	 */
	private String uiClasses;

	/**
	 * Optional preferred CSS color (e.g. {@code "#0052CC"}) used to accent this
	 * node in the UI. Companion of {@link #uiClasses}; {@code null} when unset.
	 */
	private String uiColor;

	/**
	 * Optional tag name.
	 */
	private String tag;

	/**
	 * Optional CSS classes used to render the tag.
	 */
	private String tagUiClasses;

	/**
	 * Optional expiry of the credentials this node uses to call its tool: entered by an administrator, or detected by
	 * the tool plug-in during the status check, see {@link #credentialExpirySource}. <code>null</code> when unknown or
	 * when the detected credentials never expire.
	 */
	private Instant credentialExpiry;

	/**
	 * Origin of {@link #credentialExpiry}. <code>null</code> when nothing is known.
	 */
	@Enumerated(EnumType.STRING)
	@Column(length = 10)
	private CredentialExpirySource credentialExpirySource;

	/**
	 * Optional version of the tool this node calls, detected by the tool plug-in during the status check of an UP
	 * node. <code>null</code> when never detected. The last detected one is kept when the tool no longer tells.
	 */
	@Column(length = VERSION_LENGTH)
	private String version;

	/**
	 * Optional latest released version of a tool, stored on the tool node: its plug-in reads it during the status check
	 * of its instances, or of the tool itself. <code>null</code> when never read. The last read one is kept when the
	 * plug-in no longer tells.
	 */
	@Column(length = VERSION_LENGTH)
	private String lastVersion;

	@Override
	@Transient
	@JsonIgnore
	public Node getNode() {
		return this;
	}

}
