/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.plugin;

import java.util.Arrays;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.lang3.NotImplementedException;
import org.ligoj.app.api.ToolPlugin;
import org.ligoj.app.dao.SubscriptionRepository;
import org.ligoj.app.model.Node;
import org.ligoj.app.model.Parameter;
import org.ligoj.app.resource.node.ParameterValueResource;
import org.ligoj.app.resource.subscription.SubscriptionResource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Basic implementation of a tool plug-in.
 */
public abstract class AbstractToolPluginResource implements ToolPlugin {

	@Autowired
	protected SubscriptionResource subscriptionResource;

	@Autowired
	protected ParameterValueResource pvResource;

	@Autowired
	protected SubscriptionRepository subscriptionRepository;

	/**
	 * Return the version of tool or <code>null</code> if not available/found.
	 * 
	 * @param subscription
	 *            the subscription's identifier to use to locate the target instance.
	 * @return the version of tool or <code>null</code> if not available/found.
	 * @throws Exception
	 *             When version cannot be be retrieved.
	 */
	@GET
	@Path("version/{subscription:\\d+}")
	public String getVersion(@PathParam("subscription") final int subscription) throws Exception {
		// Return the version from the subscription parameters
		return getVersion(subscriptionResource.getParameters(subscription));
	}

	@Override
	public void create(final int subscription) throws Exception {
		throw new NotImplementedException("");
	}

	/**
	 * Simple shortcut for a OK and download header.
	 * 
	 * @param output
	 *            Source entity.
	 * @param file
	 *            The target file name.
	 * @return the {@link Response} ready to be consumed.
	 */
	public static ResponseBuilder download(final StreamingOutput output, final String file) {
		return Response.ok().header("Content-Disposition", "attachment; filename=" + file + ";").entity(output);
	}

	@Override
	public List<Class<?>> getInstalledEntities() {
		return Arrays.asList(Node.class, Parameter.class);
	}

}
