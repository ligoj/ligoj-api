/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

import org.ligoj.app.api.NodeScoped;
import org.ligoj.app.api.SubscriptionMode;
import org.ligoj.bootstrap.core.model.AbstractNamedBusinessEntity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

/**
 * Node definition. Node#key is used as additional business key.
 */
@Getter
@Setter
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "name"), name = "LIGOJ_NODE")
public class Node extends AbstractNamedBusinessEntity<String> implements Refining<Node>, NodeScoped<String> {

	/**
	 * SID, for Hazelcast
	 */
	private static final long serialVersionUID = 1L;

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
	 * Optional tag name.
	 */
	private String tag;

	/**
	 * Optional CSS classes used to render the tag.
	 */
	private String tagUiClasses;

	@Override
	@Transient
	@JsonIgnore
	public Node getNode() {
		return this;
	}

}
