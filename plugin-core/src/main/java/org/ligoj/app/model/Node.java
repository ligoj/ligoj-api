/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ligoj.app.api.NodeScoped;
import org.ligoj.app.api.SubscriptionMode;
import org.ligoj.bootstrap.core.INamableBean;
import org.springframework.data.domain.Persistable;

/**
 * Node definition. Node#key is used as additional business key.
 */
@Getter
@Setter
@Entity
@ToString
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "name"), name = "LIGOJ_NODE")
public class Node implements Refining<Node>, NodeScoped<String>, INamableBean<String>, Persistable<String> {

	/**
	 * SID, for Hazelcast
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Business key.
	 */
	@Id
	@NotNull
	private String id;

	@NotBlank
	private String name;

	/**
	 * Instance of tool proving the expected service.
	 */
	@ManyToOne
	private Node refined;

	/**
	 * The subscription mode.
	 */
	@Enumerated(EnumType.STRING)
	@Column(length = 10)
	@NotNull
	private SubscriptionMode mode = SubscriptionMode.ALL;

	/**
	 * Optional CSS classes used to render this node.
	 */
	private String uiClasses;

	/**
	 * Optional tag name.
	 */
	private String tag;

	/**
	 * Optional CSS classes used to render the tag.
	 */
	private String tagUiClasses;

	@Override
	@Transient
	@JsonIgnore
	public Node getNode() {
		return this;
	}

	/**
	 * Returns if the {@code Persistable} is new or was persisted already.
	 *
	 * @return if {@literal true} the object is new.
	 */
	public boolean isNew() {
		return getId() == null;
	}

}
