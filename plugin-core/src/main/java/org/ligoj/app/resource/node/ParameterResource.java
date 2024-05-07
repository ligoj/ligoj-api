/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.node;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.apache.commons.lang3.ObjectUtils;
import org.ligoj.app.api.SubscriptionMode;
import org.ligoj.app.dao.ParameterRepository;
import org.ligoj.app.model.Parameter;
import org.ligoj.app.model.ParameterType;
import org.ligoj.bootstrap.core.model.AbstractBusinessEntity;
import org.ligoj.bootstrap.core.resource.TechnicalException;
import org.ligoj.bootstrap.core.security.SecurityHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Persistable;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Criteria values Business Layer for entity {@link Parameter}
 */
@Service
@Transactional
@Produces(MediaType.APPLICATION_JSON)
@Path("/node")
public class ParameterResource {

	/**
	 * Node parameter comparator.
	 */
	private static final DependencyComparator COMPARATOR = new DependencyComparator();

	@Autowired
	private ParameterRepository repository;

	@Autowired
	private SecurityHelper securityHelper;

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
		vo.setOwner(NodeResource.toVo(entity.getOwner()));
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
	 * Return a parameter attached to a visible node for the current user.
	 *
	 * @param id Parameter identifier.
	 * @return The parameter from its identifier. May be <code>null</code>.
	 */
	public Parameter findByIdInternal(final String id) {
		return Optional.ofNullable(repository.findOneVisible(id, securityHelper.getLogin()))
				.orElseThrow(EntityNotFoundException::new);
	}

	/**
	 * Return all node parameter definitions where a value is expected to be provided to the final subscription. The
	 * parameters are ordered by dependencies, root first.
	 *
	 * @param node The node identifier.
	 * @param mode Subscription mode.
	 * @return All parameter definitions where a value is expected to be attached to the final subscription in given
	 *         mode.
	 */
	@GET
	@Path("{node:service:.+}/parameter/{mode}")
	public List<ParameterVo> getNotProvidedParameters(@PathParam("node") final String node,
			@PathParam("mode") final SubscriptionMode mode) {
		// Build the parameters map
		final var parameters = new HashMap<String, ParameterVo>();
		repository.getOrphanParameters(node, mode, securityHelper.getLogin()).stream().map(ParameterResource::toVo)
				.forEach(v -> parameters.put(v.getId(), v));

		// Complete the dependencies graph
		var updated = true;
		while (updated) {
			updated = parameters.values().stream().anyMatch(p -> p.getDepends().addAll(p.getDepends().stream()
					.flatMap(d -> parameters.get(d).getDepends().stream()).collect(Collectors.toSet())));
		}
		final var clone = new ArrayList<>(parameters.values());
		clone.sort(Comparator.comparing(AbstractBusinessEntity::getId));
		clone.sort(COMPARATOR);
		return clone;
	}

	/**
	 * Return all node parameter definitions where a value is expected to be provided to the final subscription.
	 *
	 * @param node The node identifier.
	 * @param mode Subscription mode.
	 * @return All parameter definitions where a value is expected to be attached to the final subscription in given
	 *         mode.
	 */
	public List<ParameterVo> getNotProvidedAndAssociatedParameters(final String node, final SubscriptionMode mode) {
		return repository.getOrphanParametersExt(node, mode, securityHelper.getLogin()).stream()
				.map(ParameterResource::toVo).toList();
	}
}
