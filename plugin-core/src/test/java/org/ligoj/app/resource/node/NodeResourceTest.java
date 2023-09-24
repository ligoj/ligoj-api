/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.node;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ligoj.app.AbstractAppTest;
import org.ligoj.app.api.NodeStatus;
import org.ligoj.app.api.SubscriptionMode;
import org.ligoj.app.api.SubscriptionStatusWithData;
import org.ligoj.app.api.ToolPlugin;
import org.ligoj.app.dao.*;
import org.ligoj.app.model.*;
import org.ligoj.app.resource.ServicePluginLocator;
import org.ligoj.app.resource.node.sample.*;
import org.ligoj.bootstrap.core.resource.BusinessException;
import org.ligoj.bootstrap.core.resource.TechnicalException;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * {@link NodeResource} test cases.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
class NodeResourceTest extends AbstractAppTest {

	@Autowired
	private NodeRepository repository;
	@Autowired
	private SubscriptionRepository subscriptionRepository;

	@Autowired
	private NodeResource resource;

	@Autowired
	private ParameterValueResource parameterValueResource;

	private NodeResource resourceMock;

	@Autowired
	private ParameterValueRepository parameterValueRepository;

	@Autowired
	private ParameterRepository parameterRepository;

	@Autowired
	private TaskSampleNodeRepository taskSampleRepository;

	@Autowired
	private EventRepository eventRepository;

	@BeforeEach
	void prepare() throws IOException {
		persistEntities("csv", new Class[]{Node.class, Parameter.class, Project.class, Subscription.class,
				ParameterValue.class, Event.class, DelegateNode.class}, StandardCharsets.UTF_8);
		persistSystemEntities();
	}

	@BeforeEach
	@AfterEach
	void cleanNodeCache() {
		super.clearAllCache();
	}

	private void mockApplicationContext() {
		final var resource = new NodeResource();
		super.applicationContext.getAutowireCapableBeanFactory().autowireBean(resource);
		resource.self = resource;

		// Replace the plug-in locator
		resource.locator = Mockito.mock(ServicePluginLocator.class);
		this.resourceMock = resource;
	}

	@Test
	void checkNodesStatusFiltered() throws Exception {

		// This user sees only Jenkins nodes
		mockApplicationContext();
		initSpringSecurityContext("user1");
		final var resource = resourceMock;

		// Mock the servers
		prepareEvent();

		// check status
		final var eventsCount = eventRepository.count();
		resource.checkNodesStatus();
		/*
		 * Expected count 5 changes for tools :<br> +1 : Jenkins DOWN, was UP <br> Expected count 6 changes for
		 * subscriptions :<br> +1 : Subscription Jupiter - Jenkins, discovered, DOWN since node is DOWN <br> Nb events =
		 * nbPreviousEvents + nbNodes x2 (Because one node implies one subscription) less the already known nodes<br> =
		 * nbPreviousEvents + nbNodes x2<br>
		 */
		Assertions.assertEquals(eventsCount + 2, eventRepository.count());
	}

	@Test
	void checkNodesStatus() throws Exception {

		// This user sees all nodes
		mockApplicationContext();
		initSpringSecurityContext(DEFAULT_USER);
		final var resource = resourceMock;

		// Mock the servers
		prepareEvent();

		// check status
		final var eventsCount = eventRepository.count();
		resource.checkNodesStatus();
		/*
		 * Expected count 5 changes for tools :<br> +1 : Jenkins DOWN, was UP <br> Expected count 6 changes for
		 * subscriptions :<br> +1 : Subscription Jupiter - Jenkins, discovered, DOWN since node is DOWN <br> Nb events =
		 * nbPreviousEvents + nbNodes x2 (Because one node implies one subscription) less the already known nodes<br> =
		 * nbPreviousEvents + nbNodes x2<br>
		 */
		Assertions.assertEquals(eventsCount + 23, eventRepository.count());
	}

	@Test
	void checkNodeStatusNotVisible() throws Exception {

		// This user sees only Jenkins nodes
		mockApplicationContext();
		initSpringSecurityContext("user1");
		final var resource = resourceMock;

		// Mock the servers
		prepareEvent();

		// check status
		final var eventsCount = eventRepository.count();

		// Not visible node
		Assertions.assertNull(resource.checkNodeStatus("service:id:ldap:dig"));
		Assertions.assertEquals(eventsCount, eventRepository.count());
	}

	@Test
	void checkNodeStatus() throws Exception {
		mockApplicationContext();
		final var resource = resourceMock;

		// Mock the servers
		prepareEvent();

		// check status
		final var eventsCount = eventRepository.count();

		// Visible and down node
		Assertions.assertEquals(NodeStatus.DOWN, resource.checkNodeStatus("service:id:ldap:dig"));
		Assertions.assertEquals(eventsCount + 3, eventRepository.count());
	}

	@Test
	void getNodeStatusSingleNode() throws Exception {
		mockApplicationContext();
		final var resource = resourceMock;

		// Mock the servers
		prepareEvent();

		// Visible node, but without event/check
		Assertions.assertNull(resource.getNodeStatus("service:id:ldap:dig"));

		// First check to create the event
		Assertions.assertEquals(NodeStatus.DOWN, resource.checkNodeStatus("service:id:ldap:dig"));

		// Visible and down node
		Assertions.assertEquals(NodeStatus.DOWN, resource.getNodeStatus("service:id:ldap:dig"));
	}

