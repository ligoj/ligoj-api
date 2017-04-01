package org.ligoj.app.resource.node;

import java.io.IOException;
import java.io.Serializable;
import java.sql.Date;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CacheRemove;
import javax.cache.annotation.CacheResult;
import javax.transaction.Transactional;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.ligoj.app.dao.ParameterRepository;
import org.ligoj.app.dao.ParameterValueRepository;
import org.ligoj.app.iam.IamProvider;
import org.ligoj.app.iam.SimpleUserOrg;
import org.ligoj.app.model.Parameter;
import org.ligoj.app.model.ParameterType;
import org.ligoj.app.model.ParameterValue;
import org.ligoj.bootstrap.core.crypto.CryptoHelper;
import org.ligoj.bootstrap.core.resource.TechnicalException;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Persistable;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;

/**
 * Criteria values Business Layer for entity {@link ParameterValue}
 */
@Service
@Transactional
@Produces(MediaType.APPLICATION_JSON)
public class ParameterValueResource {

	@Autowired
	private ParameterRepository parameterRepository;

	@Autowired
	private ParameterValueRepository repository;

	@Autowired
	private CryptoHelper cryptoHelper;

	@Autowired
	private IamProvider iamProvider;

	private static final TypeReference<List<Integer>> LIST_INTEGER_TYPE = new TypeReference<List<Integer>>() {
		// Nothing to do
	};

	private static final TypeReference<List<String>> LIST_STRING_TYPE = new TypeReference<List<String>>() {
		// Nothing to do
	};
	private static final TypeReference<Map<String, Integer>> MAP_STRING_TYPE = new TypeReference<Map<String, Integer>>() {
		// Nothing to do
	};
	private static final TypeReference<Map<String, String>> MAP_STRING_STRING_TYPE = new TypeReference<Map<String, String>>() {
		// Nothing to do
	};

	@AllArgsConstructor
	private static class ParameterValueMapper<X> {
		private final BiConsumer<ParameterValueVo, X> setter;
		private final Function<String, X> toValue;
	}

	/**
	 * A mapper configuration to parse string to parameter value.
	 */
	private static final Map<ParameterType, ParameterValueMapper<?>> STRING_TO_VALUE = new EnumMap<>(ParameterType.class);
	static {
		STRING_TO_VALUE.put(ParameterType.BINARY, new ParameterValueMapper<>(ParameterValueVo::setBinary, Boolean::valueOf));
		STRING_TO_VALUE.put(ParameterType.DATE, new ParameterValueMapper<>(ParameterValueVo::setDate, s -> new Date(Long.parseLong(s))));
		STRING_TO_VALUE.put(ParameterType.INTEGER, new ParameterValueMapper<>(ParameterValueVo::setInteger, Integer::valueOf));
		STRING_TO_VALUE.put(ParameterType.MULTIPLE,
				new ParameterValueMapper<>(ParameterValueVo::setSelections, s -> toConfiguration(s, LIST_INTEGER_TYPE)));
		STRING_TO_VALUE.put(ParameterType.SELECT, new ParameterValueMapper<>(ParameterValueVo::setIndex, Integer::valueOf));
		STRING_TO_VALUE.put(ParameterType.TAGS, new ParameterValueMapper<>(ParameterValueVo::setTags, s -> toConfiguration(s, LIST_STRING_TYPE)));
		STRING_TO_VALUE.put(ParameterType.TEXT, new ParameterValueMapper<>(ParameterValueVo::setText, Function.identity()));
	}

	/**
	 * Standard mapper used to read parameter configurations.
	 */
	private static final ObjectMapper MAPPER = new ObjectMapper();

	/**
	 * Build parameter configuration from the string definition.
	 */
	private static <T> T toConfiguration(final String content, final TypeReference<T> valueTypeRef) {
		try {
			return MAPPER.readValue(ObjectUtils.defaultIfNull(content, "{}"), valueTypeRef);
		} catch (final IOException e) {
			throw new TechnicalException("Unable to build configuration from " + content, e);
		}
	}

