package org.ligoj.app.resource;

import javax.transaction.Transactional;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ligoj.app.AbstractJpaTest;
import org.ligoj.app.api.ConfigurablePlugin;
import org.ligoj.app.api.PluginNotFoundException;
import org.ligoj.app.api.ServicePlugin;
import org.ligoj.app.resource.node.sample.BugTrackerResource;
import org.ligoj.app.resource.node.sample.ConfluencePluginResource;
import org.ligoj.app.resource.node.sample.JiraBaseResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Test class of {@link ServicePluginLocator}
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
public class ServicePluginLocatorTest extends AbstractJpaTest {

	@Autowired
	private ServicePluginLocator component;

	@Test
	public void getResourceNotExist() {
		Assert.assertNull(component.getResource("any"));
	}

	@Test
	public void getResourceExpectedNotExist() {
		try {
			component.getResourceExpected("any", ServicePlugin.class);
			Assert.fail("Expected PluginNotFoundException");
		} catch (final PluginNotFoundException pne) {
			Assert.assertEquals("any", pne.getPlugin());
		}
	}

	@Test
	public void getResourceNull() {
		Assert.assertNull(component.getResource(null, ConfigurablePlugin.class));
	}

	@Test
	public void getResourceType() {
		final ConfigurablePlugin resource = component.getResource(JiraBaseResource.KEY, ConfigurablePlugin.class);
		Assert.assertNotNull(resource);
		Assert.assertTrue(resource instanceof BugTrackerResource);
	}

	@Test
	public void getResourceTypeParent() {
		final ConfigurablePlugin resource = component.getResource(JiraBaseResource.KEY + ":any", ConfigurablePlugin.class);
		Assert.assertNotNull(resource);
		Assert.assertTrue(resource instanceof BugTrackerResource);
	}

	@Test
	public void getResourceTypeNull() {
		Assert.assertNull(component.getResource(ConfluencePluginResource.KEY, ConfigurablePlugin.class));
	}

	@Test
	public void getResource() {
		Assert.assertEquals(JiraBaseResource.KEY, component.getResource(JiraBaseResource.KEY).getKey());
	}

	@Test
	public void getResourceExpected() {
		Assert.assertEquals(JiraBaseResource.KEY, component.getResourceExpected(JiraBaseResource.KEY, ServicePlugin.class).getKey());
	}

	@Test
	public void getResourceParent() {
		Assert.assertEquals(BugTrackerResource.SERVICE_KEY, component.getResource(BugTrackerResource.SERVICE_KEY + ":any").getKey());
	}
}
