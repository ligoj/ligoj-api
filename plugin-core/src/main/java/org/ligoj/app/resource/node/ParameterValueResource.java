/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.node;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CacheRemove;
import javax.cache.annotation.CacheResult;
import javax.persistence.EntityNotFoundException;
import javax.transaction.Transactional;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.ligoj.app.api.SubscriptionMode;
import org.ligoj.app.dao.ParameterRepository;
import org.ligoj.app.dao.ParameterValueRepository;
import org.ligoj.app.dao.SubscriptionRepository;
import org.ligoj.app.iam.IamProvider;
import org.ligoj.app.iam.SimpleUserOrg;
import org.ligoj.app.model.Node;
import org.ligoj.app.model.Parameter;
import org.ligoj.app.model.ParameterType;
import org.ligoj.app.model.ParameterValue;
import org.ligoj.app.model.Subscription;
import org.ligoj.bootstrap.core.crypto.CryptoHelper;
import org.ligoj.bootstrap.core.resource.BusinessException;
import org.ligoj.bootstrap.core.security.SecurityHelper;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Persistable;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;

/**
 * Criteria values Business Layer for entity {@link ParameterValue}
 */
@Service
@Transactional
@Produces(MediaType.APPLICATION_JSON)
@Path("/node")
public class ParameterValueResource {

	/**
	 * A mapper configuration to parse parameter value to string.
	 */
	private static final Map<Function<BasicParameterValueVo, Object>, Function<Object, String>> TO_STRING = new HashMap<>();

	/**
	 * A mapper configuration to parse string to parameter value.
	 */
	private static final Map<ParameterType, ParameterValueMapper<?>> TO_VALUE = new EnumMap<>(ParameterType.class);

	/**
	 * A checker configuration to check a value against the contract of the parameter.
	 */
	private final Map<ParameterType, BiConsumer<BasicParameterValueVo, Parameter>> typeToChecker = new EnumMap<>(
			ParameterType.class);

	@Autowired
	private ParameterValueRepository repository;

	@Autowired
	private ParameterRepository parameterRepository;

	@Autowired
	private SubscriptionRepository susbcriptionRepository;

	@Autowired
	private NodeResource nodeResource;

	@Autowired
	private ParameterResource parameterResource;

	@Autowired
	private SecurityHelper securityHelper;

	@Autowired
	private CryptoHelper cryptoHelper;

	@Autowired
	private IamProvider[] iamProvider;

	@Autowired
	private CacheManager cacheManager;

	@AllArgsConstructor
	private static class ParameterValueMapper<X> {
		private final BiConsumer<BasicParameterValueVo, X> setter;
		private final Function<String, X> toValue;
	}

	static {

		// To value mapping
		TO_VALUE.put(ParameterType.BOOL, new ParameterValueMapper<>(BasicParameterValueVo::setBool, Boolean::valueOf));
		TO_VALUE.put(ParameterType.DATE,
				new ParameterValueMapper<>(BasicParameterValueVo::setDate, s -> new Date(Long.parseLong(s))));
		TO_VALUE.put(ParameterType.INTEGER,
				new ParameterValueMapper<>(BasicParameterValueVo::setInteger, Integer::valueOf));
		TO_VALUE.put(ParameterType.MULTIPLE,
				new ParameterValueMapper<>(BasicParameterValueVo::setSelections, ParameterResource::toListInteger));
		TO_VALUE.put(ParameterType.SELECT,
				new ParameterValueMapper<>(BasicParameterValueVo::setIndex, Integer::valueOf));
		TO_VALUE.put(ParameterType.TAGS,
				new ParameterValueMapper<>(BasicParameterValueVo::setTags, ParameterResource::toListString));
		TO_VALUE.put(ParameterType.TEXT,
				new ParameterValueMapper<>(BasicParameterValueVo::setText, Function.identity()));

		// To String mapping
		TO_STRING.put(BasicParameterValueVo::getBool, Object::toString);
		TO_STRING.put(BasicParameterValueVo::getDate, o -> String.valueOf(((Date) o).getTime()));
		TO_STRING.put(BasicParameterValueVo::getIndex, Object::toString);
		TO_STRING.put(BasicParameterValueVo::getInteger, Object::toString);
		TO_STRING.put(BasicParameterValueVo::getTags, o -> ParameterResource.toJSon(o).toUpperCase(Locale.ENGLISH));
		TO_STRING.put(BasicParameterValueVo::getSelections, ParameterResource::toJSon);
	}

