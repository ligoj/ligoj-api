/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.model;

import java.util.Collections;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ligoj.app.iam.model.CacheCompany;
import org.ligoj.app.iam.model.CacheGroup;
import org.ligoj.app.iam.model.CacheUser;
import org.ligoj.app.iam.model.DelegateType;
import org.ligoj.app.iam.model.ReceiverType;
import org.ligoj.app.resource.plugin.BitBucketTag;
import org.ligoj.app.resource.plugin.BitBucketTags;

/**
 * Simple test of API beans for coverage and automatic nullability checks.
 */
class BeanTest {

	@Test
	void testCacheCompany() {
		Assertions.assertTrue(new CacheCompany().isNew());

	}

	@Test
	void testCacheGroup() {
		Assertions.assertTrue(new CacheGroup().isNew());
	}

	@Test
	void testCacheProjectGroup() {
		Assertions.assertNull(new CacheProjectGroup().getGroup());
		Assertions.assertNull(new CacheProjectGroup().getProject());
		Assertions.assertNull(new Project().getCacheGroups());
		Assertions.assertTrue(new AbstractStringKeyEntity(){}.isNew());
		new Project().setCacheGroups(Collections.emptyList());
	}

	@Test
	void testBitBucketTag() {
		check(new BitBucketTag(), BitBucketTag::setName, BitBucketTag::getName, "v");
	}

	@Test
	void testBitBucketTags() {
		check(new BitBucketTags(), BitBucketTags::setValues, BitBucketTags::getValues, Collections.emptyList());
	}

	@Test
	void testEnum() {
		ParameterType.valueOf(ParameterType.values()[0].name());
		ReceiverType.valueOf(ReceiverType.values()[0].name());
		DelegateType.valueOf(DelegateType.values()[0].name());
		EventType.valueOf(EventType.values()[0].name());
		ContainerType.valueOf(ContainerType.values()[0].name()).getDelegateType();
	}

	@Test
	void testCacheUser() {
		final var user = new CacheUser();

		// Simple user attributes
		check(user, CacheUser::setCompany, CacheUser::getCompany, new CacheCompany());
		check(user, CacheUser::setFirstName, CacheUser::getFirstName, "first");
		check(user, CacheUser::setLastName, CacheUser::getLastName, "last");
		check(user, CacheUser::setMails, CacheUser::getMails, "singlemail");
	}

	private <T, X> void check(X bean, BiConsumer<X, T> setter, Function<X, T> getter, T value) {
		setter.accept(bean, value);
		Assertions.assertEquals(value, getter.apply(bean));
	}
}
