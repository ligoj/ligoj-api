package org.ligoj.app.resource.plugin;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;

import net.sf.ehcache.CacheManager;

/**
 * Test class of {@link CurlCacheToken}
 */
public class CurlCacheTokenTest {

	private CurlCacheToken cacheToken;

	@BeforeEach
	@AfterEach
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
		Assertions.assertThrows(ValidationJsonException.class, () -> {
			Assertions.assertEquals("", cacheToken.getTokenCache(sync, "key", k -> {
				counter.incrementAndGet();
				return null;
			}, 2, () -> new ValidationJsonException()));
		});
		Assertions.assertEquals(2, counter.get());
		Assertions.assertEquals("ok", cacheToken.getTokenCache(sync, "key", k -> {
			if (counter.incrementAndGet() == 4) {
				return "ok";
			}
			return null;
		}, 2, () -> new ValidationJsonException()));
		Assertions.assertEquals(4, counter.get());
	}

	@Test
	public void getTokenCache() {
		final Object sync = new Object();
		AtomicInteger counter = new AtomicInteger();
		Assertions.assertEquals("ok", cacheToken.getTokenCache(sync, "key", k -> {
			if (counter.incrementAndGet() == 2) {
				return "ok";
			}
			return null;
		}, 2, () -> new ValidationJsonException()));
		Assertions.assertEquals(2, counter.get());
	}
}
