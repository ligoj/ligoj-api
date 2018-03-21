/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.node;

import java.util.function.Function;

import javax.cache.expiry.AccessedExpiryPolicy;
import javax.cache.expiry.Duration;

import org.ligoj.bootstrap.resource.system.cache.CacheManagerAware;
import org.springframework.stereotype.Component;

import com.hazelcast.cache.HazelcastCacheManager;
import com.hazelcast.config.CacheConfig;

/**
 * Nodes data cache configurations.
 */
@Component
public class NodeCache implements CacheManagerAware {

	@Override
	public void onCreate(final HazelcastCacheManager cacheManager, final Function<String, CacheConfig<?, ?>> provider) {
		cacheManager.createCache("nodes", provider.apply("nodes"));
		cacheManager.createCache("node-parameters", provider.apply("node-parameters"));
		cacheManager.createCache("services", provider.apply("services"));
		final CacheConfig<?, ?> tokens = provider.apply("curl-tokens");
		tokens.setExpiryPolicyFactory(AccessedExpiryPolicy.factoryOf(Duration.ONE_HOUR));
		cacheManager.createCache("curl-tokens", tokens);
		cacheManager.createCache("subscription-parameters", provider.apply("subscription-parameters"));
		cacheManager.createCache("plugin-data", provider.apply("plugin-data"));
	}

}
