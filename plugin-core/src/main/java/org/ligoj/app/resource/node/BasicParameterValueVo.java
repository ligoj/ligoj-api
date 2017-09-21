package org.ligoj.app.resource.node;

import java.util.Date;
import java.util.List;

import javax.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

import org.ligoj.app.iam.SimpleUserOrg;
import org.ligoj.bootstrap.core.AuditedBean;

/**
 * A criteria value.
 */
@Getter
@Setter
public class BasicParameterValueVo extends AuditedBean<SimpleUserOrg, Integer> {

	/**
	 * Not <code>null</code> for {@value org.ligoj.app.model.ParameterType#TEXT}
	 */
	@NotBlank
	private String text;

	/**
	 * Not <code>null</code> for {@value org.ligoj.app.model.ParameterType#TAGS}
	 */
	private List<String> tags;

	/**
	 * Not <code>null</code> for {@value org.ligoj.app.model.ParameterType#MULTIPLE}
	 */
	private List<Integer> selections;

	/**
	 * Not <code>null</code> for {@value org.ligoj.app.model.ParameterType#INTEGER}
	 */
	private Integer integer;

	/**
	 * Not <code>null</code> for {@value org.ligoj.app.model.ParameterType#SELECT}. Represents index of selected item.
	 */
	private Integer index;

	/**
	 * Not <code>null</code> for {@value org.ligoj.app.model.ParameterType#BINARY}.
	 */
	private Boolean bool;

	/**
	 * Not <code>null</code> for {@value org.ligoj.app.model.ParameterType#DATE}.
	 */
	private Date date;

}