	/**
	 * Mock the servers for event test
	 */
	private long prepareEvent() throws Exception {
		final var servicePluginLocator = resourceMock.locator;

		// 1 : service is down
		final var jira = Mockito.mock(JiraPluginResource.class);
		Mockito.when(servicePluginLocator.getResource(ArgumentMatchers.endsWith(":jira"),
				ArgumentMatchers.eq(ToolPlugin.class))).thenReturn(jira);
		Mockito.when(servicePluginLocator.getResourceExpected(ArgumentMatchers.endsWith(":jira"),
				ArgumentMatchers.eq(ToolPlugin.class))).thenReturn(jira);
		Mockito.when(jira.checkStatus(ArgumentMatchers.anyString(), ArgumentMatchers.anyMap())).thenReturn(false);

		// 2 : service is up
		final var sonar = Mockito.mock(SonarPluginResource.class);
		Mockito.when(servicePluginLocator.getResource(ArgumentMatchers.contains(":sonar"),
				ArgumentMatchers.eq(ToolPlugin.class))).thenReturn(sonar);
		Mockito.when(servicePluginLocator.getResourceExpected(ArgumentMatchers.contains(":sonar"),
				ArgumentMatchers.eq(ToolPlugin.class))).thenReturn(sonar);
		Mockito.when(sonar.checkStatus(ArgumentMatchers.anyString(), ArgumentMatchers.anyMap())).thenReturn(true);

		// 3 : service throw an exception (down)
		final var jenkins = Mockito.mock(JenkinsPluginResource.class);
		Mockito.when(servicePluginLocator.getResource(ArgumentMatchers.contains(":jenkins"),
				ArgumentMatchers.eq(ToolPlugin.class))).thenReturn(jenkins);
		Mockito.when(servicePluginLocator.getResourceExpected(ArgumentMatchers.contains(":jenkins"),
				ArgumentMatchers.eq(ToolPlugin.class))).thenReturn(jenkins);
		Mockito.when(jenkins.checkStatus(ArgumentMatchers.anyString(), ArgumentMatchers.anyMap()))
				.thenThrow(new TechnicalException("junit"));

		final var nbNodes = repository.findAllInstance().size();
		// Jira x2, Confluence, LDAP, Jenkins, SonarQube
		Assertions.assertTrue(nbNodes >= 6);
		return nbNodes;
	}

	@Test
	void checkNodesStatusScheduler() throws Exception {
		mockApplicationContext();
		final var resource = resourceMock;

		// data
		final var jiraNode = repository.findByName("JIRA 4");
		Assertions.assertFalse(jiraNode.isService());
		Assertions.assertFalse(jiraNode.isTool());
		Assertions.assertTrue(jiraNode.isInstance());
		Assertions.assertSame(jiraNode.getRefined(), jiraNode.getTool());

		Assertions.assertFalse(jiraNode.getRefined().isService());
		Assertions.assertTrue(jiraNode.getRefined().isTool());
		Assertions.assertFalse(jiraNode.getRefined().isInstance());
		Assertions.assertSame(jiraNode.getRefined(), jiraNode.getRefined().getTool());

		Assertions.assertTrue(jiraNode.getRefined().getRefined().isService());
		Assertions.assertFalse(jiraNode.getRefined().getRefined().isTool());
		Assertions.assertFalse(jiraNode.getRefined().getRefined().isInstance());
		Assertions.assertNull(jiraNode.getRefined().getRefined().getTool());

		// Mock the servers
		final var nbNodes = prepareEvent();

		// check status
		final var eventsCount = eventRepository.count();
		resource.checkNodesStatusScheduler();
		/*
		 * Expected count 5 changes for tools :<br> +1 : Sonar UP, discovered <br> +1 : Jenkins DOWN, was UP <br> +1 :
		 * Jira 4 was UP <br> +1 : Confluence DOWN, discovered <br> +1 : Fortify DOWN, discovered <br> +1 : vCloud DOWN,
		 * discovered <br> +1 : LDAP DOWN, discovered <br> +1 : Git DOWN, discovered <br> +1 : Subversion DOWN,
		 * discovered <br> Expected count 6 changes for subscriptions :<br> +1 : Subscription MDA - JIRA4, DOWN, was
		 * UP<br> +0 : Subscription Jupiter - JIRA6 - node has not changed, subscription is not checked<br> +1 :
		 * Subscription Jupiter - Jenkins, discovered, DOWN since node is DOWN <br> +0 : Subscription Jupiter - Sonar,
		 * discovered, node is UP, but subscription has not been checked <br> +2 : Subscription Jupiter - OpenLDAP,
		 * discovered <br> +1 : Subscription Jupiter - Confluence, discovered <br> +1 : Subscription Jupiter - Fortify,
		 * discovered <br> +1 : Subscription Jupiter - vCloud, discovered <br> +1 : Subscription Jupiter - Git, discovered
		 * <br> +1 : Subscription Jupiter - Subversion, discovered <br> +1 : Subscription Jupiter ...<br> Nb events =
		 * nbPreviousEvents + nbNodes x2 (Because one node implies one subscription + jira4/6 case) less the already
		 * know nodes<br> = nbPreviousEvents + nbNodes x2 + 1 - 1 - 1<br> = nbPreviousEvents + nbNodes x2 - 1<br>
		 */
		Assertions.assertEquals(eventsCount + nbNodes * 2 - 1, eventRepository.count());
		final var jiraEvent = eventRepository.findFirstByNodeAndTypeOrderByIdDesc(jiraNode, EventType.STATUS);
		Assertions.assertEquals(jiraNode, jiraEvent.getNode());
		Assertions.assertEquals(EventType.STATUS, jiraEvent.getType());
		Assertions.assertEquals(NodeStatus.DOWN.name(), jiraEvent.getValue());
		Assertions.assertNull(jiraEvent.getSubscription());
	}

	@Test
	void checkSubscriptionsStatus() throws Exception {
		mockApplicationContext();

		// This user sees only Jenkins nodes
		initSpringSecurityContext("user1");
		final var resource = resourceMock;
		final var eventsCount = prepareSubscriptionsEvent();
		resource.checkSubscriptionsStatus();

		/*
		 * Expected changes for instance :<br> +1 : Jenkins DOWN, was UP <br> Expected changes for subscriptions :<br>
		 * +1 : Subscription Jenkins - was UP<br>
		 */
		var expectedCount = eventsCount; // Initial amount

		// All nodes changed [(1* nb services)], but only Jenkins ones are
		// visible
		expectedCount += 2;

		Assertions.assertEquals(expectedCount, eventRepository.count());
	}

