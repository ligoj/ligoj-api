/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.plugin;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ligoj.app.dao.ProjectRepository;
import org.ligoj.app.dao.SubscriptionRepository;
import org.ligoj.app.model.*;
import org.ligoj.bootstrap.core.INamableBean;
import org.ligoj.bootstrap.core.dao.RestRepository;
import org.ligoj.bootstrap.core.security.SecurityHelper;

import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class of {@link AbstractConfiguredServicePlugin}
 */
class TestAbstractConfiguredServicePlugin {

	private interface NamedConfigurable extends Configurable<PluginConfiguration, Integer>, INamableBean<Integer> {
	}

	private AbstractConfiguredServicePlugin<PluginConfiguration> resource;

	private RestRepository<Configurable<PluginConfiguration, Integer>, Integer> repository;
	private PluginConfiguration configuration;
	private NamedConfigurable configurable;
	private Subscription subscription;
	private Project project;

	@SuppressWarnings("unchecked")
	@BeforeEach
	void prepareMock() {
		resource = new AbstractConfiguredServicePlugin<>() {

			@Override
			public String getKey() {
				return "key";
			}

			@Override
			public Object getConfiguration(int subscription) {
				return configuration;
			}
		};
		resource.subscriptionRepository = mock(SubscriptionRepository.class);
		resource.projectRepository = mock(ProjectRepository.class);
		resource.securityHelper = mock(SecurityHelper.class);

		repository = mock(RestRepository.class);
		configuration = mock(PluginConfiguration.class);
		configurable = mock(NamedConfigurable.class);
		subscription = new Subscription();
		subscription.setId(33);
		project = new Project();
		project.setId(44);
		subscription.setProject(project);
		when(resource.securityHelper.getLogin()).thenReturn("junit");
		when(configurable.getConfiguration()).thenReturn(configuration);
		when(configurable.getId()).thenReturn(1);
		when(configurable.getName()).thenReturn("my-name");
		when(configuration.getSubscription()).thenReturn(subscription);
		when(resource.subscriptionRepository.findOneExpected(33)).thenReturn(subscription);
		when(resource.projectRepository.findOneVisible(44, "junit")).thenReturn(project);
		when(repository.findOneExpected(1)).thenReturn(configurable);
		when(repository.findAllBy("configuration.subscription.id", subscription.getId(),
				new String[] { "name" }, "my-name")).thenReturn(Collections.singletonList(configurable));
	}

	@Test
	void deletedConfigured() throws Exception {
		resource.deletedConfigured(repository, 1);

		// Coverage
		Assertions.assertSame(configuration, resource.getConfiguration(1));
		Assertions.assertEquals("key", resource.getKey());
	}

	@Test
	void deletedConfiguredKo() {
		project.setId(-1);
		Assertions.assertThrows(EntityNotFoundException.class, () -> resource.deletedConfigured(repository, 1));
	}

	@Test
	void findConfigured() {
		Assertions.assertEquals(configurable, resource.findConfigured(repository, 1));
	}

	@Test
	void findConfiguredByName() {
		Assertions.assertEquals(configurable,
				resource.findConfiguredByName(repository, "my-name", subscription.getId()));
	}

	@Test
	void findConfiguredByNameNotFound() {
		final var id = subscription.getId();
		Assertions.assertEquals("not-found", Assertions.assertThrows(EntityNotFoundException.class,
				() -> resource.findConfiguredByName(repository, "not-found", id)).getMessage());
	}

	@Test
	void findConfiguredKo() {
		project.setId(-1);
		Assertions.assertThrows(EntityNotFoundException.class, () -> resource.findConfigured(repository, 1));
	}

	@Test
	void checkVisibility() {
		final var entity = new Subscription();
		final var node = new Node();
		node.setId("service:s:t:i");
		entity.setNode(node);
		Assertions.assertSame(entity, resource.checkVisibility(entity, "service:s"));
		Assertions.assertSame(entity, resource.checkVisibility(entity, "service:s:t"));
		Assertions.assertSame(entity, resource.checkVisibility(entity, "service:s:t:i"));
	}

	@Test
	void checkVisibilityKo() {
		final var entity = new Subscription();
		final var node = new Node();
		node.setId("service:s:t:i");
		entity.setNode(node);
		entity.setId(2000);
		Assertions.assertThrows(EntityNotFoundException.class, () -> resource.checkVisibility(entity, "any"));
	}
}
