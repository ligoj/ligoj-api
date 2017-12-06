package org.ligoj.app.api;

import java.io.IOException;

import org.springframework.data.domain.Persistable;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Serialize entities with their identifier.
 */
public class ToIdSerializer extends StdSerializer<Persistable<?>> {

	/**
	 * SID
	 */
	private static final long serialVersionUID = 1L;
	public static final ToIdSerializer INSTANCE = new ToIdSerializer();

	protected ToIdSerializer() {
		super(Persistable.class, false);
	}

	@Override
	public void serialize(final Persistable<?> bean, final JsonGenerator generator, final SerializerProvider provider) throws IOException {
		if (bean.getId() instanceof Number) {
			// Numeric, but no decimal accepted
			generator.writeNumber(((Number) bean.getId()).longValue());
		} else {
			// Consider ID as a String (not failsafe)
			generator.writeString((String) bean.getId());
		}
	}

}
