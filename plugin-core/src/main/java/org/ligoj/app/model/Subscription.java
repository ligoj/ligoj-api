/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.model;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import org.ligoj.app.api.NodeScoped;
import org.ligoj.app.api.SubscriptionMode;
import org.ligoj.bootstrap.core.model.AbstractAudited;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Subscription to a service.
 */
@Getter
@Setter
@Entity
@Table(name = "LIGOJ_SUBSCRIPTION")
@ToString(of = { "node", "project" }, callSuper = true)
public class Subscription extends AbstractAudited<Integer> implements NodeScoped<Integer>, Serializable {

	/**
	 * SID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The subscribed service.
	 */
	@ManyToOne
	@NotNull
	private Node node;

	/**
	 * The project related to this subscription.
	 */
	@ManyToOne
	@NotNull
	private Project project;

	/**
	 * Subscription mode. By default is the node's mode
	 */
	private SubscriptionMode mode;

}
