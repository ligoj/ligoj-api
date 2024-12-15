/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.node;

import org.ligoj.app.api.NodeVo;
import org.ligoj.app.model.Node;
import org.ligoj.app.model.ParameterValue;
import org.ligoj.app.resource.ServicePluginLocator;
import org.ligoj.bootstrap.core.NamedBean;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * {@link Node} resource.
 */
public class NodeHelper {

	/**
	 * {@link Node} JPA to business object transformer.
	 *
	 * @param locator Plug-in locator to resolve the enabled and available plug-ins.
	 * @param entity  Source entity.
	 * @return The corresponding VO object with recursive redefined reference.
	 */
	public static NodeVo toVo(final Node entity, final ServicePluginLocator locator) {
		final var vo = toVo(entity);
		vo.setEnabled(locator.isEnabled(entity.getId()));
		return vo;
	}

	/**
	 * {@link Node} JPA to business object transformer.
	 *
	 * @param entity Source entity.
	 * @return The corresponding VO object with recursive redefined reference.
	 */
	public static NodeVo toVo(final Node entity) {
		final var vo = toVoLight(entity);
		if (entity.isRefining()) {
			vo.setRefined(toVo(entity.getRefined()));
		}
		return vo;
	}

	/**
	 * {@link Node} JPA to business object transformer with parameters.
	 *
	 * @param locator Plug-in locator to resolve the enabled and available plug-ins.
	 * @param entity  Source entity.
	 * @return The corresponding VO object with resources and without recursive parent reference.
	 */
	private static NodeVo toVoParameter(final Node entity, final ServicePluginLocator locator) {
		final var vo = toVoParameter(entity);
		vo.setEnabled(locator.isEnabled(entity.getId()));
		return vo;
	}

	/**
	 * {@link Node} JPA to business object transformer with parameters.
	 *
	 * @param entity Source entity.
	 * @return The corresponding VO object with resources and without recursive parent reference.
	 */
	private static NodeVo toVoParameter(final Node entity) {
		final var vo = toVoLight(entity);
		vo.setParameters(new HashMap<>());
		vo.setTag(entity.getTag());
		vo.setTagUiClasses(entity.getTagUiClasses());
		return vo;
	}

	/**
	 * {@link Node} JPA to VO object transformer without refined information.
	 *
	 * @param locator Plug-in locator to resolve the enabled and available plug-ins.
	 * @param entity  Source entity.
	 * @return The corresponding VO object without recursive redefined reference.
	 */
	protected static NodeVo toVoLight(final Node entity, final ServicePluginLocator locator) {
		final var vo = toVoLight(entity);
		vo.setEnabled(locator.isEnabled(entity.getId()));
		return vo;
	}

	/**
	 * {@link Node} JPA to VO object transformer without refined information.
	 *
	 * @param entity Source entity.
	 * @return The corresponding VO object without recursive redefined reference.
	 */
	public static NodeVo toVoLight(final Node entity) {
		final var vo = new NodeVo();
		NamedBean.copy(entity, vo);
		vo.setMode(entity.getMode());
		vo.setUiClasses(entity.getUiClasses());
		return vo;
	}

	/**
	 * JPA {@link Node} associated to {@link ParameterValue} to detailed {@link NodeVo} converter. This not a one to one
	 * {@link Function}.
	 *
	 * @param nodesAndValues Nodes with values.
	 * @return The corresponding VO objects with recursive redefined reference.
	 */
	static Map<String, NodeVo> toVoParameters(final List<Object[]> nodesAndValues,
			final ServicePluginLocator locator) {

		// Build the nodes
		final var nodes = new HashMap<String, NodeVo>();
		final var entities = new HashMap<String, Node>();
		for (final var resultSet : nodesAndValues) {
			final var node = (Node) resultSet[0];
			final var vo = nodes.computeIfAbsent(node.getId(), id -> {
				// Build the first encountered parameter for this node
				entities.put(id, node);
				return toVoParameter(node, locator);
			});

			// Copy the parameter value if present
			Optional.ofNullable((ParameterValue) resultSet[1]).ifPresent(v -> vo.getParameters()
					.put(v.getParameter().getId(), ParameterValueHelper.parseValue(v, new ParameterValueVo())));
		}

		// Complete the hierarchy
		entities.entrySet().stream().filter(entry -> entry.getValue().isRefining()).forEach(entry -> {
			// Complete the hierarchy for this node
			final var node = nodes.get(entry.getKey());
			final var parent = nodes.get(entry.getValue().getRefined().getId());
			node.setRefined(parent);
			node.getParameters().putAll(parent.getParameters());
		});
		return nodes;
	}

}
