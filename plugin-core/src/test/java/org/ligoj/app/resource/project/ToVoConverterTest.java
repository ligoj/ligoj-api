/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.project;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ligoj.app.api.NodeStatus;
import org.ligoj.app.api.NodeVo;
import org.ligoj.app.iam.IUserRepository;
import org.ligoj.app.iam.IamConfiguration;
import org.ligoj.app.iam.IamProvider;
import org.ligoj.app.iam.UserOrg;
import org.ligoj.app.model.Node;
import org.ligoj.app.model.Parameter;
import org.ligoj.app.model.ParameterType;
import org.ligoj.app.model.ParameterValue;
import org.ligoj.app.model.Project;
import org.ligoj.app.model.Subscription;
import org.ligoj.app.resource.ServicePluginLocator;
import org.ligoj.app.resource.node.EventVo;
import org.ligoj.app.resource.node.sample.IdentityResource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

/**
 * Test class of {@link ToVoConverter}
 */
class ToVoConverterTest {

	@Test
	void applyEmpty() {
		final var locator = Mockito.mock(ServicePluginLocator.class);
		final var converter = new ToVoConverter(locator, s -> null, new ArrayList<>(), new HashMap<>());
		final var entity = new Project();
		entity.setSubscriptions(Collections.emptyList());
		final var vo = converter.apply(entity);
		Assertions.assertNull(vo.getName());
		Assertions.assertNull(vo.getPkey());
		Assertions.assertNull(vo.getCreatedBy());
		Assertions.assertNull(vo.getCreatedDate());
		Assertions.assertNull(vo.getLastModifiedBy());
		Assertions.assertNull(vo.getLastModifiedDate());
		Assertions.assertNull(vo.getTeamLeader());
		Assertions.assertNull(vo.getId());
		Assertions.assertTrue(vo.getSubscriptions().isEmpty());
	}

	@Test
	void apply() {

		// Sub user repository
		final var iamProvider = Mockito.mock(IamProvider.class);
		final var userRepository = Mockito.mock(IUserRepository.class);
		final var configuration = new IamConfiguration();
		configuration.setUserRepository(userRepository);
		Mockito.when(iamProvider.getConfiguration()).thenReturn(configuration);
		Mockito.when(userRepository.findById(ArgumentMatchers.anyString()))
				.then(invocation -> toUser((String) invocation.getArguments()[0]));

		// Stub subscriptions
		final List<Object[]> subscriptions = new ArrayList<>();
		final var parameter1 = new Parameter();
		parameter1.setId(IdentityResource.PARAMETER_GROUP);
		parameter1.setType(ParameterType.TEXT);
		parameter1.setOwner(new Node());
		final var parameter2 = new Parameter();
		parameter2.setId(IdentityResource.PARAMETER_GROUP);
		parameter2.setType(ParameterType.TEXT);
		parameter2.setOwner(new Node());
		final var parameter3 = new Parameter();
		parameter3.setId("any");
		parameter3.setType(ParameterType.TEXT);
		parameter3.setOwner(new Node());
		final var value1 = new ParameterValue();
		value1.setId(1);
		value1.setParameter(parameter1);
		value1.setData("G");
		final var value2 = new ParameterValue();
		value2.setId(2);
		value2.setParameter(parameter2);
		value2.setData("any");
		final var value3 = new ParameterValue();
		value3.setId(3);
		value3.setParameter(parameter3);
		value3.setData("any");
		final var subscription = new Subscription();
		subscription.setId(1);
		final var node = new Node();
		node.setId("service:n2");
		subscription.setNode(node);
		subscriptions.add(new Object[] { subscription, value1 });
		subscriptions.add(new Object[] { subscription, value2 });
		subscriptions.add(new Object[] { subscription, value3 });

		// Subscription without status
		final var subscription2 = new Subscription();
		subscription2.setId(-1);
		final var node2 = new Node();
		node2.setId("service:n1");
		subscription2.setNode(node2);
		subscriptions.add(new Object[] { subscription2, value3 });

		// Stub events
		final Map<Integer, EventVo> events = new HashMap<>();
		final var event = new EventVo();
		event.setSubscription(1);
		event.setNode(new NodeVo());
		event.setValue("UP");
		events.put(1, event);

		// Call
		final var locator = Mockito.mock(ServicePluginLocator.class);
		Mockito.doReturn(true).when(locator).isEnabled("service:n1");
		Mockito.doReturn(false).when(locator).isEnabled("service:n2");
		final var converter = new ToVoConverter(locator, this::toUser, subscriptions, events);
		final var entity = new Project();
		entity.setId(1);
		entity.setName("N");
		entity.setDescription("D");
		entity.setLastModifiedBy("U1");
		entity.setLastModifiedDate(new Date());
		entity.setCreatedBy("U2");
		entity.setCreatedDate(new Date());
		entity.setPkey("PK");
		entity.setTeamLeader("U3");
		entity.setSubscriptions(Arrays.asList(subscription, subscription2));
		final var vo = converter.apply(entity);

		// Check
		Assertions.assertEquals("N", vo.getName());
		Assertions.assertEquals("D", vo.getDescription());
		Assertions.assertEquals("PK", vo.getPkey());
		Assertions.assertEquals("U2", vo.getCreatedBy().getId());
		Assertions.assertNotNull(vo.getCreatedDate());
		Assertions.assertEquals("U1", vo.getLastModifiedBy().getId());
		Assertions.assertNotNull(vo.getLastModifiedDate());
		Assertions.assertEquals("U3", vo.getTeamLeader().getId());
		Assertions.assertEquals(1, vo.getId().intValue());
		Assertions.assertEquals(2, vo.getSubscriptions().size());

		// Check the statuses and their order by node
		final var subscriptionsVo = vo.getSubscriptions();
		Assertions.assertNull(subscriptionsVo.getFirst().getStatus());
		Assertions.assertEquals("service:n1", subscriptionsVo.getFirst().getNode().getId());
		Assertions.assertTrue(subscriptionsVo.getFirst().getNode().getEnabled());
		Assertions.assertEquals(NodeStatus.UP, subscriptionsVo.get(1).getStatus());
		Assertions.assertEquals("service:n2", subscriptionsVo.get(1).getNode().getId());
		Assertions.assertFalse(subscriptionsVo.get(1).getNode().getEnabled());
	}

	private UserOrg toUser(final String login) {
		final var user = new UserOrg();
		user.setId(login);
		return user;
	}
}
