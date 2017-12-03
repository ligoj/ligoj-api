package org.ligoj.app.model;

import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

/**
 * Long task status where locked resource is a node.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class AbstractLongTaskNode extends AbstractLongTask<Node, String> {

	private static final long serialVersionUID = 1L;

	@ManyToOne
	@JsonIgnore
	@NotNull
	private Node locked;

}
