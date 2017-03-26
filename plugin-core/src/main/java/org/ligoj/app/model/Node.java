package org.ligoj.app.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.ligoj.bootstrap.core.model.AbstractDescribedBusinessEntity;
import org.ligoj.app.api.SubscriptionMode;
import lombok.Getter;
import lombok.Setter;

/**
 * Node definition. Node#key is used as additional business key.
 */
@Getter
@Setter
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "name"), name = "LIGOJ_NODE")
public class Node extends AbstractDescribedBusinessEntity<String> {

	/**
	 * SID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Instance of tool proving the expected service.
	 */
	@ManyToOne
	private Node refined;

	/**
	 * The subscription mode. When <code>null</code>, the node cannot be used for any mode.
	 */
	@Enumerated(EnumType.STRING)
	@Column(length = 10)
	private SubscriptionMode mode;

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
}
