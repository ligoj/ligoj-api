package org.ligoj.app.resource.plugin;

import javax.persistence.EntityNotFoundException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.ligoj.app.dao.ProjectRepository;
import org.ligoj.app.dao.SubscriptionRepository;
import org.ligoj.app.model.Configurable;
import org.ligoj.app.model.PluginConfiguration;
import org.ligoj.app.model.Project;
import org.ligoj.app.model.Subscription;
import org.ligoj.bootstrap.core.dao.RestRepository;
import org.ligoj.bootstrap.core.security.SecurityHelper;
import org.mockito.Mockito;

/**
 * Test class of {@link AbstractConfiguredServicePlugin}
 */
public class TestAbstractConfiguredServicePlugin {

	private AbstractConfiguredServicePlugin<PluginConfiguration> resource;

	private RestRepository<Configurable<PluginConfiguration, Integer>, Integer> repository;
	private PluginConfiguration configuration;
	private Configurable<PluginConfiguration, Integer> configurable;
	private Subscription subscription;
	private Project project;

	@Before
	public void prepareMock() {
		resource = new AbstractConfiguredServicePlugin<PluginConfiguration>() {

			@Override
			public String getKey() {
				return null;
			}

			@Override
			public Object getConfiguration(int subscription) throws Exception {
				return null;
			}
		};
		resource.subscriptionRepository = Mockito.mock(SubscriptionRepository.class);
		resource.projectRepository = Mockito.mock(ProjectRepository.class);
		resource.securityHelper = Mockito.mock(SecurityHelper.class);

		repository = Mockito.mock(RestRepository.class);
		configuration = Mockito.mock(PluginConfiguration.class);
		configurable = Mockito.mock(Configurable.class);
		subscription = new Subscription();
		subscription.setId(33);
		project = new Project();
		project.setId(44);
		subscription.setProject(project);
		Mockito.when(resource.securityHelper.getLogin()).thenReturn("junit");
		Mockito.when(configurable.getConfiguration()).thenReturn(configuration);
		Mockito.when(configurable.getId()).thenReturn(1);
		Mockito.when(configuration.getSubscription()).thenReturn(subscription);
		Mockito.when(resource.subscriptionRepository.findOneExpected(33)).thenReturn(subscription);
		Mockito.when(resource.projectRepository.findOneVisible(44, "junit")).thenReturn(project);
		Mockito.when(repository.findOneExpected(1)).thenReturn(configurable);
	}

	@Test
	public void deletedConfigured() {
		resource.deletedConfigured(repository, 1);
	}

	@Test(expected = EntityNotFoundException.class)
	public void deletedConfiguredKo() {
		project.setId(-1);
		resource.deletedConfigured(repository, 1);
	}

	@Test
	public void findConfigured() {
		Assert.assertEquals(configurable, resource.findConfigured(repository, 1));
	}

	@Test(expected = EntityNotFoundException.class)
	public void findConfiguredKo() {
		project.setId(-1);
		resource.findConfigured(repository, 1);
	}
}
