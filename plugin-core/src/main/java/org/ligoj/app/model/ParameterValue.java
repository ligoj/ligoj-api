/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.model;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Length;
import org.ligoj.app.api.NodeScoped;
import org.ligoj.bootstrap.core.model.AbstractAudited;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Parameter value.
 */
@Getter
@Setter
@Entity
@Table(name = "LIGOJ_PARAMETER_VALUE", uniqueConstraints = { @UniqueConstraint(columnNames = { "parameter", "node" }),
		@UniqueConstraint(columnNames = { "parameter", "subscription" }) })
@ToString(of = { "parameter", "data" })
public class ParameterValue extends AbstractAudited<Integer> implements NodeScoped<Integer> {

	/**
	 * Parameter definition
	 */
	@NotNull
	@ManyToOne
	private Parameter parameter;

	/**
	 * JSon data can be read from different manners depending on the {@link #parameter} type value :
	 * <ul>
	 * <li>Case of 'text' : data is plain text.</li>
	 * <li>Case of 'tags' : data is the array of entered tags.</li>
	 * <li>Case of 'integer' : data is plain text to convert with {@link Integer#parseInt(String)}.</li>
	 * <li>Case of 'select' : data is index within the provided list of attached criteria.</li>
	 * <li>Case of 'multiple' : data is the array of indexes within the provided list of attached criteria.</li>
	 * <li>Case of 'date' : data is a UNIX timestamp.</li>
	 * <li>Case of 'binary' : data is plain text to convert to boolean with {@link Boolean#parseBoolean(String)}.</li>
	 * </ul>
	 */
	@Length(max = 500)
	private String data;

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
