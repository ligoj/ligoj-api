/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CacheResult;

import org.apache.commons.lang3.StringUtils;
import org.ligoj.app.api.PluginNotFoundException;
import org.ligoj.app.api.ServicePlugin;
import org.ligoj.app.api.ToolPlugin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import lombok.Setter;

/**
 * Locate service resource from the plug-in key.
 */
@Component
public class ServicePluginLocator implements ApplicationContextAware {

	/**
	 * Used for "this" and forcing proxying.
	 */
	@Autowired
	protected ServicePluginLocator self;

	@Setter
	private ApplicationContext applicationContext;

	/**
	 * Return the plug-in from the service key.
	 *
	 * @param service the service name.
	 * @return the plug-in from the service key. <code>null</code> if not found.
	 */
	public ServicePlugin getResource(final String service) {
		return getResource(service, ServicePlugin.class);
	}

	/**
	 * Return the plug-in from the service key.
	 *
	 * @param service      the service name.
	 * @param requiredType The required resource class. For sample <code>ServicePlugin.class</code>
	 * @param <T>          The required resource type. For sample <code>ServicePlugin</code>
	 * @return the plug-in from the service key. <code>null</code> if not found.
	 */
	@SuppressWarnings("unchecked")
	public <T> T getResource(final String service, final Class<T> requiredType) {
		if (service == null) {
			// No service, may be from the recursive call ...
			return null;
		}

		// Search the resource
		final var name = self.getResourceName(service);
		if (name == null) {
			// Bean does not exists
			return null;
		}

		// Check the type
		final var bean = applicationContext.getBean(name, ServicePlugin.class);
		if (requiredType.isInstance(bean)) {
			return (T) bean;
		}
		// Try the parent
		return getResource(getParent(bean.getKey()), requiredType);
	}

	/**
	 * Return and expect the plug-in from the service key.
	 *
	 * @param service      the service name.
	 * @param requiredType The required resource class. For sample <code>ServicePlugin.class</code>
	 * @param <T>          The required resource type. For sample <code>ServicePlugin</code>
	 * @return the plug-in from the service key. <code>PluginException</code> if not found.
	 */
	public <T> T getResourceExpected(final String service, final Class<T> requiredType) {
		return Optional.ofNullable(getResource(service, requiredType))
				.orElseThrow(() -> new PluginNotFoundException(service));
	}

	/**
	 * Return the plug-in from the service key.
	 *
	 * @param service the service name.
	 * @return the plug-in from the service key. <code>null</code> if not found.
	 */
	@CacheResult(cacheName = "services")
	public String getResourceName(@CacheKey final String service) {
		return getResources(service).stream().findFirst().orElse(null);
	}

	/**
	 * Return all Spring context bean of type {@link ServicePlugin}
	 */
	private String[] getPluginResources() {
		return applicationContext.getBeanNamesForType(ServicePlugin.class);
	}

	/**
	 * Return the plug-in from the service key.
	 *
	 * @param service the service name.
	 * @return the plug-in from the service key.
	 */
	private List<String> getResources(final String service) {
		return getResources(getPluginResources(), service);
	}

	/**
	 * Return the resource of required service.
	 */
	private List<String> getResources(final String[] registeredServices, final String service) {
		final var result = getExactResources(registeredServices, service);
		if (result.isEmpty()) {
			return getParentResources(registeredServices, service, result);
		}
		return result;
	}

	/**
	 * Return the resources implementing the parent of given service key.
	 */
	private List<String> getParentResources(final String[] registeredServices, final String service,
			final List<String> result) {
		// Try the parent
		final var parentService = getParent(service);
		if (parentService == null) {
			return result;
		}
		return getResources(registeredServices, parentService);
	}

	/**
	 * Return the resources implementing the exact given service key.
	 */
	private List<String> getExactResources(final String[] registeredServices, final String service) {
		final var result = new ArrayList<String>();
		for (final var plugin : registeredServices) {
			final var bean = applicationContext.getBean(plugin, ServicePlugin.class);
			if (service.equals(bean.getKey())) {
				result.add(plugin);
			}
		}
		return result;
	}

	/**
	 * Return the parent service.
	 *
	 * @param service the service name. the parent service or <code>null</code>
	 * @return the parent service key.
	 */
	public String getParent(final String service) {
		if (StringUtils.countMatches(service, ':') <= 1) {
			return null;
		}
		return StringUtils.substringBeforeLast(service, ":");
	}

	/**
	 * Return the plug-in activation.
	 * 
	 * @param id The tested plug-in identifier.
	 * @return The plug-in activation.
	 */
	@CacheResult(cacheName = "node-enablement")
	public boolean isEnabled(String id) {
		return getResource(id, ToolPlugin.class) != null
				|| (getParent(id) == null && getResource(id, ServicePlugin.class) != null);
	}

}
