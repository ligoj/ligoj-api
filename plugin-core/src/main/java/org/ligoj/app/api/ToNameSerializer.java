package org.ligoj.app.api;

import java.io.IOException;

import org.ligoj.bootstrap.core.INamableBean;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Serialize entities with their name.
 */
public class ToNameSerializer extends StdSerializer<INamableBean<?>> {

	/**
	 * SID
	 */
	private static final long serialVersionUID = 1L;
	public static final ToNameSerializer INSTANCE = new ToNameSerializer();

	protected ToNameSerializer() {
		super(INamableBean.class, false);
	}

	@Override
	public void serialize(final INamableBean<?> date, final JsonGenerator generator, final SerializerProvider provider) throws IOException {
		generator.writeString(date.getName());
	}

}
