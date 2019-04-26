/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.node;

import static java.util.concurrent.TimeUnit.HOURS;

import java.util.function.Function;

import javax.cache.expiry.Duration;
import javax.cache.expiry.ModifiedExpiryPolicy;

import org.ligoj.bootstrap.resource.system.cache.CacheManagerAware;
import org.springframework.stereotype.Component;

import com.hazelcast.cache.HazelcastCacheManager;
import com.hazelcast.config.CacheConfig;
import com.hazelcast.config.EvictionConfig;

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
		cacheManager.createCache("node-enablement", provider.apply("node-enablement"));
		final CacheConfig<?, ?> tokens = provider.apply("curl-tokens");
		tokens.setExpiryPolicyFactory(ModifiedExpiryPolicy.factoryOf(new Duration(HOURS, 10)));
		tokens.setEvictionConfig(new EvictionConfig() );
		cacheManager.createCache("curl-tokens", tokens);
		cacheManager.createCache("subscription-parameters", provider.apply("subscription-parameters"));
		cacheManager.createCache("plugin-data", provider.apply("plugin-data"));
	}

}