	private long prepareSubscriptionsEvent() throws Exception {
		// Check previous status
		final var eventsCount = eventRepository.count();
		Assertions.assertEquals(5, eventsCount);

		final var servicePluginLocator = resourceMock.locator;

		// Service is up --> SONAR
		final var sonar = Mockito.mock(SonarPluginResource.class);
		Mockito.when(
						servicePluginLocator.getResource(ArgumentMatchers.anyString(), ArgumentMatchers.eq(ToolPlugin.class)))
				.thenReturn(sonar);
		Mockito.when(servicePluginLocator.getResourceExpected(ArgumentMatchers.anyString(),
				ArgumentMatchers.eq(ToolPlugin.class))).thenReturn(sonar);
		Mockito.when(sonar.checkSubscriptionStatus(ArgumentMatchers.anyInt(), ArgumentMatchers.anyString(),
				ArgumentMatchers.anyMap())).thenReturn(new SubscriptionStatusWithData());
		Mockito.when(sonar.checkStatus(ArgumentMatchers.anyString(), ArgumentMatchers.anyMap())).thenReturn(true);

		// Service is down --> JIRA
		final var jira = Mockito.mock(JiraPluginResource.class);
		Mockito.when(servicePluginLocator.getResource(ArgumentMatchers.contains(":jira"),
				ArgumentMatchers.eq(ToolPlugin.class))).thenReturn(jira);
		Mockito.when(servicePluginLocator.getResourceExpected(ArgumentMatchers.contains(":jira"),
				ArgumentMatchers.eq(ToolPlugin.class))).thenReturn(jira);
		Mockito.when(jira.checkSubscriptionStatus(ArgumentMatchers.anyInt(), ArgumentMatchers.anyString(),
				ArgumentMatchers.anyMap())).thenReturn(new SubscriptionStatusWithData(false));

		// Service throw an exception --> JENKINS
		final var jenkins = Mockito.mock(JenkinsPluginResource.class);
		Mockito.when(servicePluginLocator.getResource(ArgumentMatchers.contains(":jenkins"),
				ArgumentMatchers.eq(ToolPlugin.class))).thenReturn(jenkins);
		Mockito.when(servicePluginLocator.getResourceExpected(ArgumentMatchers.contains(":jenkins"),
				ArgumentMatchers.eq(ToolPlugin.class))).thenReturn(jenkins);
		Mockito.when(jenkins.checkSubscriptionStatus(ArgumentMatchers.anyInt(), ArgumentMatchers.anyString(),
				ArgumentMatchers.anyMap())).thenThrow(new TechnicalException("junit"));

		return eventsCount;
	}

	@Test
	void checkSubscriptionsStatusScheduler() throws Exception {
		mockApplicationContext();
		initSpringSecurityContext(DEFAULT_USER);
		final var resource = resourceMock;
		final var eventsCount = prepareSubscriptionsEvent();
		resource.checkSubscriptionsStatusScheduler();

		/*
		 * Expected changes for instance :<br> +1 : Jenkins DOWN, was UP <br> +1 : Jira 4 DOWN, was UP <br> +0 : Jira 6
		 * DOWN, was already DOWN <br> +x ... other services are discovered and UP<br> Expected changes for
		 * subscriptions :<br> +1 : Subscription MDA - JIRA4, DOWN, was UP<br> +1 : Subscription Jupiter - JIRA6 - DOWN,
		 * was UP<br> +1 : Subscription Jenkins - was UP<br> +x ... other services <br>
		 */
		var expectedCount = eventsCount; // Initial amount

		// All nodes changed [(1* nb services) + 1 (LDAP*2) + 1(Source*2)
		// +1(BT*2)] but Jira6 node
		final var nbServices = resource.findAll(newUriInfo(), null, "service", null, 0).getData().size();
		expectedCount += nbServices + 1 + 1 - 1;

		// All subscriptions changed (1* nb services) + 1 (LDAP*2) + 1(Source*2)
		// +1(BT*2)
		expectedCount += nbServices + 1 + 1 + 1;
		Assertions.assertEquals(expectedCount, eventRepository.count());
	}

	@Test
	void checkSubscriptionStatusException() throws Exception {
		mockApplicationContext();
		final var resource = resourceMock;
		final var servicePluginLocator = resourceMock.locator;

		// data
		final var jiraNode = repository.findByName("JIRA 4");

		// subscription throw an exception
		final var jenkins = Mockito.mock(JenkinsPluginResource.class);
		Mockito.when(
						servicePluginLocator.getResource(ArgumentMatchers.anyString(), ArgumentMatchers.eq(ToolPlugin.class)))
				.thenReturn(jenkins);
		Mockito.when(servicePluginLocator.getResourceExpected(ArgumentMatchers.anyString(),
				ArgumentMatchers.eq(ToolPlugin.class))).thenReturn(jenkins);
		Mockito.when(jenkins.checkSubscriptionStatus(ArgumentMatchers.anyString(), ArgumentMatchers.anyMap()))
				.thenThrow(new TechnicalException("junit"));

		// check status
		final var eventsCount = eventRepository.count();
		resource.checkSubscriptionStatus(jiraNode, NodeStatus.UP);

		// 1 subscription
		Assertions.assertEquals(eventsCount + 1, eventRepository.count());
	}

