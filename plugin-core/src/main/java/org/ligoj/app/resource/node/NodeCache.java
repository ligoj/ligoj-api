/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.node;

import com.hazelcast.cache.HazelcastCacheManager;
import com.hazelcast.config.EvictionConfig;
import org.ligoj.bootstrap.resource.system.cache.CacheConfigurer;
import org.ligoj.bootstrap.resource.system.cache.CacheManagerAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Role;
import org.springframework.stereotype.Component;

import javax.cache.expiry.Duration;

import static java.util.concurrent.TimeUnit.HOURS;

/**
 * Nodes data cache configurations.
 */
@Component
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class NodeCache implements CacheManagerAware {

	@Override
	public void onCreate(final HazelcastCacheManager cacheManager, final CacheConfigurer configurer) {
		cacheManager.createCache("nodes", configurer.newCacheConfig("nodes"));
		cacheManager.createCache("node-parameters", configurer.newCacheConfig("node-parameters"));
		cacheManager.createCache("services", configurer.newCacheConfig("services"));
		cacheManager.createCache("node-enablement", configurer.newCacheConfig("node-enablement"));
		final var tokens = configurer.newCacheConfig("curl-tokens",new Duration(HOURS, 10));
		tokens.setEvictionConfig(new EvictionConfig());
		cacheManager.createCache("curl-tokens", tokens);
		cacheManager.createCache("subscription-parameters", configurer.newCacheConfig("subscription-parameters"));
		cacheManager.createCache("plugin-data", configurer.newCacheConfig("plugin-data"));
	}

}
