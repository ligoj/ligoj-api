/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.api;

import java.util.Collections;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Simple test of API beans.
 */
class BeanTest {

	@Test
	void testEnum() {
		SubscriptionMode.valueOf(SubscriptionMode.values()[0].name());
		Assertions.assertTrue(NodeStatus.valueOf(NodeStatus.values()[0].name()).isUp());
		Assertions.assertFalse(NodeStatus.valueOf(NodeStatus.values()[1].name()).isUp());
		Assertions.assertFalse(NodeStatus.getValue(false).isUp());
		Assertions.assertTrue(NodeStatus.getValue(true).isUp());
	}

	@Test
	void testSubscriptionStatusWithData() {
		check(new SubscriptionStatusWithData(), SubscriptionStatusWithData::setId, SubscriptionStatusWithData::getId,
				2);
		check(new SubscriptionStatusWithData(), SubscriptionStatusWithData::setNode,
				SubscriptionStatusWithData::getNode, "node");
		check(new SubscriptionStatusWithData(), SubscriptionStatusWithData::setParameters,
				SubscriptionStatusWithData::getParameters, Collections.emptyMap());
		check(new SubscriptionStatusWithData(), SubscriptionStatusWithData::setProject,
				SubscriptionStatusWithData::getProject, 1);
	}

	private <T, X> void check(X bean, BiConsumer<X, T> setter, Function<X, T> getter, T value) {
		setter.accept(bean, value);
		Assertions.assertEquals(value, getter.apply(bean));
	}
}