	@Test
	void getServices() {
		final var resources = resource.findAll(newUriInfo(), null, "service", null, -1).getData();
		Assertions.assertEquals(10, resources.size());
		final var service = resources.get(0);
		Assertions.assertEquals(BugTrackerResource.SERVICE_KEY, service.getId());
		Assertions.assertEquals("Bug Tracker", service.getName());
		Assertions.assertNull(service.getRefined());
		Assertions.assertEquals(SubscriptionMode.LINK, service.getMode());
		Assertions.assertEquals("fa fa-bug", service.getUiClasses());

		final var service2 = resources.get(1);
		Assertions.assertEquals(BuildResource.SERVICE_KEY, service2.getId());
		Assertions.assertEquals("Build", service2.getName());
		Assertions.assertNull(service2.getRefined());
		Assertions.assertEquals(SubscriptionMode.LINK, service2.getMode());

		final var service3 = resources.get(2);
		Assertions.assertEquals(IdentityResource.SERVICE_KEY, service3.getId());
		Assertions.assertEquals("Identity management", service3.getName());
		Assertions.assertEquals("fa fa-key", service3.getUiClasses());
		Assertions.assertNull(service3.getRefined());
		Assertions.assertEquals(SubscriptionMode.CREATE, service3.getMode());

		final var service4 = resources.get(3);
		Assertions.assertEquals(KmResource.SERVICE_KEY, service4.getId());
		Assertions.assertNull(service4.getRefined());
		Assertions.assertEquals(SubscriptionMode.LINK, service4.getMode());

		final var service5 = resources.get(4);
		Assertions.assertEquals(KpiResource.SERVICE_KEY, service5.getId());
		Assertions.assertEquals("KPI Collection", service5.getName());
		Assertions.assertNull(service5.getRefined());
		Assertions.assertEquals(SubscriptionMode.LINK, service5.getMode());
	}

	@Test
	void update() {
		Assertions.assertNotNull(resource.findAll().get("service:bt:jira:6"));
		final var node = new NodeEditionVo();
		node.setId("service:bt:jira:6");
		node.setMode(SubscriptionMode.LINK);
		node.setName("Jira 7");
		node.setNode("service:bt:jira");
		resource.update(node);
		Assertions.assertTrue(repository.existsById("service:bt:jira:6"));
		final var nodeVo = resource.findAll().get("service:bt:jira:6");
		Assertions.assertNotNull(nodeVo);
		Assertions.assertEquals("Jira 7", nodeVo.getName());
		Assertions.assertEquals(SubscriptionMode.LINK, nodeVo.getMode());
		Assertions.assertEquals("service:bt:jira", nodeVo.getRefined().getId());
	}

	@Test
	void createOverflowMode() {
		final var node = new NodeEditionVo();
		node.setId("service:bt:jira:7");
		node.setMode(SubscriptionMode.CREATE);
		node.setName("Jira 7");
		node.setNode("service:bt:jira");
		Assertions.assertThrows(ValidationJsonException.class, () -> resource.create(node));
	}

	@Test
	void createOverflowModeAll() {
		final var node = new NodeEditionVo();
		node.setId("service:bt:jira:7");
		node.setMode(SubscriptionMode.ALL);
		node.setName("Jira 7");
		node.setNode("service:bt:jira");
		Assertions.assertThrows(ValidationJsonException.class, () -> resource.create(node));
	}

	@Test
	void createNoParameter() {
		Assertions.assertNull(resource.findAll().get("service:bt:jira:7"));
		final var node = new NodeEditionVo();
		node.setId("service:bt:jira:7");
		node.setMode(SubscriptionMode.LINK);
		node.setName("Jira 7");
		node.setNode("service:bt:jira");
		resource.create(node);
		Assertions.assertTrue(repository.existsById("service:bt:jira:7"));
		final var nodeVo = resource.findAll().get("service:bt:jira:7");
		Assertions.assertNotNull(nodeVo);
		Assertions.assertEquals("Jira 7", nodeVo.getName());
		Assertions.assertEquals(SubscriptionMode.LINK, nodeVo.getMode());
		Assertions.assertEquals("service:bt:jira", nodeVo.getRefined().getId());
	}

	@Test
	void create() {
		Assertions.assertNull(resource.findAll().get("service:bt:jira:some-7"));
		final var node = new NodeEditionVo();
		node.setId("service:bt:jira:some-7");
		node.setMode(SubscriptionMode.LINK);
		node.setName("Jira 7");
		node.setNode("service:bt:jira");
		final var value = new ParameterValueCreateVo();
		value.setParameter("service:bt:jira:password");
		value.setText("secret");
		node.setParameters(Collections.singletonList(value));
		resource.create(node);
		Assertions.assertTrue(repository.existsById("service:bt:jira:some-7"));
		final var nodeVo = resource.findAll().get("service:bt:jira:some-7");
		Assertions.assertNotNull(nodeVo);
		Assertions.assertEquals("Jira 7", nodeVo.getName());
		Assertions.assertEquals(SubscriptionMode.LINK, nodeVo.getMode());
		Assertions.assertEquals("service:bt:jira", nodeVo.getRefined().getId());
		Assertions.assertEquals("secret",
				parameterValueResource.getNodeParameters("service:bt:jira:some-7").get("service:bt:jira:password"));

		// Secured data
		Assertions.assertNotEquals("secret",
				parameterValueRepository.getParameterValues("service:bt:jira:some-7").get(0).getData());
	}

	@Test
	void createOverrideParameter() {
		final var nodeParameter = new ParameterValue();
		nodeParameter.setParameter(parameterRepository.findOneExpected("service:bt:jira:url"));
		nodeParameter.setNode(repository.findOneExpected("service:bt:jira"));
		nodeParameter.setData("http://localhost");
		parameterValueRepository.saveAndFlush(nodeParameter);

		Assertions.assertNull(resource.findAll().get("service:bt:jira:some-7"));
		final var node = new NodeEditionVo();
		node.setId("service:bt:jira:some-7");
		node.setMode(SubscriptionMode.LINK);
		node.setName("Jira 7");
		node.setNode("service:bt:jira");
		final var value = new ParameterValueCreateVo();
		value.setParameter("service:bt:jira:url");
		value.setText("any");
		node.setParameters(Collections.singletonList(value));
		Assertions.assertThrows(ValidationJsonException.class, () -> resource.create(node));
	}

