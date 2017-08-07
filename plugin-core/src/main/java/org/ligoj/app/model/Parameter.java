package org.ligoj.app.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Length;
import org.ligoj.app.api.SubscriptionMode;
import org.ligoj.bootstrap.core.model.AbstractBusinessEntity;

import lombok.Getter;
import lombok.Setter;

/**
 * Parameter definition.
 */
@Getter
@Setter
@Entity
@Table(name = "LIGOJ_PARAMETER")
public class Parameter extends AbstractBusinessEntity<String> {

	private static final long serialVersionUID = 1L;

	/**
	 * Type
	 */
	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(length = 10)
	private ParameterType type = ParameterType.TEXT;

	/**
	 * Optional JSon constraint data. The value of this field can be read from different manners depending on the
	 * {@link #type} value :
	 * <ul>
	 * <li>Case of 'text' : contains an optional 'pattern' property to validate the text.</li>
	 * <li>Case of 'tags' : this constraint is useless, and can be null.</li>
	 * <li>Case of 'integer' : contains optional constraint 'min' and 'max' properties.</li>
	 * <li>Case of 'select' : this constraint is read as list of available values, single selection mode. 
	 * Contains optional constraints 'min' and 'max' properties.</li>
	 * <li>Case of 'multiple' : this constraint is read as list of available values, multiple selection mode.</li>
	 * <li>Case of 'date' : this constraint is useless, and can be null.</li>
	 * <li>Case of 'binary' : this constraint is useless, and can be null.</li>
	 * </ul>
	 */
	@Length(max = 500)
	private String data;

	/**
	 * <code>true</code> when this pa
	 */
	private boolean mandatory;

	/**
	 * Attached node consuming this parameter.
	 */
	@ManyToOne
	@NotNull
	@JoinColumn(name = "owner")
	private Node owner;

	/**
	 * Associated mode.
	 */
	@Enumerated(EnumType.STRING)
	@Column(length = 10)
	@NotNull
	private SubscriptionMode mode = SubscriptionMode.ALL;

	/**
	 * <code>true</code> when this parameter is secured. A secured parameter is not available for read access and will be encrypted. 
	 */
	private boolean secured;
}
