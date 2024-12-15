/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.node;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.apache.commons.lang3.StringUtils;
import org.ligoj.app.model.Parameter;
import org.ligoj.app.model.ParameterType;
import org.ligoj.app.model.ParameterValue;
import org.ligoj.bootstrap.core.crypto.CryptoHelper;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Persistable;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Criteria values Business Layer for entity {@link ParameterValue}
 */
@Component
public class ParameterValueHelper {

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
	private CryptoHelper cryptoHelper;

	private record ParameterValueMapper<X>(
			BiConsumer<BasicParameterValueVo, X> setter,
			Function<String, X> toValue) {
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
	public ParameterValueHelper() {
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
	 * Return the data String from the true data value.
	 *
	 * @param vo The object to convert.
	 * @return The String data to persist.
	 */
	public static String toData(final BasicParameterValueVo vo) {
		return StringUtils.trimToNull(TO_STRING.entrySet().stream().filter(e -> e.getKey().apply(vo) != null)
				.findFirst().map(e -> e.getValue().apply(e.getKey().apply(vo))).orElse(vo.getText()));
	}

	/**
	 * Check the data constraints and return the associated parameter definition.
	 */
	void checkConstraints(final BasicParameterValueVo vo, final Parameter parameter) {
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
		final var multiple = ParameterResource.toListString(parameter.getData());

		// Check each index
		vo.getSelections().forEach(i -> checkArrayBound(i, multiple.size(), parameter));
	}

	/**
	 * Check simple selection
	 */
	private void checkSelect(final BasicParameterValueVo vo, final Parameter parameter) {
		assertNotnull(vo.getIndex(), parameter.getId());
		final var single = ParameterResource.toListString(parameter.getData());

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
		final var minMax = ParameterResource.toMapInteger(parameter.getData());
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
			final var stringProperties = ParameterResource.toMapString(parameter.getData());
			final var patternString = stringProperties.get("pattern");
			if (StringUtils.isNotBlank(patternString)) {
				// Pattern is provided, check the string
				final var pattern = Pattern.compile(patternString);
				assertTrue(pattern.matcher(vo.getText()).matches(),
						jakarta.validation.constraints.Pattern.class.getSimpleName(), parameter.getId(), "regexp",
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
	 * Transform {@link List} to {@link Map} where key is the parameter name. Secured parameters are decrypted.
	 *
	 * @param values The parameters list.
	 * @return the corresponding key/values. Never <code>null</code>.
	 */
	public Map<String, String> toMapValues(final List<ParameterValue> values) {
		final Map<String, String> result = new HashMap<>();
		for (final var value : values) {
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
				// Non-empty value, can be stored
				result.put(value.getParameter().getId(), data);
			}
		}
		return result;
	}

	/**
	 * Transform {@link List} to {@link Map} where K is the item's identifier, and VALUE is the original item.
	 *
	 * @param items The items list.
	 * @param <K>   The entity's identifier type.
	 * @param <V>   The entity type.
	 * @return the corresponding map.
	 */
	public <K extends Serializable, V extends Persistable<K>> Map<K, V> toMap(final Iterable<V> items) {
		final var result = new LinkedHashMap<K, V>();
		items.forEach(item -> result.put(item.getId(), item));
		return result;
	}

}