	/**
	 * Default constructor initializing the type mappings.
	 */
	public ParameterValueResource() {
		typeToChecker.put(ParameterType.BOOL, (b, p) -> assertNotnull(b.getBool(), p.getId()));
		typeToChecker.put(ParameterType.DATE, (b, p) -> {
			assertNotnull(b.getDate(), p.getId());
			assertTrue(b.getDate().getTime() > 0, p.getId(), "Min", 0);
		});
		typeToChecker.put(ParameterType.INTEGER, this::checkInteger);
		typeToChecker.put(ParameterType.SELECT, this::checkSelect);
		typeToChecker.put(ParameterType.MULTIPLE, this::checkMultiple);
		typeToChecker.put(ParameterType.TAGS, this::checkTags);
		typeToChecker.put(ParameterType.TEXT, this::checkText);
	}

	/**
	 * {@link ParameterValue} JPA to business object transformer.
	 * 
	 * @param entity
	 *            The parameter value.
	 * @return The parameter value with parameter definition.
	 */
	public ParameterValueVo toVo(final ParameterValue entity) {
		final ParameterValueVo vo = new ParameterValueVo();
		vo.copyAuditData(entity,
				(Function<String, SimpleUserOrg>) iamProvider[0].getConfiguration().getUserRepository()::toUser);
		vo.setId(entity.getId());
		vo.setParameter(ParameterResource.toVo(entity.getParameter()));

		// Map node
		if (entity.getNode() != null) {
			vo.setNode(NodeResource.toVo(entity.getNode()));
		}

		// Map criteria value
		parseValue(entity, vo);
		return vo;
	}

	/**
	 * Parse the raw data to the target type and return this value.
	 * 
	 * @param entity
	 *            {@link ParameterValue} to be parsed.
	 * @param vo
	 *            Target object receiving the typed value.
	 * @return the parsed and typed value.
	 * @param <T>
	 *            The object type resolved during the parsing.
	 */
	public static <T> T parseValue(final ParameterValue entity, final BasicParameterValueVo vo) {
		@SuppressWarnings("unchecked")
		final ParameterValueMapper<T> valueMapper = (ParameterValueMapper<T>) TO_VALUE
				.get(entity.getParameter().getType());
		final T parsedValue = valueMapper.toValue.apply(entity.getData());
		valueMapper.setter.accept(vo, parsedValue);
		return parsedValue;
	}

	/**
	 * Return the data String from the true data value.
	 * 
	 * @param vo
	 *            The object to convert.
	 * @return The String data to persist.
	 */
	public static String toData(final BasicParameterValueVo vo) {
		return StringUtils.trimToNull(TO_STRING.entrySet().stream().filter(e -> e.getKey().apply(vo) != null)
				.findFirst().map(e -> e.getValue().apply(e.getKey().apply(vo))).orElse(vo.getText()));
	}

	/**
	 * Check optional but secure assertions.
	 */
	private void checkCompletude(final BasicParameterValueVo vo, final Parameter parameter) {
		Arrays.stream(new Supplier<?>[] { vo::getText, vo::getBool, vo::getDate, vo::getIndex, vo::getInteger,
				vo::getTags, vo::getSelections }).map(Supplier::get).filter(Objects::nonNull).skip(1).findFirst()
				.ifPresent(e -> {
					final ValidationJsonException exception = new ValidationJsonException();
					exception.addError(parameter.getId(), "Too many values");
					throw exception;
				});
	}

