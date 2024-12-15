/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.node;

import org.apache.commons.lang3.StringUtils;
import org.ligoj.app.model.ParameterType;
import org.ligoj.app.model.ParameterValue;
import org.ligoj.bootstrap.core.crypto.CryptoHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Persistable;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Parameter value helper.
 */
@Component
public class ParameterValueHelper {

	/**
	 * A mapper configuration to parse string to parameter value.
	 */
	private static final Map<ParameterType, ParameterValueMapper<?>> TO_VALUE = new EnumMap<>(ParameterType.class);

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
				new ParameterValueMapper<>(BasicParameterValueVo::setSelections, ParameterHelper::toListInteger));
		TO_VALUE.put(ParameterType.SELECT,
				new ParameterValueMapper<>(BasicParameterValueVo::setIndex, Integer::valueOf));
		TO_VALUE.put(ParameterType.TAGS,
				new ParameterValueMapper<>(BasicParameterValueVo::setTags, ParameterHelper::toListString));
		TO_VALUE.put(ParameterType.TEXT,
				new ParameterValueMapper<>(BasicParameterValueVo::setText, Function.identity()));
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
