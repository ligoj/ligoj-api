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
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CacheRemove;
import javax.cache.annotation.CacheResult;
import javax.persistence.EntityNotFoundException;
import javax.transaction.Transactional;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.ligoj.app.dao.NodeRepository;
import org.ligoj.app.dao.ParameterRepository;
import org.ligoj.app.dao.ParameterValueRepository;
import org.ligoj.app.dao.SubscriptionRepository;
import org.ligoj.app.iam.IamProvider;
import org.ligoj.app.iam.SimpleUserOrg;
import org.ligoj.app.model.Node;
import org.ligoj.app.model.Parameter;
import org.ligoj.app.model.ParameterType;
import org.ligoj.app.model.ParameterValue;
import org.ligoj.bootstrap.core.crypto.CryptoHelper;
import org.ligoj.bootstrap.core.resource.BusinessException;
import org.ligoj.bootstrap.core.security.SecurityHelper;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Persistable;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;

/**
 * Criteria values Business Layer for entity {@link ParameterValue}
 */
@Service
@Transactional
@Produces(MediaType.APPLICATION_JSON)
@Path("/node/parameter-value")
public class ParameterValueResource {

	@Autowired
	private ParameterValueRepository repository;

	@Autowired
	private ParameterRepository parameterRepository;

	@Autowired
	private SubscriptionRepository susbcriptionRepository;

	@Autowired
	private NodeRepository nodeRepository;

	@Autowired
	private ParameterResource parameterResource;

	@Autowired
	private SecurityHelper securityHelper;

	@Autowired
	private CryptoHelper cryptoHelper;

	@Autowired
	private IamProvider[] iamProvider;

	@AllArgsConstructor
	private static class ParameterValueMapper<X> {
		private final BiConsumer<ParameterValueVo, X> setter;
		private final Function<String, X> toValue;
	}

	/**
	 * A mapper configuration to parse parameter value to string.
	 */
	private static final Map<Function<ParameterValueCreateVo, Object>, Function<Object, String>> TO_STRING = new HashMap<>();

	/**
	 * A mapper configuration to parse string to parameter value.
	 */
	private static final Map<ParameterType, ParameterValueMapper<?>> TO_VALUE = new EnumMap<>(ParameterType.class);
	static {

		// To value mapping
		TO_VALUE.put(ParameterType.BINARY, new ParameterValueMapper<>(ParameterValueVo::setBinary, Boolean::valueOf));
		TO_VALUE.put(ParameterType.DATE, new ParameterValueMapper<>(ParameterValueVo::setDate, s -> new Date(Long.parseLong(s))));
		TO_VALUE.put(ParameterType.INTEGER, new ParameterValueMapper<>(ParameterValueVo::setInteger, Integer::valueOf));
		TO_VALUE.put(ParameterType.MULTIPLE, new ParameterValueMapper<>(ParameterValueVo::setSelections, ParameterResource::toListInteger));
		TO_VALUE.put(ParameterType.SELECT, new ParameterValueMapper<>(ParameterValueVo::setIndex, Integer::valueOf));
		TO_VALUE.put(ParameterType.TAGS, new ParameterValueMapper<>(ParameterValueVo::setTags, ParameterResource::toListString));
		TO_VALUE.put(ParameterType.TEXT, new ParameterValueMapper<>(ParameterValueVo::setText, Function.identity()));

		// To String mapping
		TO_STRING.put(ParameterValueCreateVo::getBinary, Object::toString);
		TO_STRING.put(ParameterValueCreateVo::getDate, o -> String.valueOf(((Date) o).getTime()));
		TO_STRING.put(ParameterValueCreateVo::getIndex, Object::toString);
		TO_STRING.put(ParameterValueCreateVo::getInteger, Object::toString);
		TO_STRING.put(ParameterValueCreateVo::getTags, o -> ParameterResource.toJSon(o).toUpperCase(Locale.ENGLISH));
		TO_STRING.put(ParameterValueCreateVo::getSelections, ParameterResource::toJSon);
	}

	/**
	 * A checker configuration to check a value against the contract of the
	 * parameter.
	 */
	private final Map<ParameterType, BiConsumer<ParameterValueCreateVo, Parameter>> typeToChecker = new EnumMap<>(ParameterType.class);