	/**
	 * Check the data constraints and return the associated parameter definition.
	 */
	private void checkConstraints(final BasicParameterValueVo vo, final Parameter parameter) {
		typeToChecker.get(parameter.getType()).accept(vo, parameter);
	}

	/**
	 * Check tags
	 */
	private void checkTags(final BasicParameterValueVo vo, final Parameter parameter) {
		assertNotnull(vo.getTags(), parameter.getId());
		vo.getTags().forEach(tag -> assertTrue(StringUtils.isNotBlank(tag), "NotBlank", parameter.getId()));
	}

	/**
	 * Check multiple selection
	 */
	private void checkMultiple(final BasicParameterValueVo vo, final Parameter parameter) {
		assertNotnull(vo.getSelections(), parameter.getId());
		final List<String> multiple = ParameterResource.toListString(parameter.getData());

		// Check each index
		vo.getSelections().forEach(i -> checkArrayBound(i, multiple.size(), parameter));
	}

	/**
	 * Check simple selection
	 */
	private void checkSelect(final BasicParameterValueVo vo, final Parameter parameter) {
		assertNotnull(vo.getIndex(), parameter.getId());
		final List<String> single = ParameterResource.toListString(parameter.getData());

		// Check the index
		checkArrayBound(vo.getIndex(), single.size(), parameter);
	}

	/**
	 * Check the bounds
	 */
	private void checkArrayBound(final int value, final int size, final Persistable<String> parameter) {
		checkMin(value, 0, parameter);
		checkMax(value, size - 1, parameter);
	}

	/**
	 * Check the bounds
	 */
	private void checkMin(final int value, final int min, final Persistable<String> parameter) {
		assertTrue(value >= min, Min.class.getName(), parameter.getId(), min);
	}

	/**
	 * Check the bounds
	 */
	private void checkMax(final int value, final int max, final Persistable<String> parameter) {
		assertTrue(value <= max, Max.class.getName(), parameter.getId(), max);
	}

	/**
	 * Check integer
	 */
	private void checkInteger(final BasicParameterValueVo vo, final Parameter parameter) {
		assertNotnull(vo.getInteger(), parameter.getId());
		final Map<String, Integer> minMax = ParameterResource.toMapInteger(parameter.getData());
		// Check minimal value
		Optional.ofNullable(minMax.get("max")).ifPresent(m -> checkMax(vo.getInteger(), m, parameter));

		// Check maximal value
		Optional.ofNullable(minMax.get("min")).ifPresent(m -> checkMin(vo.getInteger(), m, parameter));
	}

	/**
	 * Check text
	 */
	private void checkText(final BasicParameterValueVo vo, final Parameter parameter) {
		// Check the value if not empty
		if (StringUtils.isNotBlank(vo.getText()) && StringUtils.isNotBlank(parameter.getData())) {
			// Check the pattern if present
			final Map<String, String> stringProperties = ParameterResource.toMapString(parameter.getData());
			final String patternString = stringProperties.get("pattern");
			if (StringUtils.isNotBlank(patternString)) {
				// Pattern is provided, check the string
				final Pattern pattern = Pattern.compile(patternString);
				assertTrue(pattern.matcher(vo.getText()).matches(),
						javax.validation.constraints.Pattern.class.getSimpleName(), parameter.getId(), "regexp",
						pattern.pattern());
			}
		}
	}

	/**
	 * Check is <code>true</code>
	 */
	private void assertTrue(final boolean valid, final String error, final String property,
			final Serializable... args) {
		if (!valid) {
			throw new ValidationJsonException(property, error, args);
		}
	}

	/**
	 * Check not <code>null</code>
	 */
	private void assertNotnull(final Object value, final String property, final Serializable... args) {
		assertTrue(value != null, "NotNull", property, args);
	}

