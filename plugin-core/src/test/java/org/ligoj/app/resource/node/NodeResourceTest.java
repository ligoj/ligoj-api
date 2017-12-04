package org.ligoj.app.resource.node;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.transaction.Transactional;
import javax.ws.rs.core.UriInfo;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.ligoj.app.AbstractAppTest;
import org.ligoj.app.api.NodeStatus;
import org.ligoj.app.api.NodeVo;
import org.ligoj.app.api.SubscriptionMode;
import org.ligoj.app.api.SubscriptionStatusWithData;
import org.ligoj.app.api.ToolPlugin;
import org.ligoj.app.dao.EventRepository;
import org.ligoj.app.dao.NodeRepository;
import org.ligoj.app.dao.ParameterValueRepository;
import org.ligoj.app.dao.SubscriptionRepository;
import org.ligoj.app.dao.TaskSampleNodeRepository;
import org.ligoj.app.model.DelegateNode;
import org.ligoj.app.model.Event;
import org.ligoj.app.model.EventType;
import org.ligoj.app.model.Node;
import org.ligoj.app.model.Parameter;
import org.ligoj.app.model.ParameterValue;
import org.ligoj.app.model.Project;
import org.ligoj.app.model.Subscription;
import org.ligoj.app.model.TaskSampleNode;
import org.ligoj.app.resource.ServicePluginLocator;
import org.ligoj.app.resource.node.sample.BugTrackerResource;
import org.ligoj.app.resource.node.sample.BuildResource;
import org.ligoj.app.resource.node.sample.IdentityResource;
import org.ligoj.app.resource.node.sample.JenkinsPluginResource;
import org.ligoj.app.resource.node.sample.JiraBaseResource;
import org.ligoj.app.resource.node.sample.JiraPluginResource;
import org.ligoj.app.resource.node.sample.KmResource;
import org.ligoj.app.resource.node.sample.KpiResource;
import org.ligoj.app.resource.node.sample.LdapPluginResource;
import org.ligoj.app.resource.node.sample.SonarPluginResource;
import org.ligoj.bootstrap.core.SpringUtils;
import org.ligoj.bootstrap.core.json.TableItem;
import org.ligoj.bootstrap.core.resource.BusinessException;
import org.ligoj.bootstrap.core.resource.TechnicalException;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import net.sf.ehcache.CacheManager;

