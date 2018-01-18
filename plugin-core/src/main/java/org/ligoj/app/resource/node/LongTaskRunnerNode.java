package org.ligoj.app.resource.node;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.ligoj.app.dao.NodeRepository;
import org.ligoj.app.dao.task.LongTaskNodeRepository;
import org.ligoj.app.model.AbstractLongTaskNode;
import org.ligoj.app.model.Node;
import org.ligoj.app.resource.plugin.LongTaskRunner;
import org.ligoj.bootstrap.core.resource.OnNullReturn404;

/**
 * A resource running some long task. Implementing this interface causes the
 * subscription management checks there is no running task when a deletion is
 * requested. The contract :
 * <ul>
 * <li>At most one task can run per node</li>
 * <li>A subscription cannot be deleted while there is a running attached
 * task</li>
 * <li>A running task is task without "end" date.
 * <li>When a task is started, is will always ends.
 * <li>When a task ends, the status (boolean) is always updated.
 * </ul>
 */
public interface LongTaskRunnerNode<T extends AbstractLongTaskNode, R extends LongTaskNodeRepository<T>>
		extends LongTaskRunner<T, R, Node, String, NodeRepository> {
	@Override
	default NodeRepository getLockedRepository() {
		return getNodeRepository();
	}

	/**
	 * Return the {@link NodeRepository}.
	 * 
	 * @return The repository used to fetch related subscription entity of a task.
	 */
	NodeRepository getNodeRepository();

	/**
	 * Return status of import.
	 * 
	 * @param node
	 *            The locked node identifier.
	 * @return status of import. May <code>null</code> when there is no previous
	 *         task.
	 */
	@Override
	@GET
	@Path("{node:service:.+}/task")
	default T getTask(@PathParam("node") final String node) {
		return LongTaskRunner.super.getTask(node);
	}

	/**
	 * Cancel (stop) current the catalog update. Synchronous operation, flag the
	 * task as failed.
	 * 
	 * @param node
	 *            The node (provider) to cancel update.
	 * @return The ended task if present or <code>null</code>.
	 */
	@DELETE
	@Path("{node:service:.+}/task")
	@OnNullReturn404
	default T cancel(@PathParam("node") final String node) {
		return endTask(node, true);
	}

}