	/**
	 * {@link Parameter} JPA to {@link ParameterVo} transformer.
	 * 
	 * @param entity
	 *            The source JPA entity to convert.
	 * @return The VO with all attributes : full node reference, and definition.
	 */
	public static ParameterVo toVo(final Parameter entity) {
		final ParameterVo vo = new ParameterVo();
		vo.setId(entity.getId());
		vo.setType(entity.getType());
		vo.setMandatory(entity.isMandatory());

		// Map constraint data
		if (entity.getType().isArray()) {
			vo.setValues(toConfiguration(entity.getData(), LIST_STRING_TYPE));
		} else if (entity.getType() == ParameterType.INTEGER) {
			final Map<String, Integer> minMax = toConfiguration(entity.getData(), MAP_STRING_TYPE);
			vo.setMax(minMax.get("max"));
			vo.setMin(minMax.get("min"));
		}

		// Copy related beans
		vo.setOwner(NodeResource.toVo(entity.getOwner()));
		return vo;
	}

	/**
	 * A checker configuration to check a value against the contract of the parameter.
	 */
	private final Map<ParameterType, BiConsumer<ParameterValueEditionVo, Parameter>> typeToChecker = new EnumMap<>(ParameterType.class);
	{
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
		vo.copyAuditData(entity, (Function<String, SimpleUserOrg>) iamProvider.getConfiguration().getUserRepository()::toUser);
		vo.setId(entity.getId());
		vo.setParameter(toVo(entity.getParameter()));

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
		final ParameterValueMapper<T> valueMapper = (ParameterValueMapper<T>) STRING_TO_VALUE.get(entity.getParameter().getType());
		final T parsedValue = valueMapper.toValue.apply(entity.getData());
		valueMapper.setter.accept(vo, parsedValue);
		return parsedValue;
	}

	/**
	 * {@link ParameterValueEditionVo} to JPA entity transformer.
	 * 
	 * @param vo
	 *            The object to convert.
	 * @return The mapped entity.
	 */
	public static ParameterValue toEntity(final ParameterValueEditionVo vo) {
		final ParameterValue entity = new ParameterValue();
		final Parameter parameter = new Parameter();
		parameter.setId(vo.getParameter());
		entity.setParameter(parameter);

		// Map constraint data
		setData(vo, entity);

		return entity;
	}

	private static void setData(final ParameterValueEditionVo vo, final ParameterValue entity) {
		try {
			if (vo.getBinary() != null) { // NOPMD
				entity.setData(vo.getBinary().toString());
			} else if (vo.getDate() != null) { // NOPMD
				entity.setData(String.valueOf(vo.getDate().getTime()));
			} else if (vo.getIndex() != null) { // NOPMD
				entity.setData(vo.getIndex().toString());
			} else if (vo.getInteger() != null) { // NOPMD
				entity.setData(vo.getInteger().toString());
			} else if (vo.getTags() != null) { // NOPMD
				entity.setData(MAPPER.writeValueAsString(vo.getTags()).toUpperCase(Locale.FRANCE));
			} else if (vo.getSelections() != null) { // NOPMD
				entity.setData(MAPPER.writeValueAsString(vo.getSelections()));
			} else {
				entity.setData(vo.getText());
			}
		} catch (final JsonProcessingException e) {
			throw new TechnicalException("Unable to build JSon data from bean " + vo, e);
		}
	}

	/**
	 * Check optional but secure assertions.
	 */
	private void checkCompletude(final ParameterValueEditionVo vo) {
		Arrays.stream(new Supplier<?>[] { vo::getText, vo::getBinary, vo::getDate, vo::getIndex, vo::getInteger, vo::getTags, vo::getSelections })
				.map(Supplier::get).filter(Objects::nonNull).skip(1).findFirst().ifPresent(e -> {
					final ValidationJsonException exception = new ValidationJsonException();
					exception.addError(vo.getParameter(), "Too many values");
					throw exception;
				});
	}

	/**
	 * Check the data constraints and return the associated parameter definition.
	 */
	private Parameter checkConstraints(final ParameterValueEditionVo bean) {
		final Parameter parameter = parameterRepository.findOneExpected(bean.getParameter());
		typeToChecker.get(parameter.getType()).accept(bean, parameter);
		checkCompletude(bean);
		return parameter;
	}

