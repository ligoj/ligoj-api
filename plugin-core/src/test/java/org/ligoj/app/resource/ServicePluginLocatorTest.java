/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource;

import javax.transaction.Transactional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ligoj.app.AbstractAppTest;
import org.ligoj.app.api.ConfigurablePlugin;
import org.ligoj.app.api.PluginNotFoundException;
import org.ligoj.app.api.ServicePlugin;
import org.ligoj.app.resource.node.sample.BugTrackerResource;
import org.ligoj.app.resource.node.sample.ConfluencePluginResource;
import org.ligoj.app.resource.node.sample.JiraBaseResource;
import org.ligoj.app.resource.plugin.AbstractServicePlugin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Test class of {@link ServicePluginLocator}
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
public class ServicePluginLocatorTest extends AbstractAppTest {

	@Autowired
	private ServicePluginLocator component;

	@Test
	public void getResourceNotExist() {
		Assertions.assertNull(component.getResource("any"));
	}

	@Test
	public void getResourceExpectedNotExist() {
		Assertions.assertEquals("any", Assertions.assertThrows(PluginNotFoundException.class, () -> {
			component.getResourceExpected("any", ServicePlugin.class);
		}).getPlugin());
	}

	@Test
	public void getResourceNull() {
		Assertions.assertNull(component.getResource(null, ConfigurablePlugin.class));
	}

	@Test
	public void getResourceType() {
		final ConfigurablePlugin resource = component.getResource(JiraBaseResource.KEY, ConfigurablePlugin.class);
		Assertions.assertNotNull(resource);
		Assertions.assertTrue(resource instanceof BugTrackerResource);
	}

	@Test
	public void getResourceTypeParent() {
		final ConfigurablePlugin resource = component.getResource(JiraBaseResource.KEY + ":any",
				ConfigurablePlugin.class);
		Assertions.assertNotNull(resource);
		Assertions.assertTrue(resource instanceof BugTrackerResource);
	}

	@Test
	public void getResourceTypeNull() {
		Assertions.assertNull(component.getResource(ConfluencePluginResource.KEY, ConfigurablePlugin.class));
	}

	@Test
	public void getResource() {
		Assertions.assertEquals(JiraBaseResource.KEY, component.getResource(JiraBaseResource.KEY).getKey());
	}

	@Test
	public void getResourceExpected() {
		Assertions.assertEquals(JiraBaseResource.KEY,
				component.getResourceExpected(JiraBaseResource.KEY, ServicePlugin.class).getKey());
	}

	@Test
	public void getResourceParent() {
		Assertions.assertEquals(BugTrackerResource.SERVICE_KEY,
				component.getResource(BugTrackerResource.SERVICE_KEY + ":any").getKey());
	}

	@Test
	public void isEnabledTool() {
		Assertions.assertTrue(component.isEnabled(JiraBaseResource.KEY));
	}

	@Test
	public void isEnabledToolNo() {
		Assertions.assertFalse(component.isEnabled(BugTrackerResource.SERVICE_KEY + ":any"));
	}

	@Test
	public void isEnabledNode() {
		Assertions.assertTrue(component.isEnabled(JiraBaseResource.KEY + ":any"));
	}

	@Test
	public void isEnabledService() {
		Assertions.assertTrue(component.isEnabled(BugTrackerResource.SERVICE_KEY));
	}

	@Test
	public void isEnabledServiceRoot() {
		Assertions.assertFalse(component.isEnabled(AbstractServicePlugin.BASE_KEY));
	}

	@Test
	public void isEnabledServiceEmpty() {
		Assertions.assertFalse(component.isEnabled(""));
	}

	@Test
	public void isEnabledServiceNull() {
		Assertions.assertFalse(component.isEnabled(null));
	}
}
