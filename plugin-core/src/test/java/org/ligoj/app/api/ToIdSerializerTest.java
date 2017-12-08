package org.ligoj.app.api;

import org.junit.Assert;
import org.junit.Test;
import org.ligoj.app.api.ToIdSerializer;
import org.ligoj.bootstrap.core.json.ObjectMapperTrim;
import org.ligoj.bootstrap.core.model.AbstractPersistable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Test class of {@link ToIdSerializer}
 * TODO Delete with ligoj/bootstrap 1.7.7+
 */
public class ToIdSerializerTest {

	@Getter
	@AllArgsConstructor
	public static class Bean {
		@JsonSerialize(using = ToIdSerializer.class)
		private BeanA asIdInt;

		@JsonSerialize(using = ToIdSerializer.class)
		private BeanB asIdString;
	}

	public class BeanA extends AbstractPersistable<Integer> {
		// Only a template class implementation
	}

	public class BeanB extends AbstractPersistable<String> {
		// Only a template class implementation
	}

	@Test
	public void serializeInt() throws JsonProcessingException {
		final BeanA bean = new BeanA();
		bean.setId(1);
		Assert.assertEquals("{\"asIdInt\":1,\"asIdString\":null}", new ObjectMapperTrim().writeValueAsString(new Bean(bean, null)));
	}

	@Test
	public void serializeString() throws JsonProcessingException {
		final BeanB bean = new BeanB();
		bean.setId("key");
		Assert.assertEquals("{\"asIdInt\":null,\"asIdString\":\"key\"}", new ObjectMapperTrim().writeValueAsString(new Bean(null, bean)));
	}
}