/**
 * {@link NodeResource} test cases.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class NodeResourceTest extends AbstractAppTest {

	@Autowired
	private NodeRepository repository;
	@Autowired
	private SubscriptionRepository subscriptionRepository;

	@Autowired
	private NodeResource resource;

	@Autowired
	private ParameterValueResource pvResource;

	private NodeResource resourceMock;

	@Autowired
	private ParameterValueRepository parameterValueRepository;

	@Autowired
	private TaskSampleNodeRepository taskSampleRepository;

	@Autowired
	private EventRepository eventRepository;

	@Before
	public void prepare() throws IOException {
		persistEntities("csv", new Class[] { Node.class, Parameter.class, Project.class, Subscription.class, ParameterValue.class,
				Event.class, DelegateNode.class }, StandardCharsets.UTF_8.name());
		persistSystemEntities();
	}

	@Before
	@After
	public void cleanNodeCache() {
		CacheManager.getInstance().clearAll();
	}

	@SuppressWarnings("unchecked")
	public void mockApplicationContext() {
		final NodeResource resource = new NodeResource();
		super.applicationContext.getAutowireCapableBeanFactory().autowireBean(resource);
		final ApplicationContext applicationContext = Mockito.mock(ApplicationContext.class);
		SpringUtils.setSharedApplicationContext(applicationContext);
		final ServicePluginLocator servicePluginLocator = Mockito.mock(ServicePluginLocator.class);

		// Replace the plug-in locator
		resource.locator = servicePluginLocator;
		Mockito.when(applicationContext.getBean(ArgumentMatchers.any(Class.class))).thenAnswer(invocation -> {
			if (invocation.getArgument(0).equals(ServicePluginLocator.class)) {
				return servicePluginLocator;
			}
			if (invocation.getArgument(0).equals(NodeResource.class)) {
				return resource;
			}
			return super.applicationContext.getBean((Class<?>) invocation.getArgument(0));
		});
		this.resourceMock = resource;
	}

	@Test
	public void checkNodesStatusFiltered() throws Exception {

		// This users sees only Jenkins nodes
		mockApplicationContext();
		initSpringSecurityContext("user1");
		final NodeResource resource = resourceMock;

		// Mock the servers
		prepareEvent();

		// check status
		final long eventsCount = eventRepository.count();
		resource.checkNodesStatus();
		/*
		 * Expected count 5 changes for tools :<br> +1 : Jenkins DOWN, was UP <br>
		 * Expected count 6 changes for subscriptions :<br> +1 : Subscription gStack -
		 * Jenkins, discovered, DOWN since node is DOWN <br> Nb events =
		 * nbPreviousEvents + nbNodes x2 (Because one node implies one subscription)
		 * less the already know nodes<br> = nbPreviousEvents + nbNodes x2<br>
		 */
		Assert.assertEquals(eventsCount + 2, eventRepository.count());
	}

	@Test
	public void checkNodesStatus() throws Exception {

		// This users sees all nodes
		mockApplicationContext();
		initSpringSecurityContext(DEFAULT_USER);
		final NodeResource resource = resourceMock;

		// Mock the servers
		prepareEvent();

		// check status
		final long eventsCount = eventRepository.count();
		resource.checkNodesStatus();
		/*
		 * Expected count 5 changes for tools :<br> +1 : Jenkins DOWN, was UP <br>
		 * Expected count 6 changes for subscriptions :<br> +1 : Subscription gStack -
		 * Jenkins, discovered, DOWN since node is DOWN <br> Nb events =
		 * nbPreviousEvents + nbNodes x2 (Because one node implies one subscription)
		 * less the already know nodes<br> = nbPreviousEvents + nbNodes x2<br>
		 */
		Assert.assertEquals(eventsCount + 23, eventRepository.count());
	}

	@Test
	public void checkNodeStatusNotVisible() throws Exception {

		// This users sees only Jenkins nodes
		mockApplicationContext();
		initSpringSecurityContext("user1");
		final NodeResource resource = resourceMock;

		// Mock the servers
		prepareEvent();

		// check status
		final long eventsCount = eventRepository.count();

		// Not visible node
		Assert.assertNull(resource.checkNodeStatus("service:id:ldap:dig"));
		Assert.assertEquals(eventsCount, eventRepository.count());
	}

	@Test
	public void checkNodeStatus() throws Exception {
		mockApplicationContext();
		final NodeResource resource = resourceMock;

		// Mock the servers
		prepareEvent();

		// check status
		final long eventsCount = eventRepository.count();

		// Visible and down node
		Assert.assertEquals(NodeStatus.DOWN, resource.checkNodeStatus("service:id:ldap:dig"));
		Assert.assertEquals(eventsCount + 3, eventRepository.count());
	}

	@Test
	public void getNodeStatusSingleNode() throws Exception {
		mockApplicationContext();
		final NodeResource resource = resourceMock;

		// Mock the servers
		prepareEvent();

		// Visible node, but without event/check
		Assert.assertNull(resource.getNodeStatus("service:id:ldap:dig"));

		// First check to create the event
		Assert.assertEquals(NodeStatus.DOWN, resource.checkNodeStatus("service:id:ldap:dig"));

		// Visible and down node
		Assert.assertEquals(NodeStatus.DOWN, resource.getNodeStatus("service:id:ldap:dig"));
	}

	/**
	 * Mock the servers for event test
	 */
	private int prepareEvent() throws Exception {
		final ServicePluginLocator servicePluginLocator = resourceMock.locator;

		// 1 : service is down
		final JiraPluginResource jira = Mockito.mock(JiraPluginResource.class);
		Mockito.when(servicePluginLocator.getResource(ArgumentMatchers.endsWith(":jira"), ArgumentMatchers.eq(ToolPlugin.class)))
				.thenReturn(jira);
		Mockito.when(servicePluginLocator.getResourceExpected(ArgumentMatchers.endsWith(":jira"), ArgumentMatchers.eq(ToolPlugin.class)))
				.thenReturn(jira);
		Mockito.when(jira.checkStatus(ArgumentMatchers.anyString(), ArgumentMatchers.anyMap())).thenReturn(false);

		// 2 : service is up
		final SonarPluginResource sonar = Mockito.mock(SonarPluginResource.class);
		Mockito.when(servicePluginLocator.getResource(ArgumentMatchers.contains(":sonar"), ArgumentMatchers.eq(ToolPlugin.class)))
				.thenReturn(sonar);
		Mockito.when(servicePluginLocator.getResourceExpected(ArgumentMatchers.contains(":sonar"), ArgumentMatchers.eq(ToolPlugin.class)))
				.thenReturn(sonar);
		Mockito.when(sonar.checkStatus(ArgumentMatchers.anyString(), ArgumentMatchers.anyMap())).thenReturn(true);

		// 3 : service throw an exception (down)
		final JenkinsPluginResource jenkins = Mockito.mock(JenkinsPluginResource.class);
		Mockito.when(servicePluginLocator.getResource(ArgumentMatchers.contains(":jenkins"), ArgumentMatchers.eq(ToolPlugin.class)))
				.thenReturn(jenkins);
		Mockito.when(servicePluginLocator.getResourceExpected(ArgumentMatchers.contains(":jenkins"), ArgumentMatchers.eq(ToolPlugin.class)))
				.thenReturn(jenkins);
		Mockito.when(jenkins.checkStatus(ArgumentMatchers.anyString(), ArgumentMatchers.anyMap()))
				.thenThrow(new TechnicalException("junit"));

		final int nbNodes = repository.findAllInstance().size();
		Assert.assertTrue(nbNodes >= 6); // Jirax2, Confluence, LDAP, Jenkins,
											// SonarQube
		return nbNodes;
	}

	@Test
	public void checkNodesStatusScheduler() throws Exception {
		mockApplicationContext();
		final NodeResource resource = resourceMock;

		// data
		final Node jiraNode = repository.findByName("JIRA 4");
		Assert.assertFalse(jiraNode.isService());
		Assert.assertFalse(jiraNode.isTool());
		Assert.assertTrue(jiraNode.isInstance());
		Assert.assertSame(jiraNode.getRefined(), jiraNode.getTool());

		Assert.assertFalse(jiraNode.getRefined().isService());
		Assert.assertTrue(jiraNode.getRefined().isTool());
		Assert.assertFalse(jiraNode.getRefined().isInstance());
		Assert.assertSame(jiraNode.getRefined(), jiraNode.getRefined().getTool());

		Assert.assertTrue(jiraNode.getRefined().getRefined().isService());
		Assert.assertFalse(jiraNode.getRefined().getRefined().isTool());
		Assert.assertFalse(jiraNode.getRefined().getRefined().isInstance());
		Assert.assertNull(jiraNode.getRefined().getRefined().getTool());

		// Mock the servers
		final int nbNodes = prepareEvent();

		// check status
		final long eventsCount = eventRepository.count();
		resource.checkNodesStatusScheduler();
		/*
		 * Expected count 5 changes for tools :<br> +1 : Sonar UP, discovered <br> +1 :
		 * Jenkins DOWN, was UP <br> +1 : Jira 4 was UP <br> +1 : Confluence DOWN,
		 * discovered <br> +1 : Fortify DOWN, discovered <br> +1 : vCloud DOWN,
		 * discovered <br> +1 : LDAP DOWN, discovered <br> +1 : Git DOWN, discovered
		 * <br> +1 : Subversion DOWN, discovered <br> Expected count 6 changes for
		 * subscriptions :<br> +1 : Subscription MDA - JIRA4, DOWN, was UP<br> +0 :
		 * Subscription gStack - JIRA6 - node has not changed, subscription is not
		 * checked<br> +1 : Subscription gStack - Jenkins, discovered, DOWN since node
		 * is DOWN <br> +0 : Subscription gStack - Sonar, discovered, node is UP, but
		 * subscription has not been checked <br> +2 : Subscription gStack - OpenLDAP,
		 * discovered <br> +1 : Subscription gStack - Confluence, discovered <br> +1 :
		 * Subscription gStack - Fortify, discovered <br> +1 : Subscription gStack -
		 * vCloud, discovered <br> +1 : Subscription gStack - Git, discovered <br> +1 :
		 * Subscription gStack - Subversion, discovered <br> +1 : Subscription gStack
		 * ...<br> Nb events = nbPreviousEvents + nbNodes x2 (Because one node implies
		 * one subscription + jira4/6 case) less the already know nodes<br> =
		 * nbPreviousEvents + nbNodes x2 + 1 - 1 - 1<br> = nbPreviousEvents + nbNodes x2
		 * - 1<br>
		 */
		Assert.assertEquals(eventsCount + nbNodes * 2 - 1, eventRepository.count());
		final Event jiraEvent = eventRepository.findFirstByNodeAndTypeOrderByIdDesc(jiraNode, EventType.STATUS);
		Assert.assertEquals(jiraNode, jiraEvent.getNode());
		Assert.assertEquals(EventType.STATUS, jiraEvent.getType());
		Assert.assertEquals(NodeStatus.DOWN.name(), jiraEvent.getValue());
		Assert.assertNull(jiraEvent.getSubscription());
	}

	@Test
	public void checkSubscriptionsStatus() throws Exception {
		mockApplicationContext();

		// This users sees only Jenkins nodes
		initSpringSecurityContext("user1");
		final NodeResource resource = resourceMock;
		final long eventsCount = prepareSubscriptionsEvent();
		resource.checkSubscriptionsStatus();

		/*
		 * Expected changes for instance :<br> +1 : Jenkins DOWN, was UP <br> Expected
		 * changes for subscriptions :<br> +1 : Subscription Jenkins - was UP<br>
		 */
		long expectedCount = eventsCount; // Initial amount

		// All nodes changed [(1* nb services)], but only Jenkins ones are
		// visible
		expectedCount += 2;

		Assert.assertEquals(expectedCount, eventRepository.count());
	}

	private long prepareSubscriptionsEvent() throws Exception {
		// Check previous status
		final long eventsCount = eventRepository.count();
		Assert.assertEquals(5, eventsCount);

		final ServicePluginLocator servicePluginLocator = resourceMock.locator;

		// Service is up --> SONAR
		final SonarPluginResource sonar = Mockito.mock(SonarPluginResource.class);
		Mockito.when(servicePluginLocator.getResource(ArgumentMatchers.anyString(), ArgumentMatchers.eq(ToolPlugin.class)))
				.thenReturn(sonar);
		Mockito.when(servicePluginLocator.getResourceExpected(ArgumentMatchers.anyString(), ArgumentMatchers.eq(ToolPlugin.class)))
				.thenReturn(sonar);
		Mockito.when(sonar.checkSubscriptionStatus(ArgumentMatchers.anyInt(), ArgumentMatchers.anyString(), ArgumentMatchers.anyMap()))
				.thenReturn(new SubscriptionStatusWithData());
		Mockito.when(sonar.checkStatus(ArgumentMatchers.anyString(), ArgumentMatchers.anyMap())).thenReturn(true);

		// Service is down --> JIRA
		final JiraPluginResource jira = Mockito.mock(JiraPluginResource.class);
		Mockito.when(servicePluginLocator.getResource(ArgumentMatchers.contains(":jira"), ArgumentMatchers.eq(ToolPlugin.class)))
				.thenReturn(jira);
		Mockito.when(servicePluginLocator.getResourceExpected(ArgumentMatchers.contains(":jira"), ArgumentMatchers.eq(ToolPlugin.class)))
				.thenReturn(jira);
		Mockito.when(jira.checkSubscriptionStatus(ArgumentMatchers.anyInt(), ArgumentMatchers.anyString(), ArgumentMatchers.anyMap()))
				.thenReturn(new SubscriptionStatusWithData(false));

		// Service throw an exception --> JENKINS
		final JenkinsPluginResource jenkins = Mockito.mock(JenkinsPluginResource.class);
		Mockito.when(servicePluginLocator.getResource(ArgumentMatchers.contains(":jenkins"), ArgumentMatchers.eq(ToolPlugin.class)))
				.thenReturn(jenkins);
		Mockito.when(servicePluginLocator.getResourceExpected(ArgumentMatchers.contains(":jenkins"), ArgumentMatchers.eq(ToolPlugin.class)))
				.thenReturn(jenkins);
		Mockito.when(jenkins.checkSubscriptionStatus(ArgumentMatchers.anyInt(), ArgumentMatchers.anyString(), ArgumentMatchers.anyMap()))
				.thenThrow(new TechnicalException("junit"));

		return eventsCount;
	}

	@Test
	public void checkSubscriptionsStatusScheduler() throws Exception {
		mockApplicationContext();
		initSpringSecurityContext(DEFAULT_USER);
		final NodeResource resource = resourceMock;
		final long eventsCount = prepareSubscriptionsEvent();
		resource.checkSubscriptionsStatusScheduler();

		/*
		 * Expected changes for instance :<br> +1 : Jenkins DOWN, was UP <br> +1 : Jira
		 * 4 DOWN, was UP <br> +0 : Jira 6 DOWN, was already DOWN <br> +x ... other
		 * services are discovered and UP<br> Expected changes for subscriptions :<br>
		 * +1 : Subscription MDA - JIRA4, DOWN, was UP<br> +1 : Subscription gStack -
		 * JIRA6 - DOWN, was UP<br> +1 : Subscription Jenkins - was UP<br> +x ... other
		 * services <br>
		 */
		long expectedCount = eventsCount; // Initial amount

		// All nodes changed [(1* nb services) + 1 (LDAP*2) + 1(Source*2)
		// +1(BT*2)] but Jira6 node
		final int nbServices = resource.findAll(newUriInfo(), null, "service", null, 0).getData().size();
		expectedCount += nbServices + 1 + 1 - 1;

		// All subscriptions changed (1* nb services) + 1 (LDAP*2) + 1(Source*2)
		// +1(BT*2)
		expectedCount += nbServices + 1 + 1 + 1;
		Assert.assertEquals(expectedCount, eventRepository.count());
	}

	@Test
	public void checkSubscriptionStatusException() throws Exception {
		mockApplicationContext();
		final NodeResource resource = resourceMock;
		final ServicePluginLocator servicePluginLocator = resourceMock.locator;

		// data
		final Node jiraNode = repository.findByName("JIRA 4");

		// subscription throw an exception
		final JenkinsPluginResource jenkins = Mockito.mock(JenkinsPluginResource.class);
		Mockito.when(servicePluginLocator.getResource(ArgumentMatchers.anyString(), ArgumentMatchers.eq(ToolPlugin.class)))
				.thenReturn(jenkins);
		Mockito.when(servicePluginLocator.getResourceExpected(ArgumentMatchers.anyString(), ArgumentMatchers.eq(ToolPlugin.class)))
				.thenReturn(jenkins);
		Mockito.when(jenkins.checkSubscriptionStatus(ArgumentMatchers.anyString(), ArgumentMatchers.anyMap()))
				.thenThrow(new TechnicalException("junit"));

		// check status
		final long eventsCount = eventRepository.count();
		resource.checkSubscriptionStatus(jiraNode, NodeStatus.UP);

		// 1 subscription
		Assert.assertEquals(eventsCount + 1, eventRepository.count());
	}

	@Test
	public void getServices() {
		final List<NodeVo> resources = resource.findAll(newUriInfo(), null, "service", null, -1).getData();
		Assert.assertEquals(10, resources.size());
		final NodeVo service = resources.get(0);
		Assert.assertEquals(BugTrackerResource.SERVICE_KEY, service.getId());
		Assert.assertEquals("Bug Tracker", service.getName());
		Assert.assertNull(service.getRefined());
		Assert.assertEquals(SubscriptionMode.LINK, service.getMode());
		Assert.assertEquals("fa fa-bug", service.getUiClasses());

		final NodeVo service2 = resources.get(1);
		Assert.assertEquals(BuildResource.SERVICE_KEY, service2.getId());
		Assert.assertEquals("Build", service2.getName());
		Assert.assertNull(service2.getRefined());
		Assert.assertEquals(SubscriptionMode.LINK, service2.getMode());

		final NodeVo service3 = resources.get(2);
		Assert.assertEquals(IdentityResource.SERVICE_KEY, service3.getId());
		Assert.assertEquals("Identity management", service3.getName());
		Assert.assertEquals("fa fa-key", service3.getUiClasses());
		Assert.assertNull(service3.getRefined());
		Assert.assertEquals(SubscriptionMode.CREATE, service3.getMode());

		final NodeVo service4 = resources.get(3);
		Assert.assertEquals(KmResource.SERVICE_KEY, service4.getId());
		Assert.assertNull(service4.getRefined());
		Assert.assertEquals(SubscriptionMode.LINK, service4.getMode());

		final NodeVo service5 = resources.get(4);
		Assert.assertEquals(KpiResource.SERVICE_KEY, service5.getId());
		Assert.assertEquals("KPI Collection", service5.getName());
		Assert.assertNull(service5.getRefined());
		Assert.assertEquals(SubscriptionMode.LINK, service5.getMode());
	}

	@Test
	public void update() {
		Assert.assertNotNull(resource.findAll().get("service:bt:jira:6"));
		final NodeEditionVo node = new NodeEditionVo();
		node.setId("service:bt:jira:6");
		node.setMode(SubscriptionMode.LINK);
		node.setName("Jira 7");
		node.setNode("service:bt:jira");
		resource.update(node);
		Assert.assertTrue(repository.exists("service:bt:jira:6"));
		final NodeVo nodeVo = resource.findAll().get("service:bt:jira:6");
		Assert.assertNotNull(nodeVo);
		Assert.assertEquals("Jira 7", nodeVo.getName());
		Assert.assertEquals(SubscriptionMode.LINK, nodeVo.getMode());
		Assert.assertEquals("service:bt:jira", nodeVo.getRefined().getId());
	}

	@Test(expected = ValidationJsonException.class)
	public void createOverflowMode() {
		final NodeEditionVo node = new NodeEditionVo();
		node.setId("service:bt:jira:7");
		node.setMode(SubscriptionMode.CREATE);
		node.setName("Jira 7");
		node.setNode("service:bt:jira");
		resource.create(node);
	}

	@Test(expected = ValidationJsonException.class)
	public void createOverflowModeAll() {
		final NodeEditionVo node = new NodeEditionVo();
		node.setId("service:bt:jira:7");
		node.setMode(SubscriptionMode.ALL);
		node.setName("Jira 7");
		node.setNode("service:bt:jira");
		resource.create(node);
	}

	@Test
	public void createNoParameter() {
		Assert.assertNull(resource.findAll().get("service:bt:jira:7"));
		final NodeEditionVo node = new NodeEditionVo();
		node.setId("service:bt:jira:7");
		node.setMode(SubscriptionMode.LINK);
		node.setName("Jira 7");
		node.setNode("service:bt:jira");
		resource.create(node);
		Assert.assertTrue(repository.exists("service:bt:jira:7"));
		final NodeVo nodeVo = resource.findAll().get("service:bt:jira:7");
		Assert.assertNotNull(nodeVo);
		Assert.assertEquals("Jira 7", nodeVo.getName());
		Assert.assertEquals(SubscriptionMode.LINK, nodeVo.getMode());
		Assert.assertEquals("service:bt:jira", nodeVo.getRefined().getId());
	}

	@Test
	public void create() {
		Assert.assertNull(resource.findAll().get("service:bt:jira:some-7"));
		final NodeEditionVo node = new NodeEditionVo();
		node.setId("service:bt:jira:some-7");
		node.setMode(SubscriptionMode.LINK);
		node.setName("Jira 7");
		node.setNode("service:bt:jira");
		final ParameterValueCreateVo value = new ParameterValueCreateVo();
		value.setParameter("service:bt:jira:password");
		value.setText("secret");
		node.setParameters(Collections.singletonList(value));
		resource.create(node);
		Assert.assertTrue(repository.exists("service:bt:jira:some-7"));
		final NodeVo nodeVo = resource.findAll().get("service:bt:jira:some-7");
		Assert.assertNotNull(nodeVo);
		Assert.assertEquals("Jira 7", nodeVo.getName());
		Assert.assertEquals(SubscriptionMode.LINK, nodeVo.getMode());
		Assert.assertEquals("service:bt:jira", nodeVo.getRefined().getId());
		Assert.assertEquals("secret", pvResource.getNodeParameters("service:bt:jira:some-7").get("service:bt:jira:password"));

		// Secured data
		Assert.assertNotEquals("secret", parameterValueRepository.getParameterValues("service:bt:jira:some-7").get(0).getData());
	}

	@Test
	public void updateParameters() {
		Assert.assertNull(resource.findAll().get("service:bt:jira:7"));
		final NodeEditionVo node = new NodeEditionVo();
		node.setId("service:bt:jira:7");
		node.setMode(SubscriptionMode.LINK);
		node.setName("Jira 7");
		node.setNode("service:bt:jira");

		// This parameter would be untouched
		final ParameterValueCreateVo value = new ParameterValueCreateVo();
		value.setParameter("service:bt:jira:password");
		value.setText("secret");

		// This parameter would be deleted
		final ParameterValueCreateVo value3 = new ParameterValueCreateVo();
		value3.setParameter("service:bt:jira:user");
		value3.setText("secret3");

		// This parameter would be updated
		final ParameterValueCreateVo value4 = new ParameterValueCreateVo();
		value4.setParameter("service:bt:jira:url");
		value4.setText("http://localhost");

		// Initial node
		node.setParameters(Arrays.asList(value, value3, value4));
		resource.create(node);
		Assert.assertTrue(repository.exists("service:bt:jira:7"));

		// Don't touch the first secured parameter
		value.setUntouched(true);

		// Update another parameter
		value4.setText("http://remote");

		// Add a new parameter
		final ParameterValueCreateVo value2 = new ParameterValueCreateVo();
		value2.setParameter("service:bt:jira:jdbc-password");
		value2.setText("secret2");

		// Omit the parameter to delete (value3)
		node.setParameters(Arrays.asList(value, value2, value4));

		// Update the node : 1 untouched, 1 new, 1 added, 1 updated
		resource.update(node);

		final Map<String, String> parameters = pvResource.getNodeParameters("service:bt:jira:7");
		Assert.assertEquals("secret", parameters.get("service:bt:jira:password"));
		Assert.assertEquals("http://remote", parameters.get("service:bt:jira:url"));
		Assert.assertEquals("secret2", parameters.get("service:bt:jira:jdbc-password"));
		Assert.assertEquals(3, parameters.size());
		final List<ParameterValue> parameterValues = parameterValueRepository.getParameterValues("service:bt:jira:7");
		Assert.assertNotNull(parameterValues.get(0).getData());
		Assert.assertEquals("service:bt:jira:password", parameterValues.get(0).getParameter().getId());
		Assert.assertEquals("http://remote", parameters.get("service:bt:jira:url"));
		Assert.assertEquals("http://remote", parameterValues.get(1).getData());
		Assert.assertEquals("service:bt:jira:url", parameterValues.get(1).getParameter().getId());
		Assert.assertNotNull(parameterValues.get(2).getData());
		Assert.assertEquals("service:bt:jira:jdbc-password", parameterValues.get(2).getParameter().getId());
		Assert.assertEquals(3, parameterValues.size());

		final List<ParameterNodeVo> nodeParameters = pvResource.getNodeParameters("service:bt:jira:7", SubscriptionMode.LINK);
		Assert.assertEquals(32, nodeParameters.size());
		Assert.assertEquals("-secured-", nodeParameters.get(24).getText());
		Assert.assertEquals("service:bt:jira:jdbc-password", nodeParameters.get(24).getParameter().getId());
		Assert.assertTrue(nodeParameters.get(24).getParameter().isSecured());
		Assert.assertEquals("-secured-", nodeParameters.get(27).getText());
		Assert.assertEquals("service:bt:jira:password", nodeParameters.get(27).getParameter().getId());
		Assert.assertTrue(nodeParameters.get(27).getParameter().isSecured());
		Assert.assertEquals("http://remote", nodeParameters.get(30).getText());
		Assert.assertEquals("service:bt:jira:url", nodeParameters.get(30).getParameter().getId());
		Assert.assertFalse(nodeParameters.get(30).getParameter().isSecured());

		// Deleted secured (value3) is not set
		Assert.assertNull(nodeParameters.get(31).getText());
		Assert.assertEquals("service:bt:jira:user", nodeParameters.get(31).getParameter().getId());
		Assert.assertTrue(nodeParameters.get(31).getParameter().isSecured());
	}

	@Test
	public void updateUntouchParameters() {
		Assert.assertNull(resource.findAll().get("service:bt:jira:7"));
		final NodeEditionVo node = new NodeEditionVo();
		node.setId("service:bt:jira:7");
		node.setMode(SubscriptionMode.LINK);
		node.setName("Jira 7");
		node.setNode("service:bt:jira");

		// This parameter would be untouched
		final ParameterValueCreateVo value = new ParameterValueCreateVo();
		value.setParameter("service:bt:jira:password");
		value.setText("secret");

		// Initial node
		node.setParameters(Arrays.asList(value));
		resource.create(node);
		Assert.assertTrue(repository.exists("service:bt:jira:7"));

		// Don't touch the first secured parameter
		node.setUntouchedParameters(true);

		// Update the node without providing parameters
		resource.update(node);

		final Map<String, String> parameters = pvResource.getNodeParameters("service:bt:jira:7");
		Assert.assertEquals("secret", parameters.get("service:bt:jira:password"));
		Assert.assertEquals(1, parameters.size());
		final List<ParameterValue> parameterValues = parameterValueRepository.getParameterValues("service:bt:jira:7");
		Assert.assertNotNull(parameterValues.get(0).getData());
		Assert.assertEquals("service:bt:jira:password", parameterValues.get(0).getParameter().getId());
		Assert.assertEquals(1, parameterValues.size());
	}

	/**
	 * The relationship is valid regarding the syntax but the parent does not exist.
	 */
	@Test(expected = BusinessException.class)
	public void createNotExistRefined() {
		Assert.assertNull(resource.findAll().get("service:bt:some:instance"));
		final NodeEditionVo node = new NodeEditionVo();
		node.setId("service:bt:some:instance");
		node.setName("Any");
		node.setNode("service:bt:some");
		resource.create(node);
	}

	/**
	 * The relationship is not valid regarding the identifier syntax.
	 */
	@Test(expected = ValidationJsonException.class)
	public void createNotInvalidRefined() {
		Assert.assertNull(resource.findAll().get("service:bt:jira:7"));
		final NodeEditionVo node = new NodeEditionVo();
		node.setId("service:bt:jira:7");
		node.setName("Any");
		node.setNode("service:build:jenkins");
		resource.create(node);
	}

	/**
	 * The identifier does not match to a service.
	 */
	@Test(expected = ValidationJsonException.class)
	public void createNotInvalidRoot() {
		Assert.assertNull(resource.findAll().get("service:bt:jira:7"));
		final NodeEditionVo node = new NodeEditionVo();
		node.setId("service:bt:jira:7");
		node.setName("Any");
		resource.create(node);
	}

	@Test
	public void createRootAllMode() {
		newNode(SubscriptionMode.ALL);
	}

	@Test
	public void createOnParentAllMode() {
		newNode(SubscriptionMode.ALL);
		newSubNode(SubscriptionMode.NONE);
	}

	@Test
	public void createOnParentSameMode() {
		newNode(SubscriptionMode.CREATE);
		newSubNode(SubscriptionMode.CREATE);
	}

	@Test
	public void createOnParentGreaterMode() {
		newNode(SubscriptionMode.ALL);
		newSubNode(SubscriptionMode.CREATE);
	}

	@Test
	public void createNoneOnParent() {
		newNode(SubscriptionMode.CREATE);
		newSubNode(SubscriptionMode.NONE);
	}

	/**
	 * Cannot create sub node of a parent node having different subscription mode
	 * different from "ALL".
	 */
	@Test(expected = ValidationJsonException.class)
	public void createOnParentDifferentMode() {
		newNode(SubscriptionMode.CREATE);
		newSubNode(SubscriptionMode.LINK);
	}

	/**
	 * Cannot create sub node of a parent node having subscription mode "NONE".
	 */
	@Test(expected = ValidationJsonException.class)
	public void createOnParentDifferentMode2() {
		newNode(SubscriptionMode.NONE);
		newSubNode(SubscriptionMode.CREATE);
	}

	/**
	 * Cannot create sub node of a parent node having subscription mode "NONE".
	 */
	@Test(expected = ValidationJsonException.class)
	public void createOnParentNoneMode() {
		newNode(SubscriptionMode.NONE);
		newSubNode(SubscriptionMode.NONE);
	}

	private void newNode(final SubscriptionMode mode) {
		Assert.assertNull(resource.findAll().get("service:some"));
		final NodeEditionVo node = new NodeEditionVo();
		node.setId("service:some");
		node.setName("New Service");
		node.setMode(mode);
		resource.create(node);
		Assert.assertTrue(repository.exists("service:some"));
		final NodeVo nodeVo = resource.findAll().get("service:some");
		Assert.assertNotNull(nodeVo);
		Assert.assertEquals("New Service", nodeVo.getName());
		Assert.assertFalse(nodeVo.isRefining());
	}

	private void newSubNode(SubscriptionMode mode) {
		final NodeEditionVo node2 = new NodeEditionVo();
		node2.setId("service:some:tool");
		node2.setMode(mode);
		node2.setName("New Tool");
		node2.setNode("service:some");
		resource.create(node2);
		final NodeVo nodeVo2 = resource.findAll().get("service:some:tool");
		Assert.assertNotNull(nodeVo2);
		Assert.assertEquals("New Tool", nodeVo2.getName());
		Assert.assertTrue(nodeVo2.isRefining());
	}

	@Test(expected = BusinessException.class)
	public void deleteNotExist() throws Exception {
		resource.delete("service:bt:jira:any");
	}

	@Test(expected = BusinessException.class)
	public void deleteNotVisible() throws Exception {
		initSpringSecurityContext("any");
		delete();
	}

	@Test(expected = BusinessException.class)
	public void deleteHasSubscription() throws Exception {
		Assert.assertTrue(repository.exists("service:bt:jira:6"));
		em.clear();
		resource.delete("service:bt:jira:6");
	}

	@Test
	public void delete() throws Exception {
		Assert.assertTrue(repository.exists("service:bt:jira:6"));
		subscriptionRepository.findAllBy("node.id", "service:bt:jira:6").forEach(s -> {
			eventRepository.deleteAllBy("subscription.id", s.getId());
			parameterValueRepository.deleteAllBy("subscription.id", s.getId());
			em.remove(s);
		});
		em.flush();
		em.clear();
		resource.delete("service:bt:jira:6");
		Assert.assertFalse(repository.exists("service:bt:jira:6"));
	}

	@Test
	public void findAllByParent() {
		final List<NodeVo> resources = resource.findAll(newUriInfo(), null, BugTrackerResource.SERVICE_KEY, null, -1).getData();
		Assert.assertEquals(1, resources.size());
		final NodeVo service = resources.get(0);
		Assert.assertEquals("service:bt:jira", service.getId());
		Assert.assertEquals("JIRA", service.getName());
		Assert.assertEquals("service:bt", service.getRefined().getId());
		Assert.assertNull(service.getUiClasses());
	}

	@Test
	public void findAllByDepth() {
		final UriInfo newUriInfo = newUriInfo();
		newUriInfo.getQueryParameters().putSingle("length", "100");

		// Service only
		Assert.assertEquals(10, resource.findAll(newUriInfo, null, null, null, 0).getData().size());

		// Tools + Services only
		Assert.assertEquals(21, resource.findAll(newUriInfo, null, null, null, 1).getData().size());

		// No limit : Instances + Services + instances
		Assert.assertEquals(33, resource.findAll(newUriInfo, null, null, null, 2).getData().size());
	}

	@Test
	public void findAllByParentFilterModeCreate() {
		final List<NodeVo> resources = resource.findAll(newUriInfo(), null, LdapPluginResource.KEY, SubscriptionMode.CREATE, -1).getData();
		Assert.assertEquals(1, resources.size());
		final NodeVo service = resources.get(0);
		Assert.assertEquals("service:id:ldap:dig", service.getId());
		Assert.assertEquals("OpenLDAP", service.getName());
		Assert.assertEquals("service:id:ldap", service.getRefined().getId());

		// This node accept creation
		Assert.assertEquals(SubscriptionMode.CREATE, service.getMode());
	}

	@Test
	public void findAllByParentFilterModeLinkAcceptNoCreate() {
		final List<NodeVo> resources = resource.findAll(newUriInfo(), null, BugTrackerResource.SERVICE_KEY, SubscriptionMode.CREATE, 0)
				.getData();
		Assert.assertEquals(0, resources.size());
	}

	@Test
	public void findAllByParentFilterModeLinkStrict() {
		final List<NodeVo> resources = resource.findAll(newUriInfo(), null, BugTrackerResource.SERVICE_KEY, SubscriptionMode.LINK, 2)
				.getData();
		Assert.assertEquals(1, resources.size());
		final NodeVo service = resources.get(0);
		Assert.assertEquals("service:bt:jira", service.getId());
		Assert.assertEquals("JIRA", service.getName());
		Assert.assertEquals("service:bt", service.getRefined().getId());
		Assert.assertNull(service.getUiClasses());
	}

	@Test
	public void findAllByParentFilterModeAkkAcceptLink() {
		final List<NodeVo> resources = resource.findAll(newUriInfo(), null, "service:scm:git", SubscriptionMode.LINK, -1).getData();
		Assert.assertEquals(1, resources.size());
		final NodeVo service = resources.get(0);
		Assert.assertEquals("service:scm:git:dig", service.getId());
		Assert.assertEquals("git DIG", service.getName());
		Assert.assertEquals("service:scm:git", service.getRefined().getId());
		Assert.assertEquals("fa fa-git", service.getUiClasses());
	}

	@Test
	public void findAllByParentCreateMode() {
		final List<NodeVo> resources = resource.findAll(newUriInfo(), null, LdapPluginResource.KEY, null, -1).getData();
		Assert.assertEquals(1, resources.size());
		final NodeVo service = resources.get(0);
		Assert.assertEquals("service:id:ldap:dig", service.getId());
		Assert.assertEquals("OpenLDAP", service.getName());
		Assert.assertEquals("service:id:ldap", service.getRefined().getId());

		// This node accept creation
		Assert.assertEquals(SubscriptionMode.CREATE, service.getMode());
	}

	@Test
	public void findAllByParentMutiple() {
		final List<NodeVo> resources = resource.findAll(newUriInfo(), null, JiraBaseResource.KEY, null, -1).getData();
		Assert.assertEquals(2, resources.size());
		Assert.assertEquals("service:bt:jira:4", resources.get(0).getId());
		Assert.assertEquals("service:bt:jira:6", resources.get(1).getId());
	}

	@Test
	public void getNodeStatus() throws Exception {
		final List<EventVo> nodes = resource.getNodeStatus();
		Assert.assertEquals(2, nodes.size());
		Assert.assertTrue(nodes.get(0).getNode().getId().endsWith("build") && NodeStatus.UP.name().equals(nodes.get(0).getValue())
				|| NodeStatus.DOWN.name().equals(nodes.get(0).getValue()));
		Assert.assertTrue(nodes.get(1).getNode().getId().endsWith("build") && NodeStatus.UP.name().equals(nodes.get(1).getValue())
				|| NodeStatus.DOWN.name().equals(nodes.get(1).getValue()));
		Assert.assertEquals(EventType.STATUS, nodes.get(0).getType());
	}

	@Test
	public void nodeStatus() throws Exception {
		// dummy test : used to cover enum methods.
		Assert.assertEquals(NodeStatus.UP, NodeStatus.valueOf("UP"));
		Assert.assertEquals(2, NodeStatus.values().length);
	}

	@Test
	public void getNodeStatistics() throws Exception {
		final List<NodeStatisticsVo> nodes = resource.getNodeStatistics();
		// +2 Since there are 2 nodes for JIRA and 2 for source
		Assert.assertEquals(resource.findAll(newUriInfo(), null, "service", null, 0).getData().size() + 2, nodes.size());
	}

	@Test
	public void findSubscriptionsWithParams() {
		final Map<Subscription, Map<String, String>> result = resource.findSubscriptionsWithParams("service:bt:jira:4");
		Assert.assertEquals(1, result.size());
		Assert.assertEquals(2, result.values().iterator().next().size());
	}

	@Test
	public void findAll() {
		final Map<String, NodeVo> result = resource.findAll();
		Assert.assertTrue(result.size() > 30);
		// Check SonarQube
		Assert.assertEquals("service:kpi:sonar", result.get("service:kpi:sonar").getId());
		Assert.assertEquals("SonarQube", result.get("service:kpi:sonar").getName());
		Assert.assertEquals("service:kpi", result.get("service:kpi:sonar").getRefined().getId());
		Assert.assertEquals("KPI Collection", result.get("service:kpi:sonar").getRefined().getName());

		// Check JIRA
		Assert.assertEquals("service:bt:jira:6", result.get("service:bt:jira:6").getId());
		Assert.assertEquals("JIRA 6", result.get("service:bt:jira:6").getName());
		Assert.assertEquals(7, result.get("service:bt:jira:6").getParameters().size());
		Assert.assertEquals("service:bt:jira", result.get("service:bt:jira:6").getRefined().getId());
		Assert.assertEquals("JIRA", result.get("service:bt:jira:6").getRefined().getName());
		Assert.assertEquals("service:bt", result.get("service:bt:jira:6").getRefined().getRefined().getId());
		Assert.assertEquals("Bug Tracker", result.get("service:bt:jira:6").getRefined().getRefined().getName());
		Assert.assertEquals("functional", result.get("service:bt:jira:6").getRefined().getRefined().getTag());
		Assert.assertEquals("fa fa-suitcase", result.get("service:bt:jira:6").getRefined().getRefined().getTagUiClasses());
	}

	private UriInfo newFindAllParameters() {
		final UriInfo uriInfo = newUriInfo();
		uriInfo.getQueryParameters().add("draw", "1");
		uriInfo.getQueryParameters().add("length", "10");
		uriInfo.getQueryParameters().add("columns[0][data]", "name");
		uriInfo.getQueryParameters().add("order[0][column]", "0");
		uriInfo.getQueryParameters().add("order[0][dir]", "asc");
		return uriInfo;
	}

	@Test
	public void findAllCriteria() {
		final List<NodeVo> result = resource.findAll(newFindAllParameters(), "sonar", null, null, -1).getData();
		Assert.assertEquals(2, result.size());
		// Check SonarQube
		Assert.assertEquals("service:kpi:sonar", result.get(0).getId());
		Assert.assertEquals("SonarQube", result.get(0).getName());
		Assert.assertEquals("service:kpi", result.get(0).getRefined().getId());
		Assert.assertEquals("KPI Collection", result.get(0).getRefined().getName());

		Assert.assertEquals("service:kpi:sonar:bpr", result.get(1).getId());
		Assert.assertEquals("SonarQube DIG", result.get(1).getName());
	}

	@Test
	public void findAllNoCriteria() {
		final TableItem<NodeVo> findAll = resource.findAll(newFindAllParameters(), null, null, null, 2);
		final List<NodeVo> result = findAll.getData();
		Assert.assertEquals(10, result.size());
		Assert.assertTrue(findAll.getRecordsTotal() > 30);
		Assert.assertEquals("service:bt", result.get(0).getId());
		Assert.assertEquals("Bug Tracker", result.get(0).getName());
	}

	@Test
	public void findByIdExpected() {
		Assert.assertEquals("service:kpi:sonar", resource.findById("service:kpi:sonar").getId());
	}

	@Test(expected = ValidationJsonException.class)
	public void findByIdExpectedNotExists() {
		resource.findById("service:any");
	}

	@Test(expected = ValidationJsonException.class)
	public void findByIdExpectedNoDelegate() {
		initSpringSecurityContext("any");
		resource.findById("service:kpi:sonar");
	}

	@Test(expected = ValidationJsonException.class)
	public void findByIdExpectedNoValidDelegate() {
		initSpringSecurityContext("user1");
		resource.findById("service:kpi:sonar");
	}

	@Test
	public void findByIdExpectedSubNodes() {
		initSpringSecurityContext("user1");
		Assert.assertEquals("service:build:jenkins:bpr", resource.findById("service:build:jenkins:bpr").getId());
		Assert.assertEquals("service:build:jenkins", resource.findById("service:build:jenkins").getId());
	}

	@Test
	public void findByIdInternal() {
		initSpringSecurityContext("any");
		Assert.assertEquals("service:build:jenkins:bpr", resource.findByIdInternal("service:build:jenkins:bpr").getId());
		Assert.assertEquals("service:build:jenkins", resource.findByIdInternal("service:build:jenkins").getId());
	}

	@Test(expected = JpaObjectRetrievalFailureException.class)
	public void findByIdInternalNotExists() {
		resource.findByIdInternal("any");
	}

	@Test
	public void deleteTasks() throws Exception {
		final TaskSampleNodeResource sampleResource = registerSingleton("taskSampleResource",
				applicationContext.getAutowireCapableBeanFactory().createBean(TaskSampleNodeResource.class));

		try {
			final TaskSampleNode entity = sampleResource.startTask("service:bt:jira:4", task -> task.setData("init"));
			Assert.assertEquals("service:bt:jira:4", taskSampleRepository.findNotFinishedByLocked("service:bt:jira:4").getLocked().getId());

			try {
				sampleResource.startTask("service:bt:jira:4", task -> task.setData("init"));
				Assert.fail();
			} catch (BusinessException e) {
				// ignore, as expected
			}

			sampleResource.endTask("service:bt:jira:4", false);
			taskSampleRepository.saveAndFlush(entity);
			Assert.assertNull(taskSampleRepository.findNotFinishedByLocked("service:bt:jira:4"));
			em.flush();
			em.clear();
			Assert.assertEquals(1, taskSampleRepository.count());
			resource.deleteTasks(sampleResource, "service:bt:jira:4");
			Assert.assertNull(taskSampleRepository.findNotFinishedByLocked("service:bt:jira:4"));
			Assert.assertEquals(0, taskSampleRepository.count());
		} finally {
			destroySingleton("taskSampleResource");
		}
	}
}
