package org.ligoj.app.model;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.hibernate.validator.constraints.Length;

import org.ligoj.bootstrap.core.model.AbstractAudited;

/**
 * Parameter instance.
 */
@Getter
@Setter
@Entity
@Table(name = "LIGOJ_PARAMETER_INSTANCE")
@ToString(of = { "parameter", "data" })
public class ParameterValue extends AbstractAudited<Integer> {

	/**
	 * SID
	 */
	private static final long serialVersionUID = 1L;

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
