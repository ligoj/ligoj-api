/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.model;

import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.NotNull;

import org.ligoj.bootstrap.core.model.ToIdSerializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import lombok.Getter;
import lombok.Setter;

/**
 * Long task status where locked resource is a node.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class AbstractLongTaskNode extends AbstractLongTask<Node, String> {

	@ManyToOne
	@NotNull
	@JsonSerialize(using = ToIdSerializer.class)
	private Node locked;

}
