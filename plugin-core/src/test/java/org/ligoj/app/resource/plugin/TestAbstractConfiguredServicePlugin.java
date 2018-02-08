package org.ligoj.app.resource.plugin;

import java.util.Collections;

import javax.persistence.EntityNotFoundException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ligoj.app.dao.ProjectRepository;
import org.ligoj.app.dao.SubscriptionRepository;
import org.ligoj.app.model.Configurable;
import org.ligoj.app.model.Node;
import org.ligoj.app.model.PluginConfiguration;
import org.ligoj.app.model.Project;
import org.ligoj.app.model.Subscription;
import org.ligoj.bootstrap.core.INamableBean;
import org.ligoj.bootstrap.core.dao.RestRepository;
import org.ligoj.bootstrap.core.security.SecurityHelper;
import org.mockito.Mockito;

/**
 * Test class of {@link AbstractConfiguredServicePlugin}
 */
public class TestAbstractConfiguredServicePlugin {

	private static interface NamedConfigurable
			extends Configurable<PluginConfiguration, Integer>, INamableBean<Integer> {
	}

	private AbstractConfiguredServicePlugin<PluginConfiguration> resource;

	private RestRepository<Configurable<PluginConfiguration, Integer>, Integer> repository;
	private PluginConfiguration configuration;
	private NamedConfigurable configurable;
	private Subscription subscription;
	private Project project;

	@SuppressWarnings("unchecked")
	@BeforeEach
	public void prepareMock() {
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
		resource.subscriptionRepository = Mockito.mock(SubscriptionRepository.class);
		resource.projectRepository = Mockito.mock(ProjectRepository.class);
		resource.securityHelper = Mockito.mock(SecurityHelper.class);

		repository = Mockito.mock(RestRepository.class);
		configuration = Mockito.mock(PluginConfiguration.class);
		configurable = Mockito.mock(NamedConfigurable.class);
		subscription = new Subscription();
		subscription.setId(33);
		project = new Project();
		project.setId(44);
		subscription.setProject(project);
		Mockito.when(resource.securityHelper.getLogin()).thenReturn("junit");
		Mockito.when(configurable.getConfiguration()).thenReturn(configuration);
		Mockito.when(configurable.getId()).thenReturn(1);
		Mockito.when(configurable.getName()).thenReturn("my-name");
		Mockito.when(configuration.getSubscription()).thenReturn(subscription);
		Mockito.when(resource.subscriptionRepository.findOneExpected(33)).thenReturn(subscription);
		Mockito.when(resource.projectRepository.findOneVisible(44, "junit")).thenReturn(project);
		Mockito.when(repository.findOneExpected(1)).thenReturn(configurable);
		Mockito.when(repository.findAllBy("configuration.subscription.id", subscription.getId()))
				.thenReturn(Collections.singletonList(configurable));
	}

	@Test
	public void deletedConfigured() throws Exception {
		resource.deletedConfigured(repository, 1);

		// Coverage
		Assertions.assertSame(configuration, resource.getConfiguration(1));
		Assertions.assertEquals("key", resource.getKey());
	}

	@Test
	public void deletedConfiguredKo() {
		project.setId(-1);
		Assertions.assertThrows(EntityNotFoundException.class, () -> {
			resource.deletedConfigured(repository, 1);
		});
	}

	@Test
	public void findConfigured() {
		Assertions.assertEquals(configurable, resource.findConfigured(repository, 1));
	}

	@Test
	public void findConfiguredByName() {
		Assertions.assertEquals(configurable,
				resource.findConfiguredByName(repository, "my-name", subscription.getId()));
	}

	@Test
	public void findConfiguredByNameNotFound() {
		Assertions.assertEquals("not-found", Assertions.assertThrows(EntityNotFoundException.class, () -> {
			Assertions.assertEquals(configurable,
					resource.findConfiguredByName(repository, "not-found", subscription.getId()));
		}).getMessage());
	}

	@Test
	public void findConfiguredKo() {
		project.setId(-1);
		Assertions.assertThrows(EntityNotFoundException.class, () -> {
			resource.findConfigured(repository, 1);
		});
	}

	@Test
	public void checkVisibility() {
		final Subscription subscription = new Subscription();
		final Node node = new Node();
		node.setId("service:s:t:i");
		subscription.setNode(node);
		Assertions.assertSame(subscription, resource.checkVisibility(subscription, "service:s"));
		Assertions.assertSame(subscription, resource.checkVisibility(subscription, "service:s:t"));
		Assertions.assertSame(subscription, resource.checkVisibility(subscription, "service:s:t:i"));
	}

	@Test
	public void checkVisibilityKo() {
		final Subscription subscription = new Subscription();
		final Node node = new Node();
		node.setId("service:s:t:i");
		subscription.setNode(node);
		subscription.setId(2000);
		Assertions.assertThrows(EntityNotFoundException.class, () -> {
			resource.checkVisibility(subscription, "any");
		});
	}
}
