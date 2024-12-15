/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.node;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.ObjectUtils;
import org.ligoj.app.model.Parameter;
import org.ligoj.app.model.ParameterType;
import org.ligoj.bootstrap.core.resource.TechnicalException;
import org.springframework.data.domain.Persistable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Criteria values Business Layer for entity {@link Parameter}
 */
@Component
public class ParameterHelper {

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
			return MAPPER.readValue(ObjectUtils.defaultIfNull(content, "{}"), valueTypeRef);
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

}