	/**
	 * Default constructor initializing the type mappings.
	 */
	public ParameterValueResource() {
		typeToChecker.put(ParameterType.BINARY, (b, p) -> ValidationJsonException.assertNotnull(b.getBinary(), p.getId()));
		typeToChecker.put(ParameterType.DATE, (b, p) -> {
			ValidationJsonException.assertNotnull(b.getDate(), p.getId());
			ValidationJsonException.assertTrue(b.getDate().getTime() > 0, p.getId());
		});
		typeToChecker.put(ParameterType.INTEGER, this::checkInteger);
		typeToChecker.put(ParameterType.SELECT, this::checkSelect);
		typeToChecker.put(ParameterType.MULTIPLE, this::checkMultiple);
		typeToChecker.put(ParameterType.TAGS, (b, p) -> checkTags(b));
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
		vo.copyAuditData(entity, (Function<String, SimpleUserOrg>) iamProvider[0].getConfiguration().getUserRepository()::toUser);
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
	public static <T> T parseValue(final ParameterValue entity, final ParameterValueVo vo) {
		@SuppressWarnings("unchecked")
		final ParameterValueMapper<T> valueMapper = (ParameterValueMapper<T>) TO_VALUE.get(entity.getParameter().getType());
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
	public static String toData(final ParameterValueCreateVo vo) {
		return TO_STRING.entrySet().stream().filter(e -> e.getKey().apply(vo) != null).findFirst()
				.map(e -> e.getValue().apply(e.getKey().apply(vo))).orElse(vo.getText());
	}

	/**
	 * Check optional but secure assertions.
	 */
	private void checkCompletude(final ParameterValueCreateVo vo) {
		Arrays.stream(
				new Supplier<?>[] { vo::getText, vo::getBinary, vo::getDate, vo::getIndex, vo::getInteger, vo::getTags, vo::getSelections })
				.map(Supplier::get).filter(Objects::nonNull).skip(1).findFirst().ifPresent(e -> {
					final ValidationJsonException exception = new ValidationJsonException();
					exception.addError(vo.getParameter(), "Too many values");
					throw exception;
				});
	}

	/**
	 * Check the data constraints and return the associated parameter
	 * definition.
	 */
	private void checkConstraints(final ParameterValueCreateVo vo, final Parameter parameter) {
		typeToChecker.get(parameter.getType()).accept(vo, parameter);
		checkCompletude(vo);
	}

	/**
	 * Check tags
	 */
	private void checkTags(final ParameterValueCreateVo vo) {
		ValidationJsonException.assertNotnull(vo.getTags(), vo.getParameter());
		vo.getTags().forEach(tag -> ValidationJsonException.assertTrue(StringUtils.isNotBlank(tag), "NotBlank", vo.getParameter()));
	}

	/**
	 * Check multiple selection
	 */
	private void checkMultiple(final ParameterValueCreateVo vo, final Parameter parameter) {
		ValidationJsonException.assertNotnull(vo.getSelections(), parameter.getId());
		final List<String> multiple = ParameterResource.toListString(parameter.getData());

		// Check each index
		vo.getSelections().forEach(i -> checkArrayBound(i, multiple.size(), parameter));
	}

	/**
	 * Check simple selection
	 */
	private void checkSelect(final ParameterValueCreateVo vo, final Parameter parameter) {
		ValidationJsonException.assertNotnull(vo.getIndex(), vo.getParameter());
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
		ValidationJsonException.assertTrue(value >= min, Min.class.getName(), parameter.getId(), min);
	}

	/**
	 * Check the bounds
	 */
	private void checkMax(final int value, final int max, final Persistable<String> parameter) {
		ValidationJsonException.assertTrue(value <= max, Max.class.getName(), parameter.getId(), max);
	}

	/**
	 * Check integer
	 */
	private void checkInteger(final ParameterValueCreateVo vo, final Parameter parameter) {
		ValidationJsonException.assertNotnull(vo.getInteger(), parameter.getId());
		final Map<String, Integer> minMax = ParameterResource.toMapInteger(parameter.getData());
		// Check minimal value
		if (minMax.get("max") != null) {
			checkMax(vo.getInteger(), minMax.get("max"), parameter);
		}
		// Check maximal value
		if (minMax.get("min") != null) {
			checkMin(vo.getInteger(), minMax.get("min"), parameter);
		}
	}

	/**
	 * Check text
	 */
	private void checkText(final ParameterValueCreateVo vo, final Parameter parameter) {
		// Check the value if not empty
		if (StringUtils.isNotBlank(vo.getText()) && StringUtils.isNotBlank(parameter.getData())) {
			// Check the pattern if present
			final Map<String, String> stringProperties = ParameterResource.toMapString(parameter.getData());
			final String patternString = stringProperties.get("pattern");
			if (StringUtils.isNotBlank(patternString)) {
				// Pattern is provided, check the string
				final Pattern pattern = Pattern.compile(patternString);
				ValidationJsonException.assertTrue(pattern.matcher(vo.getText()).matches(),
						javax.validation.constraints.Pattern.class.getName(), parameter.getId(), pattern.pattern());
			}
		}
	}

	/**
	 * Delete a {@link ParameterValue}. A value can be deleted only where there
	 * is no subscription on the related node, or the related parameter is not
	 * mandatory.
	 * 
	 * @param id
	 *            The entity's identifier.
	 */
	@DELETE
	public void delete(final int id) {
		// Check the value exist and related to a visible writable node
		final ParameterValue value = findOneExpected(id);

		// Check the visible node can also be edited
		checkWritableNode(value.getNode().getId());

		// A mandatory parameter can be deleted only when there is no
		// subscription to the same node
		if (value.getParameter().isMandatory()) {
			checkUnusedValue(value.getId());
		}

		// Deletion can be performed
		repository.delete(id);
	}

	/**
	 * Update a {@link ParameterValue}. Visibility of value and related
	 * parameter is checked. Only value attached to a {@link Node} can be
	 * updated. The related node cannot be updated. A parameter used in a
	 * subscription cannot be updated.
	 * 
	 * @param vo
	 *            {@link ParameterValueCreateVo} to update. Identifier is
	 *            required.
	 */
	@PUT
	public void update(final ParameterValueNodeUpdateVo vo) {
		final ParameterValue value = findOneExpected(vo.getId());
		checkWritableNode(value.getNode().getId());
		checkUnusedValue(value.getId());
		saveOrUpdateInternal(vo, parameterResource.findByIdInternal(vo.getParameter()), value);
		repository.saveAndFlush(value);
	}

	/**
	 * Create a new {@link ParameterValue} to a node. The related node must be
	 * visible and writable for the current user.
	 * 
	 * @param vo
	 *            new {@link ParameterValueCreateVo} to persist.
	 * @return The new identifier.
	 */
	@POST
	public int create(final ParameterValueNodeVo vo) {
		final ParameterValue value = createInternal(vo, parameterResource.findByIdInternal(vo.getParameter()));
		value.setNode(checkWritableNode(vo.getNode()));
		repository.saveAndFlush(value);
		return value.getId();
	}

	/**
	 * Return a visible {@link ParameterValue} for the current user. Only values
	 * associated to a node are considered as valid values.
	 * 
	 * @param id
	 *            The entity's identifier.
	 * @return The visible {@link ParameterValue}.
	 */
	private ParameterValue findOneExpected(final int id) {
		return Optional.ofNullable(repository.findOneVisible(id, securityHelper.getLogin())).orElseThrow(EntityNotFoundException::new);
	}

	/**
	 * Check the parameter value is not used in a subscription.
	 */
	private void checkUnusedValue(final int value) {
		if (susbcriptionRepository.countByParameterValue(value) > 0) {
			throw new BusinessException("used-parameter-value", "parameter-value", value);
		}
	}

	/**
	 * Check the related node can be updated by the current user.
	 */
	private Node checkWritableNode(final String id) {
		final Node node = nodeRepository.findOneWritable(id, securityHelper.getLogin());
		if (node == null) {
			// Node is not readable
			throw new BusinessException("read-only-node", "node", id);
		}
		return node;
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
		return saveOrUpdateInternal(vo, parameter, new ParameterValue());
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
	private ParameterValue saveOrUpdateInternal(final ParameterValueCreateVo vo, final Parameter parameter, final ParameterValue value) {
		checkConstraints(vo, parameter);
		checkCompletude(vo);

		value.setData(toData(vo));
		value.setParameter(parameter);

		// Encrypt the data as needed
		if (parameter.isSecured()) {
			// Data need to be encrypted
			value.setData(cryptoHelper.encryptAsNeeded(value.getData()));
		}

		return value;
	}

	/**
	 * Return non secured parameters values related to the subscription.
	 * 
	 * @param subscription
	 *            The subscription identifier.
	 * @return secured associated parameters values. Key of returned map is the
	 *         identifier of {@link org.ligoj.app.model.Parameter}
	 */
	public Map<String, String> getNonSecuredSubscriptionParameters(final int subscription) {
		return toMapValues(repository.findAllSecureBySubscription(subscription));
	}

	/**
	 * Return all parameters values related to the subscription. Secured
	 * (encrypted) are parameters are decrypted.
	 * 
	 * @param subscription
	 *            The subscription identifier.
	 * @return all associated parameters values. Key of returned map is the
	 *         identifier of {@link org.ligoj.app.model.Parameter}
	 */
	@CacheResult(cacheName = "subscription-parameters")
	public Map<String, String> getSubscriptionParameters(@CacheKey final int subscription) {
		return toMapValues(repository.findAllBySubscription(subscription));
	}

	/**
	 * Create given parameter values.
	 * 
	 * @param values
	 *            the parameter values to persist.
	 * @param presave
	 *            The operation to perform before persisting.
	 */
	public void create(final List<ParameterValueCreateVo> values, final Consumer<ParameterValue> presave) {
		// Persist each not null parameter
		values.stream().map(this::createInternal).filter(v -> StringUtils.isNotBlank(v.getData())).forEach(v -> {
			// Link this value to the subscription
			presave.accept(v);
			repository.saveAndFlush(v);
		});
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
	 * Transform {@link List} to {@link Map} where key is the parameter name.
	 * Secured parameters are decrypted.
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
	 * Transform {@link List} to {@link Map} where K is the item's identifier,
	 * and VALUE is the original item.
	 * 
	 * @param items
	 *            The items list.
	 * @return the corresponding map.
	 */
	public <K extends Serializable, V extends Persistable<K>> Map<K, V> toMap(final Iterable<V> items) {
		final Map<K, V> result = new LinkedHashMap<>();
		items.forEach(item -> result.put(item.getId(), item));
		return result;
	}
}
