/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.node;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ligoj.app.AbstractAppTest;
import org.ligoj.app.api.SubscriptionMode;
import org.ligoj.app.api.ToolPlugin;
import org.ligoj.app.dao.EventRepository;
import org.ligoj.app.dao.NodeRepository;
import org.ligoj.app.dao.ParameterValueRepository;
import org.ligoj.app.model.*;
import org.ligoj.app.resource.ServicePluginLocator;
import org.ligoj.bootstrap.core.json.ObjectMapperTrim;
import org.ligoj.bootstrap.core.resource.BusinessException;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test class of {@link NodeResource#checkConnection(NodeEditionVo)}: the connection check of a node configuration
 * before it is saved.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
class NodeConnectionCheckTest extends AbstractAppTest {

	@Autowired
	private NodeRepository repository;

	@Autowired
	private ParameterValueRepository parameterValueRepository;

	@Autowired
	private EventRepository eventRepository;

	@Autowired
	private CacheManager cacheManager;

	private NodeResource resource;

	private ToolPlugin plugin;

	/**
	 * A plug-in implementing the connection check, as a real one declares it: an interface mock would not.
	 */
	abstract static class CheckingPlugin implements ToolPlugin {
		@Override
		public abstract boolean checkConnection(String node, Map<String, String> parameters) throws Exception;
	}

	private final List<Map<String, String>> checked = new ArrayList<>();

	@BeforeEach
	void prepare() throws Exception {
		persistEntities("csv", new Class<?>[]{Node.class, Parameter.class, Project.class, Subscription.class,
				ParameterValue.class, Event.class, DelegateNode.class}, StandardCharsets.UTF_8);
		persistSystemEntities();
		clearAllCache();
		resource = new NodeResource();
		applicationContext.getAutowireCapableBeanFactory().autowireBean(resource);
		resource.self = resource;
		resource.locator = mock(ServicePluginLocator.class);
		plugin = mock(CheckingPlugin.class);
		when(resource.locator.getResourceExpected("service:bt:jira", ToolPlugin.class)).thenReturn(plugin);
		when(plugin.checkConnection(anyString(), anyMap())).thenAnswer(i -> {
			checked.add(i.getArgument(1));
			return true;
		});
	}

	private ParameterValueCreateVo value(final String parameter, final String text) {
		final var value = new ParameterValueCreateVo();
		value.setParameter(parameter);
		value.setText(text);
		return value;
	}

	private ParameterValueCreateVo untouched(final String parameter) {
		final var value = new ParameterValueCreateVo();
		value.setParameter(parameter);
		value.setUntouched(true);
		return value;
	}

	private NodeEditionVo node(final String id, final ParameterValueCreateVo... values) {
		final var vo = new NodeEditionVo();
		vo.setId(id);
		vo.setNode("service:bt:jira");
		vo.setName("Jira");
		vo.setMode(SubscriptionMode.LINK);
		vo.setParameters(List.of(values));
		return vo;
	}

	private String json(final NodeCheckVo result) {
		return new ObjectMapperTrim().writeValueAsString(result);
	}

	@Test
	void checkConnectionEditKeepsStoredSecrets() throws Exception {
		final var result = resource.checkConnection(node("service:bt:jira:6", untouched("service:bt:jira:user"),
				value("service:bt:jira:url", "http://remote")));
		Assertions.assertTrue(result.isUp());
		Assertions.assertEquals(List.of(Map.of("service:bt:jira:user", "junit", "service:bt:jira:url", "http://remote")), checked);
		verify(plugin).checkConnection(eq("service:bt:jira:6"), anyMap());
		// The stored secret is used, never returned
		Assertions.assertFalse(json(result).contains("junit"));
	}

	@Test
	void checkConnectionCreatePersistsNothing() throws Exception {
		final var nodes = repository.count();
		final var values = parameterValueRepository.count();
		final var events = eventRepository.count();
		final var result = resource.checkConnection(node("service:bt:jira:new", value("service:bt:jira:password", "secret"),
				value("service:bt:jira:url", "http://new")));
		Assertions.assertTrue(result.isUp());
		Assertions.assertEquals("secret", checked.getFirst().get("service:bt:jira:password"));
		Assertions.assertEquals(nodes, repository.count());
		Assertions.assertEquals(values, parameterValueRepository.count());
		Assertions.assertEquals(events, eventRepository.count());
		Assertions.assertNull(cacheManager.getCache("node-parameters").get("service:bt:jira:new"));
		Assertions.assertFalse(json(result).contains("secret"));
	}

	@Test
	void checkConnectionDown() throws Exception {
		when(plugin.checkConnection(anyString(), anyMap())).thenReturn(false);
		final var result = resource.checkConnection(node("service:bt:jira:6", value("service:bt:jira:url", "http://remote")));
		Assertions.assertFalse(result.isUp());
		Assertions.assertNull(result.getErrors());
		Assertions.assertNull(result.getMessage());
	}

	@Test
	void checkConnectionValidationError() throws Exception {
		when(plugin.checkConnection(anyString(), anyMap())).thenThrow(new ValidationJsonException("service:bt:jira:user", "jira-login"));
		final var result = resource.checkConnection(node("service:bt:jira:6", value("service:bt:jira:url", "http://remote")));
		Assertions.assertFalse(result.isUp());
		Assertions.assertEquals("jira-login", result.getErrors().get("service:bt:jira:user").getFirst().get("rule"));
	}

	@Test
	void checkConnectionInvalidValue() throws Exception {
		// A value refused by its parameter definition is a failed check naming the parameter, not a 400
		final var result = resource.checkConnection(node("service:bt:jira:6", value("c_17", "A")));
		Assertions.assertFalse(result.isUp());
		Assertions.assertEquals("Pattern", result.getErrors().get("c_17").getFirst().get("rule"));
		verifyNoInteractions(plugin);
	}

	@Test
	void checkConnectionBusinessError() throws Exception {
		when(plugin.checkConnection(anyString(), anyMap())).thenThrow(new BusinessException("Cannot access to the tool with these parameters"));
		final var result = resource.checkConnection(node("service:bt:jira:6", value("service:bt:jira:url", "http://remote")));
		Assertions.assertEquals("Cannot access to the tool with these parameters", result.getMessage());
	}

	@Test
	void checkConnectionHidesFailureMessage() throws Exception {
		when(plugin.checkConnection(anyString(), anyMap())).thenThrow(new IllegalStateException("http://admin:secret@remote refused"));
		final var result = resource.checkConnection(node("service:bt:jira:6", value("service:bt:jira:url", "http://remote")));
		Assertions.assertFalse(result.isUp());
		Assertions.assertEquals("node-check-failed", result.getMessage());
		Assertions.assertFalse(json(result).contains("secret"));
	}

	@Test
	void checkConnectionNotWritable() throws Exception {
		// This user sees only the Jenkins nodes
		initSpringSecurityContext("user1");
		final var vo = node("service:bt:jira:6", value("service:bt:jira:url", "http://remote"));
		Assertions.assertThrows(BusinessException.class, () -> resource.checkConnection(vo));
		verifyNoInteractions(plugin);
	}

	@Test
	void checkConnectionOtherParent() throws Exception {
		// The stored secrets of a node are never sent to the plug-in of another tool
		final var vo = node("service:bt:jira:6", untouched("service:bt:jira:user"));
		vo.setNode("service:build:jenkins");
		Assertions.assertThrows(ValidationJsonException.class, () -> resource.checkConnection(vo));
		verifyNoInteractions(plugin);
	}

	@Test
	void checkConnectionUnsupported() throws Exception {
		// A plug-in checking nothing on the given values, such as one whose status reads the stored configuration of the
		// node: "not checked", never a false "connection succeeded"
		final var calls = new ArrayList<String>();
		final ToolPlugin checksNothing = new ToolPlugin() {
			@Override
			public String getKey() {
				return "service:bt:jira";
			}

			@Override
			public boolean checkStatus(final String node, final Map<String, String> parameters) {
				calls.add(node);
				return true;
			}
		};
		when(resource.locator.getResourceExpected("service:bt:jira", ToolPlugin.class)).thenReturn(checksNothing);
		final var result = resource.checkConnection(node("service:bt:jira:6", value("service:bt:jira:url", "http://remote")));
		Assertions.assertFalse(result.isUp());
		Assertions.assertEquals("node-check-unsupported", result.getMessage());
		Assertions.assertEquals(List.of(), calls);
	}

	@Test
	void checkConnectionGivenValuesStatus() throws Exception {
		// A plug-in checking the given values with its anonymous status: the check uses it
		final ToolPlugin checksValues = new ToolPlugin() {
			@Override
			public String getKey() {
				return "service:bt:jira";
			}

			@Override
			public boolean checkStatus(final Map<String, String> parameters) {
				return "http://remote".equals(parameters.get("service:bt:jira:url"));
			}
		};
		when(resource.locator.getResourceExpected("service:bt:jira", ToolPlugin.class)).thenReturn(checksValues);
		Assertions.assertTrue(resource.checkConnection(node("service:bt:jira:6", value("service:bt:jira:url", "http://remote"))).isUp());
	}
}
