package org.ligoj.app.model;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

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
public class Subscription extends AbstractAudited<Integer> {

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
}
