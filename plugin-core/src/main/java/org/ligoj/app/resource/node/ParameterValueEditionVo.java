package org.ligoj.app.resource.node;

import java.util.Date;
import java.util.List;

import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Parameter value for edition.
 */
@Getter
@Setter
@ToString(of = "parameter")
public class ParameterValueEditionVo {

	/**
	 * Parameter's identifier.
	 */
	@NotNull
	private String parameter;

	/**
	 * Not null for {@value org.ligoj.app.model.ParameterType#TEXT}
	 */
	private String text;

	/**
	 * Not null for {@value org.ligoj.app.model.ParameterType#TAGS}
	 */
	private List<String> tags;

	/**
	 * Not null for {@value org.ligoj.app.model.ParameterType#MULTIPLE}
	 */
	private List<Integer> selections;

	/**
	 * Not null for {@value org.ligoj.app.model.ParameterType#INTEGER}
	 */
	private Integer integer;

	/**
	 * Not null for {@value org.ligoj.app.model.ParameterType#SELECT}. Represents index of selected item.
	 */
	private Integer index;

	/**
	 * Not null for {@value org.ligoj.app.model.ParameterType#BINARY}.
	 */
	private Boolean binary;

	/**
	 * Not null for {@value org.ligoj.app.model.ParameterType#DATE}.
	 */
	private Date date;
}
