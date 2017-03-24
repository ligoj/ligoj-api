package org.ligoj.app.resource.project;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import org.ligoj.app.api.NodeStatus;
import org.ligoj.app.api.UserOrg;
import org.ligoj.app.iam.IUserRepository;
import org.ligoj.app.iam.IamConfiguration;
import org.ligoj.app.iam.IamProvider;
import org.ligoj.app.model.Node;
import org.ligoj.app.model.Parameter;
import org.ligoj.app.model.ParameterType;
import org.ligoj.app.model.ParameterValue;
import org.ligoj.app.model.Project;
import org.ligoj.app.model.Subscription;
import org.ligoj.app.resource.node.EventVo;
import org.ligoj.app.resource.node.sample.IdentityResource;
import org.ligoj.app.resource.project.ProjectVo;
import org.ligoj.app.resource.project.ToVoConverter;
import org.ligoj.app.resource.subscription.SubscriptionVo;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

/**
 * Test class of {@link ToVoConverter}
 */
public class ToVoConverterTest {

	@Test
	public void applyEmpty() {
		final ToVoConverter converter = new ToVoConverter(s -> null, new ArrayList<>(), new HashMap<>());
		final Project entity = new Project();
		final ProjectVo vo = converter.apply(entity);
		Assert.assertNull(vo.getName());
		Assert.assertNull(vo.getPkey());
		Assert.assertNull(vo.getCreatedBy());
		Assert.assertNull(vo.getCreatedDate());
		Assert.assertNull(vo.getLastModifiedBy());
		Assert.assertNull(vo.getLastModifiedDate());
		Assert.assertNull(vo.getTeamLeader());
		Assert.assertNull(vo.getId());
		Assert.assertTrue(vo.getSubscriptions().isEmpty());
	}

	@Test
	public void apply() throws Exception {

		// Sub user repository
		final IamProvider iamProvider = Mockito.mock(IamProvider.class);
		final IUserRepository userRepository = Mockito.mock(IUserRepository.class);
		final IamConfiguration configuration = new IamConfiguration();
		configuration.setUserRepository(userRepository);
		Mockito.when(iamProvider.getConfiguration()).thenReturn(configuration);
		Mockito.when(userRepository.findById(ArgumentMatchers.anyString())).then(invocation -> toUser((String) invocation.getArguments()[0]));

		// Stub subscriptions
		final List<Object[]> subscriptions = new ArrayList<>();
		final Parameter parameter1 = new Parameter();
		parameter1.setId(IdentityResource.PARAMETER_GROUP);
		parameter1.setType(ParameterType.TEXT);
		parameter1.setOwner(new Node());
		final Parameter parameter2 = new Parameter();
		parameter2.setId(IdentityResource.PARAMETER_GROUP);
		parameter2.setType(ParameterType.TEXT);
		parameter2.setOwner(new Node());
		final Parameter parameter3 = new Parameter();
		parameter3.setId("any");
		parameter3.setType(ParameterType.TEXT);
		parameter3.setOwner(new Node());
		final ParameterValue value1 = new ParameterValue();
		value1.setId(1);
		value1.setParameter(parameter1);
		value1.setData("G");
		final ParameterValue value2 = new ParameterValue();
		value2.setId(2);
		value2.setParameter(parameter2);
		value2.setData("any");
		final ParameterValue value3 = new ParameterValue();
		value3.setId(3);
		value3.setParameter(parameter3);
		value3.setData("any");
		final Subscription subscription = new Subscription();
		subscription.setId(1);
		subscription.setNode(new Node());
		subscriptions.add(new Object[] { subscription, value1 });
		subscriptions.add(new Object[] { subscription, value2 });
		subscriptions.add(new Object[] { subscription, value3 });

		// Subscription without status
		final Subscription subscription2 = new Subscription();
		subscription2.setId(-1);
		subscription2.setNode(new Node());
		subscriptions.add(new Object[] { subscription2, value3 });

		// Stub events
		final Map<Integer, EventVo> events = new HashMap<>();
		final EventVo event = new EventVo();
		event.setSubscription(1);
		event.setLabel("UP");
		event.setValue("UP");
		events.put(1, event);

		// Call
		final ToVoConverter converter = new ToVoConverter(this::toUser, subscriptions, events);
		final Project entity = new Project();
		entity.setId(1);
		entity.setName("N");
		entity.setDescription("D");
		entity.setLastModifiedBy("U1");
		entity.setLastModifiedDate(new DateTime());
		entity.setCreatedBy("U2");
		entity.setCreatedDate(new DateTime());
		entity.setPkey("PK");
		entity.setTeamLeader("U3");
		final ProjectVo vo = converter.apply(entity);

		// Check
		Assert.assertEquals("N", vo.getName());
		Assert.assertEquals("D", vo.getDescription());
		Assert.assertEquals("PK", vo.getPkey());
		Assert.assertEquals("U2", vo.getCreatedBy().getId());
		Assert.assertNotNull(vo.getCreatedDate());
		Assert.assertEquals("U1", vo.getLastModifiedBy().getId());
		Assert.assertNotNull(vo.getLastModifiedDate());
		Assert.assertEquals("U3", vo.getTeamLeader().getId());
		Assert.assertEquals(1, vo.getId().intValue());
		Assert.assertEquals(2, vo.getSubscriptions().size());
		
		final Iterator<SubscriptionVo> iterator = vo.getSubscriptions().iterator();
		Assert.assertEquals(NodeStatus.UP, iterator.next().getStatus());
		Assert.assertNull(iterator.next().getStatus());
	}

	private UserOrg toUser(final String login) {
		final UserOrg user = new UserOrg();
		user.setId(login);
		return user;
	}
}
