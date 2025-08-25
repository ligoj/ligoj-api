/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.node;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.ObjectUtils;
import org.ligoj.app.api.NodeVo;
import org.ligoj.app.model.Node;
import org.ligoj.app.model.Parameter;
import org.ligoj.app.model.ParameterType;
import org.ligoj.app.model.ParameterValue;
import org.ligoj.app.resource.ServicePluginLocator;
import org.ligoj.bootstrap.core.NamedBean;
import org.ligoj.bootstrap.core.resource.TechnicalException;
import org.springframework.data.domain.Persistable;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * {@link Node} helper resource.
 */
public class NodeHelper {

	/**
	 * Utility class
	 */
	private NodeHelper() {}

	private static final TypeReference<List<Integer>> LIST_INTEGER_TYPE = new TypeReference<>() {
		// Nothing to do
	};

	private static final TypeReference<List<String>> LIST_STRING_TYPE = new TypeReference<>() {
		// Nothing to do
	};
	private static final TypeReference<Map<String, Integer>> MAP_STRING_TYPE = new TypeReference<>() {
		// Nothing to do
	};
	private static final TypeReference<Map<String, String>> MAP_STRING_STRING_TYPE = new TypeReference<>() {
		// Nothing to do
	};

	/**
	 * A mapper configuration to parse string to parameter value.
	 */
	private static final Map<ParameterType, ParameterValueMapper<?>> TO_VALUE = new EnumMap<>(ParameterType.class);

	private record ParameterValueMapper<X>(
			BiConsumer<BasicParameterValueVo, X> setter,
			Function<String, X> toValue) {
	}

	static {

		// To value mapping
		TO_VALUE.put(ParameterType.BOOL, new ParameterValueMapper<>(BasicParameterValueVo::setBool, Boolean::valueOf));
		TO_VALUE.put(ParameterType.DATE, new ParameterValueMapper<>(BasicParameterValueVo::setDate, s -> new Date(Long.parseLong(s))));
		TO_VALUE.put(ParameterType.INTEGER, new ParameterValueMapper<>(BasicParameterValueVo::setInteger, Integer::valueOf));
		TO_VALUE.put(ParameterType.MULTIPLE, new ParameterValueMapper<>(BasicParameterValueVo::setSelections, NodeHelper::toListInteger));
		TO_VALUE.put(ParameterType.SELECT, new ParameterValueMapper<>(BasicParameterValueVo::setIndex, Integer::valueOf));
		TO_VALUE.put(ParameterType.TAGS, new ParameterValueMapper<>(BasicParameterValueVo::setTags, NodeHelper::toListString));
		TO_VALUE.put(ParameterType.TEXT, new ParameterValueMapper<>(BasicParameterValueVo::setText, Function.identity()));
	}

	/**
	 * Standard mapper used to read parameter configurations.
	 */
	private static final ObjectMapper MAPPER = new ObjectMapper();

	/**
	 * Return a list of {@link Integer} from a raw JSON string.
	 *
	 * @param json The raw JSON string.
	 * @return The not <code>null</code> list.
	 */
	public static List<Integer> toListInteger(final String json) {
		return toConfiguration(json, LIST_INTEGER_TYPE);
	}

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
					.put(v.getParameter().getId(), NodeHelper.parseValue(v, new ParameterValueVo())));
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


	/**
	 * Return a list of {@link String} from a raw JSON string.
	 *
	 * @param json The raw JSON string.
	 * @return The not <code>null</code> list.
	 */
	public static List<String> toListString(final String json) {
		return toConfiguration(json, LIST_STRING_TYPE);
	}

	/**
	 * Return a Map of {@link Integer} as value from a raw JSON string.
	 *
	 * @param json The raw JSON string.
	 * @return The not <code>null</code> Map.
	 */
	public static Map<String, Integer> toMapInteger(final String json) {
		return toConfiguration(json, MAP_STRING_TYPE);
	}

	/**
	 * Return a Map of {@link String} as value from a raw JSON string.
	 *
	 * @param json The raw JSON string.
	 * @return The not <code>null</code> Map.
	 */
	public static Map<String, String> toMapString(final String json) {
		return toConfiguration(json, MAP_STRING_STRING_TYPE);
	}

	/**
	 * Managed JSON writer
	 *
	 * @param any Any object to serialize.
	 * @return The JSON string from an object.
	 */
	public static String toJSon(final Object any) {
		try {
			return MAPPER.writeValueAsString(any);
		} catch (final JsonProcessingException e) {
			throw new TechnicalException("Unable to build JSon data from bean " + any, e);
		}
	}

	/**
	 * Build parameter configuration from the string definition.
	 *
	 * @param content      The content JSON string configuration. May be <code>null</code>.
	 * @param valueTypeRef The type reference to fix the return type.
	 * @param <T>          The return type.
	 * @return The parameter configuration.
	 */
	public static <T> T toConfiguration(final String content, final TypeReference<T> valueTypeRef) {
		try {
			return MAPPER.readValue(ObjectUtils.getIfNull(content, "{}"), valueTypeRef);
		} catch (final IOException e) {
			throw new TechnicalException("Unable to build configuration from " + content, e);
		}
	}

	/**
	 * {@link Parameter} JPA to {@link ParameterVo} transformer.
	 *
	 * @param entity The source JPA entity to convert.
	 * @return The VO with all attributes : full node reference, and definition.
	 */
	public static ParameterVo toVo(final Parameter entity) {
		final var vo = new ParameterVo();
		// Copy basic data
		vo.setId(entity.getId());
		vo.setType(entity.getType());
		vo.setMandatory(entity.isMandatory());
		vo.setSecured(entity.isSecured());
		vo.setOwner(NodeHelper.toVo(entity.getOwner()));
		vo.setDefaultValue(entity.getDefaultValue());
		vo.setDepends(entity.getDepends().stream().map(Persistable::getId).collect(Collectors.toSet()));

		// Map constraint data
		if (entity.getType().isArray()) {
			vo.setValues(toConfiguration(entity.getData(), LIST_STRING_TYPE));
		} else if (entity.getType() == ParameterType.INTEGER) {
			final var minMax = toConfiguration(entity.getData(), MAP_STRING_TYPE);
			vo.setMax(minMax.get("max"));
			vo.setMin(minMax.get("min"));
		}
		return vo;
	}

	/**
	 * Parse the raw data to the target type and return this value.
	 *
	 * @param entity {@link ParameterValue} to be parsed.
	 * @param vo     Target object receiving the typed value.
	 * @param <T>    The object type resolved during the parsing.
	 * @return the parsed and typed value.
	 */
	public static <T> T parseValue(final ParameterValue entity, final BasicParameterValueVo vo) {
		@SuppressWarnings("unchecked") final var valueMapper = (ParameterValueMapper<T>) TO_VALUE.get(entity.getParameter().getType());
		final var parsedValue = valueMapper.toValue.apply(entity.getData());
		valueMapper.setter.accept(vo, parsedValue);
		return parsedValue;
	}

	/**
	 * Transform {@link List} to {@link Map} where K is the item's identifier, and VALUE is the original item.
	 *
	 * @param items The items list.
	 * @param <K>   The entity's identifier type.
	 * @param <V>   The entity type.
	 * @return the corresponding map.
	 */
	public static <K extends Serializable, V extends Persistable<K>> Map<K, V> toMap(final Iterable<V> items) {
		final var result = new LinkedHashMap<K, V>();
		items.forEach(item -> result.put(item.getId(), item));
		return result;
	}
}
