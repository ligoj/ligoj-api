/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.node;

import java.util.List;
import java.util.Set;

import jakarta.validation.constraints.NotNull;

import org.ligoj.app.api.NodeVo;
import org.ligoj.app.model.ParameterType;
import org.ligoj.bootstrap.core.model.AbstractBusinessEntity;

import lombok.Getter;
import lombok.Setter;

/**
 * A fully described parameter.
 */
@Getter
@Setter
public class ParameterVo extends AbstractBusinessEntity<String> {

	/**
	 * Type
	 */
	@NotNull
	private ParameterType type;

	/**
	 * Minimal value for {@link ParameterType#INTEGER}
	 */
	private Integer min;

	/**
	 * Maximal value for {@link ParameterType#INTEGER}
	 */
	private Integer max;

	/**
	 * {@link ParameterType#SELECT}
	 */
	private List<String> values;

	/**
	 * Attached node consuming this parameter.
	 */
	private NodeVo owner;

	private boolean mandatory;

	private boolean secured;

	/**
	 * The optional default value of this parameter. May be <code>null</code>.
	 */
	private String defaultValue;

	/**
	 * Parameters this one is depending on.
	 */
	private Set<String> depends;

	/**
	 * <code>true</code> when this parameter is available at the subscription level (i.e. as a value bound to a
	 * {@link org.ligoj.app.model.Subscription}). Mirrors {@code Parameter#getAvailableForSubscription}; a {@code null}
	 * entity value is exposed as {@link Boolean#TRUE} so the wire shape is always definitive for the UI's mode-specific
	 * filtering in {@code SubscribeWizardView}.
	 */
	private Boolean availableForSubscription;

	/**
	 * <code>true</code> when this parameter is available at the node level (i.e. as a value bound to a
	 * {@link org.ligoj.app.model.Node} during node create / edit). Mirrors {@code Parameter#getAvailableForNode};
	 * a {@code null} entity value is exposed as {@link Boolean#TRUE}.
	 */
	private Boolean availableForNode;

}