	/**
	 * Check tags
	 */
	private void checkTags(final ParameterValueEditionVo bean) {
		ValidationJsonException.assertNotnull(bean.getTags(), bean.getParameter());
		bean.getTags().forEach(tag -> ValidationJsonException.assertTrue(StringUtils.isNotBlank(tag), "NotBlank", bean.getParameter()));
	}

	/**
	 * Check multiple selection
	 */
	private void checkMultiple(final ParameterValueEditionVo bean, final Parameter parameter) {
		ValidationJsonException.assertNotnull(bean.getSelections(), parameter.getId());
		final List<String> multiple = toConfiguration(parameter.getData(), LIST_STRING_TYPE);
		// Check each index
		for (final int index : bean.getSelections()) {
			checkArrayBound(index, multiple.size(), parameter);
		}
	}

	/**
	 * Check simple selection
	 */
	private void checkSelect(final ParameterValueEditionVo bean, final Parameter parameter) {
		ValidationJsonException.assertNotnull(bean.getIndex(), bean.getParameter());
		final List<String> single = toConfiguration(parameter.getData(), LIST_STRING_TYPE);

		// Check the index
		checkArrayBound(bean.getIndex(), single.size(), parameter);
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
	private void checkInteger(final ParameterValueEditionVo bean, final Parameter parameter) {
		ValidationJsonException.assertNotnull(bean.getInteger(), parameter.getId());
		final Map<String, Integer> minMax = toConfiguration(parameter.getData(), MAP_STRING_TYPE);
		// Check minimal value
		if (minMax.get("max") != null) {
			checkMax(bean.getInteger(), minMax.get("max"), parameter);
		}
		// Check maximal value
		if (minMax.get("min") != null) {
			checkMin(bean.getInteger(), minMax.get("min"), parameter);
		}
	}

	/**
	 * Check text
	 */
	private void checkText(final ParameterValueEditionVo bean, final Parameter parameter) {
		// Check the value if not empty
		if (StringUtils.isNotBlank(bean.getText()) && StringUtils.isNotBlank(parameter.getData())) {
			// Check the pattern if present
			final Map<String, String> stringProperties = toConfiguration(parameter.getData(), MAP_STRING_STRING_TYPE);
			final String patternString = stringProperties.get("pattern");
			if (StringUtils.isNotBlank(patternString)) {
				// Pattern is provided, check the string
				final Pattern pattern = Pattern.compile(patternString);
				ValidationJsonException.assertTrue(pattern.matcher(bean.getText()).matches(), javax.validation.constraints.Pattern.class.getName(),
						parameter.getId(), pattern.pattern());
			}
		}
	}

	/**
	 * Check transition and convert to entity.
	 * 
	 * @param vo
	 *            new {@link ParameterValueEditionVo} to persist.
	 * @return corresponding entity.
	 */
	public ParameterValue create(final ParameterValueEditionVo vo) {
		final Parameter parameter = checkConstraints(vo);
		checkCompletude(vo);

		final ParameterValue value = toEntity(vo);

		// Override the parameter with the loaded one
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
	 * @return secured associated parameters values. Key of returned map is the identifier of
	 *         {@link org.ligoj.app.model.Parameter}
	 */
	public Map<String, String> getNonSecuredSubscriptionParameters(final int subscription) {
		return toMapValues(repository.findAllSecureBySubscription(subscription));
	}

	/**
	 * Return all parameters values related to the subscription. Secured (encrypted) are parameters are decrypted.
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

	/**
	 * Create given values.
	 * 
	 * @param parameters
	 *            the parameter values to persist.
	 * @param presave
	 *            The operation to perform before persisting.
	 */
	public void create(final List<ParameterValueEditionVo> parameters, final Consumer<ParameterValue> presave) {
		// Persist each not null parameter
		parameters.stream().map(this::create).filter(v -> StringUtils.isNotBlank(v.getData())).forEach(v -> {
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
	 * Transform {@link List} to {@link Map} where key is the parameter name. Secured parameters are decrypted.
	 * 
	 * @param values
	 *            The parameters list.
	 * @return the corresponding key/values
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
	 * @return the corresponding map.
	 */
	public <K extends Serializable, V extends Persistable<K>> Map<K, V> toMap(final Iterable<V> items) {
		final Map<K, V> result = new LinkedHashMap<>();
		items.forEach(item -> result.put(item.getId(), item));
		return result;
	}
}
