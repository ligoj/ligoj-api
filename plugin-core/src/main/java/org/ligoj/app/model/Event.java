/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.model;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import org.hibernate.validator.constraints.Length;
import org.ligoj.app.api.NodeScoped;
import org.ligoj.bootstrap.core.model.AbstractPersistable;

import lombok.Getter;
import lombok.Setter;

/**
 * Event associated to a node or a subscription.
 */
@Getter
@Setter
@Entity
@Table(name = "LIGOJ_EVENT")
public class Event extends AbstractPersistable<Integer> implements NodeScoped<Integer> {

	/**
	 * Type of event : status, ...
	 */
	@Enumerated(EnumType.STRING)
	@Column(length = 10)
	private EventType type;

	/**
	 * Value of the event : up, down, ...
	 */
	@Length(max = 100)
	private String value;

	/**
	 * Date of event.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	private Date date;

	/**
	 * Optional linked node providing this value. Either this attribute, either {@link #subscription} is not
	 * <code>null</code>.
	 */
	@ManyToOne
	private Node node;

	/**
	 * Optional linked subscription providing this value. Either this attribute, either {@link #node} is not
	 * <code>null</code>.
	 */
	@ManyToOne
	private Subscription subscription;
}
