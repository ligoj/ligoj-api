package org.ligoj.app.resource.plugin;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;

import net.sf.ehcache.CacheManager;

/**
 * Test class of {@link CurlCacheToken}
 */
public class CurlCacheTokenTest {

	private CurlCacheToken cacheToken;

	@Before
	@After
	public void clearCache() {
		CacheManager.getInstance().getCache("curl-tokens").removeAll();
		cacheToken = new CurlCacheToken();
		cacheToken.applicationContext = Mockito.mock(ApplicationContext.class);
		Mockito.when(cacheToken.applicationContext.getBean(CurlCacheToken.class)).thenReturn(cacheToken);
	}

	@Test
	public void getTokenCacheFailed() {
		final Object sync = new Object();
		AtomicInteger counter = new AtomicInteger();
		try {
			Assert.assertEquals("", cacheToken.getTokenCache(sync, "key", k -> {
				counter.incrementAndGet();
				return null;
			}, 2, () -> new ValidationJsonException()));
			Assert.fail("Expected ValidationJsonException");
		} catch (final ValidationJsonException ve) {
			// Good
		}
		Assert.assertEquals(2, counter.get());
		Assert.assertEquals("ok", cacheToken.getTokenCache(sync, "key", k -> {
			if (counter.incrementAndGet() == 4) {
				return "ok";
			}
			return null;
		}, 2, () -> new ValidationJsonException()));
		Assert.assertEquals(4, counter.get());
	}

	@Test
	public void getTokenCache() {
		final Object sync = new Object();
		AtomicInteger counter = new AtomicInteger();
		Assert.assertEquals("ok", cacheToken.getTokenCache(sync, "key", k -> {
			if (counter.incrementAndGet() == 2) {
				return "ok";
			}
			return null;
		}, 2, () -> new ValidationJsonException()));
		Assert.assertEquals(2, counter.get());
	}
}