	@Test
	void updateParameters() {
		Assertions.assertNull(resource.findAll().get("service:bt:jira:7"));
		final var node = new NodeEditionVo();
		node.setId("service:bt:jira:7");
		node.setMode(SubscriptionMode.LINK);
		node.setName("Jira 7");
		node.setNode("service:bt:jira");

		// This parameter would be untouched
		final var value = new ParameterValueCreateVo();
		value.setParameter("service:bt:jira:password");
		value.setText("secret");

		// This parameter would be deleted
		final var value3 = new ParameterValueCreateVo();
		value3.setParameter("service:bt:jira:user");
		value3.setText("secret3");

		// This parameter would be updated
		final var value4 = new ParameterValueCreateVo();
		value4.setParameter("service:bt:jira:url");
		value4.setText("http://localhost");

		// Initial node
		node.setParameters(Arrays.asList(value, value3, value4));
		resource.create(node);
		Assertions.assertTrue(repository.existsById("service:bt:jira:7"));

		// Don't touch the first secured parameter
		value.setUntouched(true);

		// Update another parameter
		value4.setText("http://remote");

		// Add a new parameter
		final var value2 = new ParameterValueCreateVo();
		value2.setParameter("service:bt:jira:jdbc-password");
		value2.setText("secret2");

		// Omit the parameter to delete (value3)
		node.setParameters(Arrays.asList(value, value2, value4));

		// Update the node : 1 untouched, 1 new, 1 added, 1 updated
		resource.update(node);

		final var parameters = parameterValueResource.getNodeParameters("service:bt:jira:7");
		Assertions.assertEquals("secret", parameters.get("service:bt:jira:password"));
		Assertions.assertEquals("http://remote", parameters.get("service:bt:jira:url"));
		Assertions.assertEquals("secret2", parameters.get("service:bt:jira:jdbc-password"));
		Assertions.assertEquals(3, parameters.size());
		final var parameterValues = parameterValueRepository.getParameterValues("service:bt:jira:7");
		Assertions.assertNotNull(parameterValues.get(0).getData());
		Assertions.assertEquals("service:bt:jira:password", parameterValues.get(0).getParameter().getId());
		Assertions.assertEquals("http://remote", parameters.get("service:bt:jira:url"));
		Assertions.assertEquals("http://remote", parameterValues.get(1).getData());
		Assertions.assertEquals("service:bt:jira:url", parameterValues.get(1).getParameter().getId());
		Assertions.assertNotNull(parameterValues.get(2).getData());
		Assertions.assertEquals("service:bt:jira:jdbc-password", parameterValues.get(2).getParameter().getId());
		Assertions.assertEquals(3, parameterValues.size());

		var nodeParameters = parameterValueResource.getNodeParameters("service:bt:jira:7", SubscriptionMode.LINK);
		Assertions.assertEquals(32, nodeParameters.size());
		Assertions.assertEquals("-secured-", nodeParameters.get(24).getText());
		Assertions.assertEquals("service:bt:jira:jdbc-password", nodeParameters.get(24).getParameter().getId());
		Assertions.assertTrue(nodeParameters.get(24).getParameter().isSecured());
		Assertions.assertEquals("-secured-", nodeParameters.get(27).getText());
		Assertions.assertEquals("service:bt:jira:password", nodeParameters.get(27).getParameter().getId());
		Assertions.assertTrue(nodeParameters.get(27).getParameter().isSecured());
		Assertions.assertEquals("http://remote", nodeParameters.get(30).getText());
		Assertions.assertEquals("service:bt:jira:url", nodeParameters.get(30).getParameter().getId());
		Assertions.assertFalse(nodeParameters.get(30).getParameter().isSecured());

		nodeParameters = parameterValueResource.getNodeParametersSecured("service:bt:jira:7", SubscriptionMode.LINK);
		Assertions.assertEquals(32, nodeParameters.size());
		Assertions.assertEquals("secret2", nodeParameters.get(24).getText());
		Assertions.assertEquals("secret", nodeParameters.get(27).getText());

		// Deleted secured (value3) is not set
		Assertions.assertNull(nodeParameters.get(31).getText());
		Assertions.assertEquals("service:bt:jira:user", nodeParameters.get(31).getParameter().getId());
		Assertions.assertTrue(nodeParameters.get(31).getParameter().isSecured());
	}

	@Test
	void updateUntouchedParameters() {
		Assertions.assertNull(resource.findAll().get("service:bt:jira:7"));
		final var node = new NodeEditionVo();
		node.setId("service:bt:jira:7");
		node.setMode(SubscriptionMode.LINK);
		node.setName("Jira 7");
		node.setNode("service:bt:jira");

		// This parameter would be untouched
		final var value = new ParameterValueCreateVo();
		value.setParameter("service:bt:jira:password");
		value.setText("secret");

		// Initial node
		node.setParameters(List.of(value));
		resource.create(node);
		Assertions.assertTrue(repository.existsById("service:bt:jira:7"));

		// Don't touch the first secured parameter
		node.setUntouchedParameters(true);

		// Update the node without providing parameters
		resource.update(node);

		final var parameters = parameterValueResource.getNodeParameters("service:bt:jira:7");
		Assertions.assertEquals("secret", parameters.get("service:bt:jira:password"));
		Assertions.assertEquals(1, parameters.size());
		final var parameterValues = parameterValueRepository.getParameterValues("service:bt:jira:7");
		Assertions.assertNotNull(parameterValues.get(0).getData());
		Assertions.assertEquals("service:bt:jira:password", parameterValues.get(0).getParameter().getId());
		Assertions.assertEquals(1, parameterValues.size());
	}