	/**
	 * Delete a {@link ParameterValue}. A value can be deleted only where there is no subscription on the related node,
	 * or the related parameter is not mandatory.
	 * 
	 * @param id
	 *            The entity's identifier.
	 */
	public void delete(final int id) {
		// Check the value exist and related to a visible writable node
		final ParameterValue value = findOneExpected(id);

		// Check the visible node can also be edited
		nodeResource.checkWritableNode(value.getNode().getId());

		// A mandatory parameter can be deleted only when there is no
		// subscription to the same node
		checkUnusedValue(value);

		// Deletion can be performed
		repository.deleteById(id);
	}

	/**
	 * Update the given node parameter values. The old not updated values are deleted.
	 * 
	 * @param values
	 *            the parameter values to persist.
	 * @param node
	 *            The related node.
	 */
	public void update(final List<ParameterValueCreateVo> values, final Node node) {
		// Build the old parameter values
		final List<ParameterValue> oldList = repository.getParameterValues(node.getId());
		final Map<String, ParameterValue> oldMap = oldList.stream()
				.collect(Collectors.toMap(v -> v.getParameter().getId(), Function.identity()));

		// Build the target parameter values
		final Set<String> newParam = values.stream().map(v -> saveOrUpdate(oldMap, v)).filter(Objects::nonNull)
				.peek(v -> v.setNode(node)).map(repository::saveAndFlush).map(v -> v.getParameter().getId())
				.collect(Collectors.toSet());

		// Delete the existing but not provided values
		CollectionUtils.removeAll(oldMap.keySet(), newParam).stream().map(oldMap::get).forEach(repository::delete);
		cacheManager.getCache("node-parameters").evict(node.getId());
	}

	/**
	 * Create the given subscription parameter values. Value validity is checked.
	 * 
	 * @param values
	 *            the parameter values to persist.
	 * @param subscription
	 *            The related subscription.
	 */
	public void create(final List<ParameterValueCreateVo> values, final Subscription subscription) {
		create(values, v -> v.setSubscription(subscription));
		cacheManager.getCache("subscription-parameters").evict(subscription.getId());
	}

	/**
	 * Create the given node parameter values.
	 * 
	 * @param values
	 *            the parameter values to persist.
	 * @param node
	 *            The related node.
	 */
	public void create(final List<ParameterValueCreateVo> values, final Node node) {
		create(values, v -> v.setNode(node));
		cacheManager.getCache("node-parameters").evict(node.getId());
	}

	private void create(final List<ParameterValueCreateVo> values, final Consumer<ParameterValue> presave) {
		// Persist each not blank parameter
		values.stream().map(this::createInternal).filter(Objects::nonNull).forEach(v -> {
			// Link this value to the subscription
			presave.accept(v);
			repository.saveAndFlush(v);
		});
	}

	/**
	 * Return a visible {@link ParameterValue} for the current user. Only values associated to a node are considered as
	 * valid values.
	 * 
	 * @param id
	 *            The entity's identifier.
	 * @return The visible {@link ParameterValue}.
	 */
	private ParameterValue findOneExpected(final int id) {
		return Optional.ofNullable(repository.findOneVisible(id, securityHelper.getLogin()))
				.orElseThrow(EntityNotFoundException::new);
	}

	/**
	 * Check the parameter value is not used in a subscription when the target value becomes <code>null</code> or empty.
	 * 
	 * @param entity
	 *            The {@link ParameterValue} entity to check.
	 */
	private void checkUnusedValue(final ParameterValue entity) {
		if (entity.getParameter().isMandatory()) {
			final int nb = susbcriptionRepository.countByParameterValue(entity.getId());
			if (nb > 0) {
				throw new ValidationJsonException(entity.getParameter().getId(), "used-parameter-value",
						"subscriptions", nb);
			}
		}
	}

	/**
	 * Check transition and convert to ParameterValue.
	 * 
	 * @param vo
	 *            new {@link ParameterValueCreateVo} to persist.
	 * @return corresponding entity.
	 */
	protected ParameterValue createInternal(final ParameterValueCreateVo vo) {
		return createInternal(vo, parameterRepository.findOneExpected(vo.getParameter()));
	}

