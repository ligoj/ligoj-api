package org.ligoj.app.model;

import javax.persistence.Entity;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

/**
 * A {@link Node} right delegation. Grants to a user the right to create, delete and update a node or create
 * sub-nodes.<br>
 * The <code>name</code> corresponds to associated node. This is not a foreign key, in order to support provisioning and
 * safe deletion.
 */
@Getter
@Setter
@Entity
@Table(name = "SAAS_DELEGATE_NODE")
public class DelegateNode extends AbstractDelegate {

	/**
	 * SID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Can subscribe projects to any node within the scope of this delegate.
	 */
	private boolean canSubscribe;

	/**
	 * Set the related node's identifier. Alias for name property.
	 * 
	 * @param node
	 *            The related node.
	 */
	public void setNode(final String node) {
		setName(node);
	}
}
