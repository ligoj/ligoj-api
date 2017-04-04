package org.ligoj.app.api;

import java.util.Collections;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Test;

/**
 * Simple test of API beans.
 */
public class BeanTest {

	@Test
	public void testEnum() {
		SubscriptionMode.valueOf(SubscriptionMode.values()[0].name());
		Assert.assertTrue(NodeStatus.valueOf(NodeStatus.values()[0].name()).isUp());
		Assert.assertFalse(NodeStatus.valueOf(NodeStatus.values()[1].name()).isUp());
	}

	@Test
	public void testSubscriptionStatusWithData() {
		check(new SubscriptionStatusWithData(), SubscriptionStatusWithData::setId, SubscriptionStatusWithData::getId,2);
		check(new SubscriptionStatusWithData(), SubscriptionStatusWithData::setNode, SubscriptionStatusWithData::getNode, "node");
		check(new SubscriptionStatusWithData(), SubscriptionStatusWithData::setParameters, SubscriptionStatusWithData::getParameters,Collections.emptyMap());
		check(new SubscriptionStatusWithData(), SubscriptionStatusWithData::setProject, SubscriptionStatusWithData::getProject, 1);
	}

	private <T, X> void check(X bean, BiConsumer<X, T> setter, Function<X, T> getter, T value) {
		setter.accept(bean, value);
		Assert.assertEquals(value, getter.apply(bean));
	}
}