	/**
	 * The relationship is valid regarding the syntax but the parent does not exist.
	 */
	@Test
	void createNotExistRefined() {
		Assertions.assertNull(resource.findAll().get("service:bt:some:instance"));
		final var node = new NodeEditionVo();
		node.setId("service:bt:some:instance");
		node.setName("Any");
		node.setNode("service:bt:some");
		Assertions.assertThrows(BusinessException.class, () -> resource.create(node));
	}

	/**
	 * The relationship is not valid regarding the identifier syntax.
	 */
	@Test
	void createNotInvalidRefined() {
		Assertions.assertNull(resource.findAll().get("service:bt:jira:7"));
		final var node = new NodeEditionVo();
		node.setId("service:bt:jira:7");
		node.setName("Any");
		node.setNode("service:build:jenkins");
		Assertions.assertThrows(ValidationJsonException.class, () -> resource.create(node));
	}

	/**
	 * The identifier does not match to a service.
	 */
	@Test
	void createNotInvalidRoot() {
		Assertions.assertNull(resource.findAll().get("service:bt:jira:7"));
		final var node = new NodeEditionVo();
		node.setId("service:bt:jira:7");
		node.setName("Any");
		Assertions.assertThrows(ValidationJsonException.class, () -> resource.create(node));
	}

	@Test
	void createRootAllMode() {
		newNode(SubscriptionMode.ALL);
	}

	@Test
	void createOnParentAllMode() {
		newNode(SubscriptionMode.ALL);
		newSubNode(SubscriptionMode.NONE);
	}

	@Test
	void createOnParentSameMode() {
		newNode(SubscriptionMode.CREATE);
		newSubNode(SubscriptionMode.CREATE);
	}

	@Test
	void createOnParentGreaterMode() {
		newNode(SubscriptionMode.ALL);
		newSubNode(SubscriptionMode.CREATE);
	}

	@Test
	void createNoneOnParent() {
		newNode(SubscriptionMode.CREATE);
		newSubNode(SubscriptionMode.NONE);
	}

	/**
	 * Cannot create sub node of a parent node having different subscription mode different from "ALL".
	 */
	@Test
	void createOnParentDifferentMode() {
		newNode(SubscriptionMode.CREATE);
		Assertions.assertThrows(ValidationJsonException.class, () -> newSubNode(SubscriptionMode.LINK));
	}

	/**
	 * Cannot create sub node of a parent node having subscription mode "NONE".
	 */
	@Test
	void createOnParentDifferentMode2() {
		newNode(SubscriptionMode.NONE);
		Assertions.assertThrows(ValidationJsonException.class, () -> newSubNode(SubscriptionMode.CREATE));
	}

	/**
	 * Create sub node of a parent node having subscription mode "NONE".
	 */
	@Test
	void createOnParentNoneMode() {
		newNode(SubscriptionMode.NONE);
		newSubNode(SubscriptionMode.NONE);
	}

	private void newNode(final SubscriptionMode mode) {
		Assertions.assertNull(resource.findAll().get("service:some"));
		final var node = new NodeEditionVo();
		node.setId("service:some");
		node.setName("New Service");
		node.setMode(mode);
		resource.create(node);
		Assertions.assertTrue(repository.existsById("service:some"));
		final var nodeVo = resource.findAll().get("service:some");
		Assertions.assertNotNull(nodeVo);
		Assertions.assertEquals("New Service", nodeVo.getName());
		Assertions.assertFalse(nodeVo.isRefining());
	}

	private void newSubNode(SubscriptionMode mode) {
		final var node2 = new NodeEditionVo();
		node2.setId("service:some:tool");
		node2.setMode(mode);
		node2.setName("New Tool");
		node2.setNode("service:some");
		resource.create(node2);
		final var nodeVo2 = resource.findAll().get("service:some:tool");
		Assertions.assertNotNull(nodeVo2);
		Assertions.assertEquals("New Tool", nodeVo2.getName());
		Assertions.assertTrue(nodeVo2.isRefining());
	}

	@Test
	void deleteNotExist() {
		Assertions.assertThrows(BusinessException.class, () -> resource.delete("service:bt:jira:any"));
	}

	@Test
	void deleteNotVisible() {
		initSpringSecurityContext("any");
		Assertions.assertThrows(BusinessException.class, this::delete);
	}

	@Test
	void deleteHasSubscription() {
		Assertions.assertTrue(repository.existsById("service:bt:jira:6"));
		em.clear();
		Assertions.assertThrows(BusinessException.class, () -> resource.delete("service:bt:jira:6"));
	}

	@Test
	void delete() throws Exception {
		Assertions.assertTrue(repository.existsById("service:bt:jira:6"));
		subscriptionRepository.findAllBy("node.id", "service:bt:jira:6").forEach(s -> {
			eventRepository.deleteAllBy("subscription.id", s.getId());
			parameterValueRepository.deleteAllBy("subscription.id", s.getId());
			em.remove(s);
		});
		em.flush();
		em.clear();
		resource.delete("service:bt:jira:6");
		Assertions.assertFalse(repository.existsById("service:bt:jira:6"));
	}

	@Test
	void findAllByParent() {
		final var resources = resource.findAll(newUriInfo(), null, BugTrackerResource.SERVICE_KEY, null, -1).getData();
		Assertions.assertEquals(1, resources.size());
		final var service = resources.get(0);
		Assertions.assertEquals("service:bt:jira", service.getId());
		Assertions.assertEquals("JIRA", service.getName());
		Assertions.assertEquals("service:bt", service.getRefined().getId());
		Assertions.assertNull(service.getUiClasses());
	}

