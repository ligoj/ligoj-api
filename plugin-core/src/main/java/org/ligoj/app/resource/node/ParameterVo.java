package org.ligoj.app.resource.node;

import java.util.List;

import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;
import org.ligoj.bootstrap.core.DescribedBean;
import org.ligoj.app.api.NodeVo;
import org.ligoj.app.model.ParameterType;

/**
 * A fully described parameter.
 */
@Getter
@Setter
public class ParameterVo extends DescribedBean<String> {

	/**
	 * Type
	 */
	@NotNull
	private ParameterType type;

	/**
	 * Minimal value for {@value ParameterType#INTEGER}
	 */
	private Integer min;

	/**
	 * Maximal value for {@value ParameterType#INTEGER}
	 */
	private Integer max;

	/**
	 * {@value ParameterType#SELECT}
	 */
	private List<String> values;

	/**
	 * Attached node consuming this parameter.
	 */
	private NodeVo owner;

	private boolean mandatory;

}