	/**
	 * Check transition and convert to entity.
	 * 
	 * @param vo
	 *            new {@link ParameterValueCreateVo} to persist.
	 * @param parameter
	 *            The resolved parameter related to the {@link ParameterValue}
	 * @return corresponding entity.
	 */
	private ParameterValue createInternal(final ParameterValueCreateVo vo, final Parameter parameter) {
		return checkSaveOrUpdate(vo, parameter, new ParameterValue());
	}

	/**
	 * Check transition and convert to entity.
	 * 
	 * @param vo
	 *            new {@link ParameterValueCreateVo} to be merged into the entity.
	 * @param parameter
	 *            The resolved parameter related to the {@link ParameterValue}
	 * @param entity
	 *            The entity to update.
	 * @return corresponding entity when accepted for update. <code>null</code> when all constraints are checked, but
	 *         the target operation should be a deletion because of the empty value.
	 */
	private ParameterValue checkSaveOrUpdate(final ParameterValueCreateVo vo, final Parameter parameter,
			final ParameterValue entity) {
		checkConstraints(vo, parameter);
		checkCompletude(vo, parameter);

		entity.setData(toData(vo));
		entity.setParameter(parameter);

		// Handle the target empty data
		if (StringUtils.isBlank(entity.getData())) {
			// A blank parameter value must be deleted
			return null;
		}

		// Encrypt the data as needed
		if (parameter.isSecured()) {
			// Data need to be encrypted
			entity.setData(cryptoHelper.encryptAsNeeded(entity.getData()));
		}

		return entity;
	}

	/**
	 * Check the parameter is related to the given node.
	 * 
	 * @param parameter
	 *            The parameter to check.
	 * @param node
	 *            The node scope the parameter must be related to.
	 */
	public void checkOwnership(final Parameter parameter, final Node node) {
		if (!equalsOrParentOf(parameter.getOwner(), node)) {
			// This parameter is detached from the node's hierarchy
			throw new BusinessException("invalid-parameter-node-ownership", "parameter", parameter.getId(), "node",
					node.getId());
		}
	}

	private boolean equalsOrParentOf(final Node parent, final Node node) {
		return node.equals(parent) || node.isRefining() && equalsOrParentOf(parent, node.getRefined());
	}

	/**
	 * Return non secured parameters values related to the subscription. Secured parameters are not returned.
	 * 
	 * @param subscription
	 *            The subscription identifier.
	 * @return secured associated parameters values. Key of returned map is the identifier of
	 *         {@link org.ligoj.app.model.Parameter}
	 */
	public Map<String, String> getNonSecuredSubscriptionParameters(final int subscription) {
		return toMapValues(repository.findAllSecureBySubscription(subscription));
	}

	/**
	 * Return all parameters values related to the subscription. Secured (encrypted) parameters are decrypted.
	 * 
	 * @param subscription
	 *            The subscription identifier.
	 * @return all associated parameters values. Key of returned map is the identifier of
	 *         {@link org.ligoj.app.model.Parameter}
	 */
	@CacheResult(cacheName = "subscription-parameters")
	public Map<String, String> getSubscriptionParameters(@CacheKey final int subscription) {
		return toMapValues(repository.findAllBySubscription(subscription));
	}

	private ParameterValue saveOrUpdate(final Map<String, ParameterValue> existing,
			final ParameterValueCreateVo value) {
		if (value.isUntouched()) {
			// Untouched value, keep the previous value but must exists
			return Optional.ofNullable(existing.get(value.getParameter())).orElseThrow(
					() -> new BusinessException(BusinessException.KEY_UNKNOW_ID, "parameter", value.getParameter()));
		}

		// Updated or created value
		ParameterValue entity = existing.get(value.getParameter());
		if (entity == null) {
			// Need to parse and recreate the value
			return createInternal(value);
		}

		// Update mode
		return checkSaveOrUpdate(value, entity.getParameter(), entity);
	}