	@Test
	void findAllByDepth() {
		final var newUriInfo = newUriInfo();
		newUriInfo.getQueryParameters().putSingle("length", "100");

		// Service only
		Assertions.assertEquals(10, resource.findAll(newUriInfo, null, null, null, 0).getData().size());

		// Tools + Services only
		Assertions.assertEquals(21, resource.findAll(newUriInfo, null, null, null, 1).getData().size());

		// No limit : Instances + Services + instances
		Assertions.assertEquals(33, resource.findAll(newUriInfo, null, null, null, 2).getData().size());
	}

	@Test
	void findAllByParentFilterModeCreate() {
		final var resources = resource.findAll(newUriInfo(), null, LdapPluginResource.KEY, SubscriptionMode.CREATE, -1)
				.getData();
		Assertions.assertEquals(1, resources.size());
		final var service = resources.get(0);
		Assertions.assertEquals("service:id:ldap:dig", service.getId());
		Assertions.assertEquals("OpenLDAP", service.getName());
		Assertions.assertEquals("service:id:ldap", service.getRefined().getId());

		// This node accept creation
		Assertions.assertEquals(SubscriptionMode.CREATE, service.getMode());
	}

	@Test
	void findAllByParentFilterModeLinkAcceptNoCreate() {
		final var resources = resource
				.findAll(newUriInfo(), null, BugTrackerResource.SERVICE_KEY, SubscriptionMode.CREATE, 0).getData();
		Assertions.assertEquals(0, resources.size());
	}

	@Test
	void findAllByParentFilterModeLinkStrict() {
		final var resources = resource
				.findAll(newUriInfo(), null, BugTrackerResource.SERVICE_KEY, SubscriptionMode.LINK, 2).getData();
		Assertions.assertEquals(1, resources.size());
		final var service = resources.get(0);
		Assertions.assertEquals("service:bt:jira", service.getId());
		Assertions.assertEquals("JIRA", service.getName());
		Assertions.assertEquals("service:bt", service.getRefined().getId());
		Assertions.assertNull(service.getUiClasses());
	}

	@Test
	void findAllByParentFilterModeAkkAcceptLink() {
		final var resources = resource.findAll(newUriInfo(), null, "service:scm:git", SubscriptionMode.LINK, -1)
				.getData();
		Assertions.assertEquals(1, resources.size());
		final var service = resources.get(0);
		Assertions.assertEquals("service:scm:git:dig", service.getId());
		Assertions.assertEquals("git DIG", service.getName());
		Assertions.assertEquals("service:scm:git", service.getRefined().getId());
		Assertions.assertEquals("fab fa-git", service.getUiClasses());
	}

	@Test
	void findAllByParentCreateMode() {
		final var resources = resource.findAll(newUriInfo(), null, LdapPluginResource.KEY, null, -1).getData();
		Assertions.assertEquals(1, resources.size());
		final var service = resources.get(0);
		Assertions.assertEquals("service:id:ldap:dig", service.getId());
		Assertions.assertEquals("OpenLDAP", service.getName());
		Assertions.assertEquals("service:id:ldap", service.getRefined().getId());

		// This node accept creation
		Assertions.assertEquals(SubscriptionMode.CREATE, service.getMode());
	}

	@Test
	void findAllByParentMultiple() {
		final var resources = resource.findAll(newUriInfo(), null, JiraBaseResource.KEY, null, -1).getData();
		Assertions.assertEquals(2, resources.size());
		Assertions.assertEquals("service:bt:jira:4", resources.get(0).getId());
		Assertions.assertEquals("service:bt:jira:6", resources.get(1).getId());
	}

	@Test
	void getNodeStatus() {
		final var nodes = resource.getNodeStatus();
		Assertions.assertEquals(2, nodes.size());
		Assertions.assertTrue(
				nodes.get(0).getNode().getId().endsWith("build") && NodeStatus.UP.name().equals(nodes.get(0).getValue())
						|| NodeStatus.DOWN.name().equals(nodes.get(0).getValue()));
		Assertions.assertTrue(
				nodes.get(1).getNode().getId().endsWith("build") && NodeStatus.UP.name().equals(nodes.get(1).getValue())
						|| NodeStatus.DOWN.name().equals(nodes.get(1).getValue()));
		Assertions.assertEquals(EventType.STATUS, nodes.get(0).getType());
	}

	@Test
	void nodeStatus() {
		// dummy test : used to cover enum methods.
		Assertions.assertEquals(NodeStatus.UP, NodeStatus.valueOf("UP"));
		Assertions.assertEquals(2, NodeStatus.values().length);
	}

	@Test
	void getNodeStatistics() {
		final var nodes = resource.getNodeStatistics();
		// +2 Since there are 2 nodes for JIRA and 2 for source
		Assertions.assertEquals(resource.findAll(newUriInfo(), null, "service", null, 0).getData().size() + 2,
				nodes.size());
	}

	@Test
	void findSubscriptionsWithParams() {
		final var result = resource.findSubscriptionsWithParams("service:bt:jira:4");
		Assertions.assertEquals(1, result.size());
		Assertions.assertEquals(2, result.values().iterator().next().size());
	}

	@Test
	void findAll() {
		final var result = resource.findAll();
		Assertions.assertTrue(result.size() > 30);
		// Check SonarQube
		Assertions.assertEquals("service:kpi:sonar", result.get("service:kpi:sonar").getId());
		Assertions.assertEquals("SonarQube", result.get("service:kpi:sonar").getName());
		Assertions.assertEquals("service:kpi", result.get("service:kpi:sonar").getRefined().getId());
		Assertions.assertEquals("KPI Collection", result.get("service:kpi:sonar").getRefined().getName());

		// Check JIRA
		Assertions.assertEquals("service:bt:jira:6", result.get("service:bt:jira:6").getId());
		Assertions.assertEquals("JIRA 6", result.get("service:bt:jira:6").getName());
		Assertions.assertEquals(7, result.get("service:bt:jira:6").getParameters().size());
		Assertions.assertEquals("service:bt:jira", result.get("service:bt:jira:6").getRefined().getId());
		Assertions.assertEquals("JIRA", result.get("service:bt:jira:6").getRefined().getName());
		Assertions.assertEquals("service:bt", result.get("service:bt:jira:6").getRefined().getRefined().getId());
		Assertions.assertEquals("Bug Tracker", result.get("service:bt:jira:6").getRefined().getRefined().getName());
		Assertions.assertEquals("functional", result.get("service:bt:jira:6").getRefined().getRefined().getTag());
		Assertions.assertEquals("fa fa-suitcase",
				result.get("service:bt:jira:6").getRefined().getRefined().getTagUiClasses());
	}

