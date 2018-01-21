package org.ligoj.app.resource.plugin;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CacheResult;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * A a cache manager for token used by the {@link CurlProcessor}
 */
@Component
public class CurlCacheToken {

	@Autowired
	protected ApplicationContext applicationContext;

	/**
	 * Return a cache token.
	 * 
	 * @param key
	 *            The cache key.
	 * @param function
	 *            The {@link Function} used to retrieve the token value when the cache fails.
	 * @param retries
	 *            The amount of retries until the provider returns a not <code>null</code> value.
	 * @param exceptionSupplier
	 *            The exception used when the token cannot be retrieved.
	 *            @param <X>
	 * @return The token value either from the cache, either from the fresh computed one.
	 */
	@CacheResult(cacheName = "curl-tokens")
	public String getTokenCache(@CacheKey @NotNull final String key, final Function<String, String> function,
			final int retries, final Supplier<? extends RuntimeException> exceptionSupplier) {
		// First access to this function
		return IntStream.range(0, retries).mapToObj(i -> function.apply(key)).filter(Objects::nonNull).findFirst().orElseThrow(exceptionSupplier);
	}

	/**
	 * Return a synchronized cache token.
	 * 
	 * @param synchronizeObject
	 *            The object used to synchronize the access to the cache.
	 * @param key
	 *            The cache key.
	 * @param function
	 *            The {@link Function} used to retrieve the token value when the cache fails.
	 * @param retries
	 *            The amount of retries until the provider returns a not <code>null</code> value.
	 * @param exceptionSupplier
	 *            The exception used when the token cannot be retrieved.
	 * @return The token value either from the cache, either from the fresh computed one.
	 */
	public String getTokenCache(@NotNull final Object synchronizeObject, @NotNull final String key,
			final Function<String, String> function, final int retries, final Supplier<? extends RuntimeException> exceptionSupplier) {
		synchronized (synchronizeObject) {
			// Use the jcache API to get the token
			return applicationContext.getBean(CurlCacheToken.class).getTokenCache(key, function, retries, exceptionSupplier);
		}
	}

}