	/**
	 * Delete all parameter values associated to given subscription.
	 * 
	 * @param subscription
	 *            the associated subscription to delete.
	 */
	@CacheRemove(cacheName = "subscription-parameters")
	public void deleteBySubscription(@CacheKey final int subscription) {
		repository.deleteAllBy("subscription.id", subscription);
	}

	/**
	 * Delete all parameter values associated to given node. This includes the related subscriptions parameter values.
	 * 
	 * @param node
	 *            The parent node.
	 */
	@CacheRemove(cacheName = "node-parameters")
	public void deleteByNode(@CacheKey final String node) {
		repository.deleteByNode(node);
	}

	/**
	 * Transform {@link List} to {@link Map} where key is the parameter name. Secured parameters are decrypted.
	 * 
	 * @param values
	 *            The parameters list.
	 * @return the corresponding key/values. Never <code>null</code>.
	 */
	public Map<String, String> toMapValues(final List<ParameterValue> values) {
		final Map<String, String> result = new HashMap<>();
		for (final ParameterValue value : values) {
			String data;
			if (value.getParameter().isSecured()) {
				// Value may be encrypted
				data = cryptoHelper.decryptAsNeeded(value.getData());
			} else {
				data = value.getData();
			}

			// Trim the data to get only the relevant values
			data = StringUtils.trimToNull(data);
			if (data != null) {
				// Non empty value, can be stored
				result.put(value.getParameter().getId(), data);
			}
		}
		return result;
	}

	/**
	 * Transform {@link List} to {@link Map} where K is the item's identifier, and VALUE is the original item.
	 * 
	 * @param items
	 *            The items list.
	 * @param <K>
	 *            The entity's identifier type.
	 * @param <V>
	 *            The entity type.
	 * @return the corresponding map.
	 */
	public <K extends Serializable, V extends Persistable<K>> Map<K, V> toMap(final Iterable<V> items) {
		final Map<K, V> result = new LinkedHashMap<>();
		items.forEach(item -> result.put(item.getId(), item));
		return result;
	}

	/**
	 * Return the parameter values associated to the given node. Not exposed as web-service, contains secured data. The
	 * result is cached.
	 * 
	 * @param node
	 *            the node identifier.
	 * @return the parameters of given node as {@link Map}.
	 */
	@org.springframework.transaction.annotation.Transactional(readOnly = true)
	@CacheResult(cacheName = "node-parameters")
	public Map<String, String> getNodeParameters(@CacheKey final String node) {
		// Get parameters of given node
		return toMapValues(repository.getParameterValues(node));
	}

	/**
	 * Return all node parameter definitions where a value is expected to be provided to the final subscription. When
	 * defined, the current value is specified.
	 * 
	 * @param node
	 *            The node identifier.
	 * @param mode
	 *            Subscription mode.
	 * @return All parameter definitions where a value is expected to be attached to the final subscription in given
	 *         mode.
	 */
	@GET
	@Path("{node:service:.+}/parameter-value/{mode}")
	public List<ParameterNodeVo> getNodeParameters(@PathParam("node") final String node,
			@PathParam("mode") final SubscriptionMode mode) {
		final List<ParameterVo> parameters = parameterResource.getNotProvidedAndAssociatedParameters(node, mode);
		final Map<String, ParameterValue> vmap = repository.getParameterValues(node).stream()
				.collect(Collectors.toMap(v -> v.getParameter().getId(), Function.identity()));
		return parameters.stream().map(p -> {
			final ParameterNodeVo vo = new ParameterNodeVo();
			vo.setParameter(p);
			if (vmap.containsKey(p.getId())) {
				if (p.isSecured()) {
					// Secured parameter value is not returned
					vo.setText("-secured-");
				} else {
					// Return the parsed value
					parseValue(vmap.get(p.getId()), vo);
				}
			}
			return vo;
		}).collect(Collectors.toList());
	}
}