	private UriInfo newFindAllParameters() {
		final var uriInfo = newUriInfo();
		uriInfo.getQueryParameters().add("draw", "1");
		uriInfo.getQueryParameters().add("length", "10");
		uriInfo.getQueryParameters().add("columns[0][data]", "name");
		uriInfo.getQueryParameters().add("order[0][column]", "0");
		uriInfo.getQueryParameters().add("order[0][dir]", "asc");
		return uriInfo;
	}

	@Test
	void findAllCriteria() {
		final var result = resource.findAll(newFindAllParameters(), "sonar", null, null, -1).getData();
		Assertions.assertEquals(2, result.size());
		// Check SonarQube
		Assertions.assertEquals("service:kpi:sonar", result.get(0).getId());
		Assertions.assertEquals("SonarQube", result.get(0).getName());
		Assertions.assertEquals("service:kpi", result.get(0).getRefined().getId());
		Assertions.assertEquals("KPI Collection", result.get(0).getRefined().getName());

		Assertions.assertEquals("service:kpi:sonar:bpr", result.get(1).getId());
		Assertions.assertEquals("SonarQube DIG", result.get(1).getName());
	}

	@Test
	void findAllNoCriteria() {
		final var findAll = resource.findAll(newFindAllParameters(), null, null, null, 2);
		final var result = findAll.getData();
		Assertions.assertEquals(10, result.size());
		Assertions.assertTrue(findAll.getRecordsTotal() > 30);
		Assertions.assertEquals("service:bt", result.get(0).getId());
		Assertions.assertEquals("Bug Tracker", result.get(0).getName());
	}

	@Test
	void toVoLightDisabled() {
		final var entity = new Node();
		entity.setId("disabled:node");
		final var locator = Mockito.mock(ServicePluginLocator.class);
		Assertions.assertFalse(NodeResource.toVoLight(entity, locator).getEnabled());
	}

	@Test
	void toVoLightEnabled() {
		final var entity = new Node();
		entity.setId("enabled:node");
		final var locator = Mockito.mock(ServicePluginLocator.class);
		Mockito.doReturn(true).when(locator).isEnabled("enabled:node");
		Assertions.assertTrue(NodeResource.toVoLight(entity, locator).getEnabled());
	}

	@Test
	void findByIdExpected() {
		Assertions.assertEquals("service:kpi:sonar", resource.findById("service:kpi:sonar").getId());
	}

	@Test
	void findByIdExpectedNotExists() {
		Assertions.assertThrows(ValidationJsonException.class, () -> resource.findById("service:any"));
	}

	@Test
	void findByIdExpectedNoDelegate() {
		initSpringSecurityContext("any");
		Assertions.assertThrows(ValidationJsonException.class, () -> resource.findById("service:kpi:sonar"));
	}

	@Test
	void findByIdExpectedNoValidDelegate() {
		initSpringSecurityContext("user1");
		Assertions.assertThrows(ValidationJsonException.class, () -> resource.findById("service:kpi:sonar"));
	}

	@Test
	void findByIdExpectedSubNodes() {
		initSpringSecurityContext("user1");
		Assertions.assertEquals("service:build:jenkins:bpr", resource.findById("service:build:jenkins:bpr").getId());
		Assertions.assertEquals("service:build:jenkins", resource.findById("service:build:jenkins").getId());
	}

	@Test
	void findByIdInternal() {
		initSpringSecurityContext("any");
		Assertions.assertEquals("service:build:jenkins:bpr",
				resource.findByIdInternal("service:build:jenkins:bpr").getId());
		Assertions.assertEquals("service:build:jenkins", resource.findByIdInternal("service:build:jenkins").getId());
	}

	@Test
	void findByIdInternalNotExists() {
		Assertions.assertThrows(JpaObjectRetrievalFailureException.class, () -> resource.findByIdInternal("any"));
	}

	@Test
	void deleteTasks() {
		final var sampleResource = registerSingleton("taskSampleResource",
				applicationContext.getAutowireCapableBeanFactory().createBean(TaskSampleNodeResource.class));

		try {
			final var entity = sampleResource.startTask("service:bt:jira:4", task -> task.setData("init"));
			Assertions.assertEquals("service:bt:jira:4",
					taskSampleRepository.findNotFinishedByLocked("service:bt:jira:4").getLocked().getId());

			Assertions.assertThrows(BusinessException.class, () -> sampleResource.startTask("service:bt:jira:4", task -> task.setData("init")));

			sampleResource.endTask("service:bt:jira:4", false);
			taskSampleRepository.saveAndFlush(entity);
			Assertions.assertNull(taskSampleRepository.findNotFinishedByLocked("service:bt:jira:4"));
			em.flush();
			em.clear();
			Assertions.assertEquals(1, taskSampleRepository.count());
			resource.deleteTasks(sampleResource, "service:bt:jira:4");
			Assertions.assertNull(taskSampleRepository.findNotFinishedByLocked("service:bt:jira:4"));
			Assertions.assertEquals(0, taskSampleRepository.count());
		} finally {
			destroySingleton("taskSampleResource");
		}
	}
}
