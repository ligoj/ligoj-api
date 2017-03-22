package org.ligoj.app.model;

import java.util.function.BiConsumer;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Test;
import org.ligoj.app.iam.model.CacheCompany;
import org.ligoj.app.iam.model.CacheGroup;
import org.ligoj.app.iam.model.CacheUser;

/**
 * Simple test of API beans.
 */
public class BeanTest {

	@Test
	public void testCacheCompany() {
		Assert.assertTrue(new CacheCompany().isNew());
		
	}

	@Test
	public void testCacheGroup() {
		Assert.assertTrue(new CacheGroup().isNew());
	}

	@Test
	public void testCacheUser() {
		final CacheUser user = new CacheUser();

		// Simple user attributes
		check(user, CacheUser::setCompany, CacheUser::getCompany, new CacheCompany());
		check(user, CacheUser::setFirstName, CacheUser::getFirstName, "first");
		check(user, CacheUser::setLastName, CacheUser::getLastName, "last");
		check(user, CacheUser::setMails, CacheUser::getMails, "singlemail");
	}

	private <T, X> void check(X bean, BiConsumer<X, T> setter, Function<X, T> getter, T value) {
		setter.accept(bean, value);
		Assert.assertEquals(value, getter.apply(bean));
	}
}
