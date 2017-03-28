package org.ligoj.app.resource.node;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.transaction.Transactional;
import javax.ws.rs.core.UriInfo;

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
import org.ligoj.app.model.DelegateNode;
import org.ligoj.app.model.Event;
import org.ligoj.app.model.EventType;
import org.ligoj.app.model.Node;
import org.ligoj.app.model.Parameter;
import org.ligoj.app.model.ParameterType;
import org.ligoj.app.model.ParameterValue;
import org.ligoj.app.model.Project;
import org.ligoj.app.model.Subscription;
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
	private NodeResource resource;

	private NodeResource resourceMock;

	@Autowired
	private EventRepository eventRepository;

	@Before
	public void prepare() throws IOException {
		persistEntities("csv",
				new Class[] { Node.class, Parameter.class, Project.class, Subscription.class, ParameterValue.class, Event.class, DelegateNode.class },
				StandardCharsets.UTF_8.name());
	}

	@SuppressWarnings("unchecked")
	@Before
	public void mockApplicationContext() {
		final NodeResource resource = new NodeResource();
		super.applicationContext.getAutowireCapableBeanFactory().autowireBean(resource);
		final ApplicationContext applicationContext = Mockito.mock(ApplicationContext.class);
		SpringUtils.setSharedApplicationContext(applicationContext);
		final ServicePluginLocator servicePluginLocator = Mockito.mock(ServicePluginLocator.class);

		// Replace the plugin locator
		resource.servicePluginLocator = servicePluginLocator;
		Mockito.when(applicationContext.getBean(ArgumentMatchers.any(Class.class))).thenAnswer(invocation -> {
			final Class<?> requiredType = (Class<Object>) invocation.getArguments()[0];
			if (requiredType == NodeResource.class) {
				return resource;
			}
			if (requiredType == ServicePluginLocator.class) {
				return servicePluginLocator;
			}
			return NodeResourceTest.super.applicationContext.getBean(requiredType);
		});
		this.resourceMock = resource;
	}

	@Test
	public void checkNodesStatusFiltered() throws Exception {

		// This users sees only Jenkins nodes
		initSpringSecurityContext("user1");
		final NodeResource resource = resourceMock;

		// Mock the servers
		prepareEvent();

		// check status
		final long eventsCount = eventRepository.count();
		resource.checkNodesStatus();
		/*
		 * Expected count 5 changes for tools :<br>
		 * +1 : Jenkins DOWN, was UP <br>
		 * Expected count 6 changes for subscriptions :<br>
		 * +1 : Subscription gStack - Jenkins, discovered, DOWN since node is DOWN <br>
		 * Nb events = nbPreviousEvents + nbNodes x2 (Because one node implies one subscription) less the already know
		 * nodes<br>
		 * = nbPreviousEvents + nbNodes x2<br>
		 */
		Assert.assertEquals(eventsCount + 2, eventRepository.count());
	}

	@Test
	public void checkNodesStatus() throws Exception {

		// This users sees all nodes
		initSpringSecurityContext(DEFAULT_USER);
		final NodeResource resource = resourceMock;

		// Mock the servers
		prepareEvent();

		// check status
		final long eventsCount = eventRepository.count();
		resource.checkNodesStatus();
		/*
		 * Expected count 5 changes for tools :<br>
		 * +1 : Jenkins DOWN, was UP <br>
		 * Expected count 6 changes for subscriptions :<br>
		 * +1 : Subscription gStack - Jenkins, discovered, DOWN since node is DOWN <br>
		 * Nb events = nbPreviousEvents + nbNodes x2 (Because one node implies one subscription) less the already know
		 * nodes<br>
		 * = nbPreviousEvents + nbNodes x2<br>
		 */
		Assert.assertEquals(eventsCount + 23, eventRepository.count());
	}

	@Test
	public void checkNodeStatus() throws Exception {

		// This users sees only Jenkins nodes
		initSpringSecurityContext("user1");
		final NodeResource resource = resourceMock;

		// Mock the servers
		prepareEvent();

		// check status
		final long eventsCount = eventRepository.count();
		resource.checkNodeStatus("service:id:ldap:dig");
		Assert.assertEquals(eventsCount, eventRepository.count());
	}

	/**
	 * Mock the servers for event test
	 */
	private int prepareEvent() throws Exception {
		final ServicePluginLocator servicePluginLocator = resourceMock.servicePluginLocator;

		// 1 : service is down
		final JiraPluginResource jira = Mockito.mock(JiraPluginResource.class);
		Mockito.when(servicePluginLocator.getResource(ArgumentMatchers.endsWith(":jira"), ArgumentMatchers.eq(ToolPlugin.class))).thenReturn(jira);
		Mockito.when(servicePluginLocator.getResourceExpected(ArgumentMatchers.endsWith(":jira"), ArgumentMatchers.eq(ToolPlugin.class)))
				.thenReturn(jira);
		Mockito.when(jira.checkStatus(ArgumentMatchers.anyString(), ArgumentMatchers.anyMap())).thenReturn(false);

		// 2 : service is up
		final SonarPluginResource sonar = Mockito.mock(SonarPluginResource.class);
		Mockito.when(servicePluginLocator.getResource(ArgumentMatchers.contains(":sonar"), ArgumentMatchers.eq(ToolPlugin.class))).thenReturn(sonar);
		Mockito.when(servicePluginLocator.getResourceExpected(ArgumentMatchers.contains(":sonar"), ArgumentMatchers.eq(ToolPlugin.class)))
				.thenReturn(sonar);
		Mockito.when(sonar.checkStatus(ArgumentMatchers.anyString(), ArgumentMatchers.anyMap())).thenReturn(true);

		// 3 : service throw an exception (down)
		final JenkinsPluginResource jenkins = Mockito.mock(JenkinsPluginResource.class);
		Mockito.when(servicePluginLocator.getResource(ArgumentMatchers.contains(":jenkins"), ArgumentMatchers.eq(ToolPlugin.class)))
				.thenReturn(jenkins);
		Mockito.when(servicePluginLocator.getResourceExpected(ArgumentMatchers.contains(":jenkins"), ArgumentMatchers.eq(ToolPlugin.class)))
				.thenReturn(jenkins);
		Mockito.when(jenkins.checkStatus(ArgumentMatchers.anyString(), ArgumentMatchers.anyMap())).thenThrow(new TechnicalException("junit"));

		final int nbNodes = repository.findAllInstance().size();
		Assert.assertTrue(nbNodes >= 6); // Jirax2, Confluence, LDAP, Jenkins, SonarQube
		return nbNodes;
	}

	@Test
	public void checkNodesStatusScheduler() throws Exception {
		final NodeResource resource = resourceMock;

		// data
		final Node jiraNode = repository.findByName("JIRA 4");

		// Mock the servers
		final int nbNodes = prepareEvent();

		// check status
		final long eventsCount = eventRepository.count();
		resource.checkNodesStatusScheduler();
		/*
		 * Expected count 5 changes for tools :<br>
		 * +1 : Sonar UP, discovered <br>
		 * +1 : Jenkins DOWN, was UP <br>
		 * +1 : Jira 4 was UP <br>
		 * +1 : Confluence DOWN, discovered <br>
		 * +1 : Fortify DOWN, discovered <br>
		 * +1 : vCloud DOWN, discovered <br>
		 * +1 : LDAP DOWN, discovered <br>
		 * +1 : Git DOWN, discovered <br>
		 * +1 : Subversion DOWN, discovered <br>
		 * Expected count 6 changes for subscriptions :<br>
		 * +1 : Subscription MDA - JIRA4, DOWN, was UP<br>
		 * +0 : Subscription gStack - JIRA6 - node has not changed, subscription is not checked<br>
		 * +1 : Subscription gStack - Jenkins, discovered, DOWN since node is DOWN <br>
		 * +0 : Subscription gStack - Sonar, discovered, node is UP, but subscription has not been checked <br>
		 * +2 : Subscription gStack - OpenLDAP, discovered <br>
		 * +1 : Subscription gStack - Confluence, discovered <br>
		 * +1 : Subscription gStack - Fortify, discovered <br>
		 * +1 : Subscription gStack - vCloud, discovered <br>
		 * +1 : Subscription gStack - Git, discovered <br>
		 * +1 : Subscription gStack - Subversion, discovered <br>
		 * +1 : Subscription gStack ...<br>
		 * Nb events = nbPreviousEvents + nbNodes x2 (Because one node implies one subscription + jira4/6 case) less the
		 * already know nodes<br>
		 * = nbPreviousEvents + nbNodes x2 + 1 - 1 - 1<br>
		 * = nbPreviousEvents + nbNodes x2 - 1<br>
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

		// This users sees only Jenkins nodes
		initSpringSecurityContext("user1");
		final NodeResource resource = resourceMock;
		final long eventsCount = prepareSubscriptionsEvent();
		resource.checkSubscriptionsStatus();

		/*
		 * Expected changes for instance :<br>
		 * +1 : Jenkins DOWN, was UP <br>
		 * Expected changes for subscriptions :<br>
		 * +1 : Subscription Jenkins - was UP<br>
		 */
		long expectedCount = eventsCount; // Initial amount

		// All nodes changed [(1* nb services)], but only Jenkins ones are visible
		expectedCount += 2;

		Assert.assertEquals(expectedCount, eventRepository.count());
	}

	private long prepareSubscriptionsEvent() throws Exception {
		final ServicePluginLocator servicePluginLocator = resourceMock.servicePluginLocator;

		// 3 : service is up --> all services and all subscriptions
		final SonarPluginResource sonar = Mockito.mock(SonarPluginResource.class);
		Mockito.when(servicePluginLocator.getResource(ArgumentMatchers.anyString(), ArgumentMatchers.eq(ToolPlugin.class))).thenReturn(sonar);
		Mockito.when(servicePluginLocator.getResourceExpected(ArgumentMatchers.anyString(), ArgumentMatchers.eq(ToolPlugin.class))).thenReturn(sonar);
		Mockito.when(sonar.checkSubscriptionStatus(ArgumentMatchers.anyString(), ArgumentMatchers.anyMap()))
				.thenReturn(new SubscriptionStatusWithData());
		Mockito.when(sonar.checkStatus(ArgumentMatchers.anyString(), ArgumentMatchers.anyMap())).thenReturn(true);

		// 1 : service is down --> all Jira instances
		final JiraPluginResource jira = Mockito.mock(JiraPluginResource.class);
		Mockito.when(servicePluginLocator.getResource(ArgumentMatchers.endsWith("jira"), ArgumentMatchers.eq(ToolPlugin.class))).thenReturn(jira);
		Mockito.when(servicePluginLocator.getResourceExpected(ArgumentMatchers.endsWith("jira"), ArgumentMatchers.eq(ToolPlugin.class)))
				.thenReturn(jira);
		Mockito.when(jira.checkSubscriptionStatus(ArgumentMatchers.anyString(), ArgumentMatchers.anyMap()))
				.thenReturn(new SubscriptionStatusWithData(false));

		// 2 : service throw an exception --> Jenkins
		final JenkinsPluginResource jenkins = Mockito.mock(JenkinsPluginResource.class);
		Mockito.when(servicePluginLocator.getResource(ArgumentMatchers.contains("jenkins"), ArgumentMatchers.eq(ToolPlugin.class)))
				.thenReturn(jenkins);
		Mockito.when(servicePluginLocator.getResourceExpected(ArgumentMatchers.contains("jenkins"), ArgumentMatchers.eq(ToolPlugin.class)))
				.thenReturn(jenkins);
		Mockito.when(jenkins.checkSubscriptionStatus(ArgumentMatchers.anyString(), ArgumentMatchers.anyMap()))
				.thenThrow(new TechnicalException("junit"));

		// check status
		final long eventsCount = eventRepository.count();
		Assert.assertEquals(5, eventsCount);

		return eventsCount;
	}

	@Test
	public void checkSubscriptionsStatusScheduler() throws Exception {
		initSpringSecurityContext(DEFAULT_USER);
		final NodeResource resource = resourceMock;
		final long eventsCount = prepareSubscriptionsEvent();
		resource.checkSubscriptionsStatusScheduler();

		/*
		 * Expected changes for instance :<br>
		 * +1 : Jenkins DOWN, was UP <br>
		 * +1 : Jira 4 DOWN, was UP <br>
		 * +0 : Jira 6 DOWN, was already DOWN <br>
		 * +x ... other services are discovered and UP<br>
		 * Expected changes for subscriptions :<br>
		 * +1 : Subscription MDA - JIRA4, DOWN, was UP<br>
		 * +1 : Subscription gStack - JIRA6 - DOWN, was UP<br>
		 * +1 : Subscription Jenkins - was UP<br>
		 * +x ... other services <br>
		 */
		long expectedCount = eventsCount; // Initial amount

		// All nodes changed [(1* nb services) + 1 (LDAP*2) + 1(Source*2) +1(BT*2)] but Jira6 node
		expectedCount += resource.findAllNoParent().size() + 1 + 1 - 1;

		// All subscriptions changed (1* nb services) + 1 (LDAP*2) + 1(Source*2) +1(BT*2)
		expectedCount += resource.findAllNoParent().size() + 1 + 1 + 1;
		Assert.assertEquals(expectedCount, eventRepository.count());
	}

	@Test
	public void checkSubscriptionStatusException() throws Exception {
		final NodeResource resource = resourceMock;
		final ServicePluginLocator servicePluginLocator = resourceMock.servicePluginLocator;

		// data
		final Node jiraNode = repository.findByName("JIRA 4");

		// subscription throw an exception
		final JenkinsPluginResource jenkins = Mockito.mock(JenkinsPluginResource.class);
		Mockito.when(servicePluginLocator.getResource(ArgumentMatchers.anyString(), ArgumentMatchers.eq(ToolPlugin.class))).thenReturn(jenkins);
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
		final List<NodeVo> resources = resource.findAllNoParent();
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
	public void findAllByParent() {
		final List<NodeVo> resources = resource.findAllByParent(BugTrackerResource.SERVICE_KEY);
		Assert.assertEquals(1, resources.size());
		final NodeVo service = resources.get(0);
		Assert.assertEquals("service:bt:jira", service.getId());
		Assert.assertEquals("JIRA", service.getName());
		Assert.assertNull(service.getRefined());
		Assert.assertNull(service.getUiClasses());
	}

	@Test
	public void findAllByParentFilterModeCreate() {
		final List<NodeVo> resources = resource.findAllByParent(LdapPluginResource.KEY, SubscriptionMode.CREATE);
		Assert.assertEquals(1, resources.size());
		final NodeVo service = resources.get(0);
		Assert.assertEquals("service:id:ldap:dig", service.getId());
		Assert.assertEquals("OpenLDAP", service.getName());
		Assert.assertNull(service.getRefined());

		// This node accept creation
		Assert.assertEquals(SubscriptionMode.CREATE, service.getMode());
	}

	@Test
	public void findAllByParentFilterModeLinkAcceptNoCreate() {
		final List<NodeVo> resources = resource.findAllByParent(BugTrackerResource.SERVICE_KEY, SubscriptionMode.CREATE);
		Assert.assertEquals(0, resources.size());
	}

	@Test
	public void findAllByParentFilterModeLinkStrict() {
		final List<NodeVo> resources = resource.findAllByParent(BugTrackerResource.SERVICE_KEY, SubscriptionMode.LINK);
		Assert.assertEquals(1, resources.size());
		final NodeVo service = resources.get(0);
		Assert.assertEquals("service:bt:jira", service.getId());
		Assert.assertEquals("JIRA", service.getName());
		Assert.assertNull(service.getRefined());
		Assert.assertNull(service.getUiClasses());
	}

	@Test
	public void findAllByParentFilterModeCreateAcceptLink() {
		final List<NodeVo> resources = resource.findAllByParent(LdapPluginResource.KEY, SubscriptionMode.LINK);
		Assert.assertEquals(1, resources.size());
		final NodeVo service = resources.get(0);
		Assert.assertEquals("service:id:ldap:dig", service.getId());
		Assert.assertEquals("OpenLDAP", service.getName());
		Assert.assertNull(service.getRefined());
		Assert.assertNull(service.getUiClasses());
	}

	@Test
	public void findAllByParentCreateMode() {
		final List<NodeVo> resources = resource.findAllByParent(LdapPluginResource.KEY);
		Assert.assertEquals(1, resources.size());
		final NodeVo service = resources.get(0);
		Assert.assertEquals("service:id:ldap:dig", service.getId());
		Assert.assertEquals("OpenLDAP", service.getName());
		Assert.assertNull(service.getRefined());

		// This node accept creation
		Assert.assertEquals(SubscriptionMode.CREATE, service.getMode());
	}

	@Test
	public void findAllByParentMutiple() {
		final List<NodeVo> resources = new ArrayList<>(resource.findAllByParent(JiraBaseResource.KEY));
		Assert.assertEquals(2, resources.size());
		Assert.assertEquals("service:bt:jira:4", resources.get(0).getId());
		Assert.assertEquals("service:bt:jira:6", resources.get(1).getId());
	}

	@Test
	public void getNotProvidedParameters() {
		final List<ParameterVo> parameters = resource.getNotProvidedParameters("service:bt:jira:6", SubscriptionMode.LINK);
		Assert.assertEquals(25, parameters.size());
		final int nonDummyStartIndex = 23;
		Assert.assertEquals("service:bt:jira:pkey", parameters.get(nonDummyStartIndex).getId());
		Assert.assertEquals("service:bt:jira:project", parameters.get(nonDummyStartIndex + 1).getId());
	}

	@Test
	public void getNotProvidedParametersTool() {
		final List<ParameterVo> parameters = resource.getNotProvidedParameters("service:bt:jira", SubscriptionMode.LINK);
		Assert.assertEquals(32, parameters.size());
		Assert.assertEquals("c_10", parameters.get(0).getId());
		final int nonDummyStartIndex = 23;
		Assert.assertEquals("service:bt:jira:jdbc-driver", parameters.get(nonDummyStartIndex).getId());
		Assert.assertEquals("service:bt:jira:jdbc-password", parameters.get(nonDummyStartIndex + 1).getId());
		Assert.assertEquals("service:bt:jira:jdbc-url", parameters.get(nonDummyStartIndex + 2).getId());
		Assert.assertEquals("service:bt:jira:jdbc-user", parameters.get(nonDummyStartIndex + 3).getId());

		final ParameterVo projectParameter = parameters.get(nonDummyStartIndex + 4);
		Assert.assertEquals("service:bt:jira:password", projectParameter.getId());
		Assert.assertEquals("service:bt:jira:pkey", parameters.get(nonDummyStartIndex + 5).getId());
		final ParameterVo projectParameter2 = parameters.get(nonDummyStartIndex + 6);
		Assert.assertEquals(ParameterType.INTEGER, projectParameter2.getType());
		Assert.assertEquals("service:bt:jira", projectParameter2.getOwner().getId());
		Assert.assertEquals("service:bt:jira:project", projectParameter2.getId());
		Assert.assertEquals(1, projectParameter2.getMin().intValue());
		Assert.assertNull(projectParameter2.getMax());
		Assert.assertNull(projectParameter2.getValues());
	}

	@Test
	public void getNotProvidedParametersServiceEmpty() {
		final List<ParameterVo> parameters = resource.getNotProvidedParameters("service:bt", SubscriptionMode.LINK);
		Assert.assertEquals(0, parameters.size());
	}

	@Test
	public void getNotProvidedParametersServiceToService() {
		final List<ParameterVo> parameters = resource.getNotProvidedParameters("service:id", SubscriptionMode.LINK);
		Assert.assertEquals(2, parameters.size());
		Assert.assertEquals("service:id:group", parameters.get(0).getId());
		Assert.assertEquals("service:id:uid-pattern", parameters.get(1).getId());
	}

	@Test
	public void getNotProvidedParametersServiceToTool() {
		final List<ParameterVo> parameters = resource.getNotProvidedParameters("service:id:ldap", SubscriptionMode.LINK);
		Assert.assertEquals(19, parameters.size());
		final List<String> expected = Arrays.asList("service:id:ldap:base-dn", "service:id:ldap:companies-dn", "service:id:group",
				"service:id:ldap:groups-dn", "service:id:ldap:local-id-attribute", "service:id:ldap:locked-attribute", "service:id:ldap:locked-value",
				"service:id:ldap:password", "service:id:ldap:people-dn", "service:id:ldap:people-internal-dn", "service:id:ldap:people-class",
				"service:id:ldap:quarantine-dn", "service:id:ldap:referral", "service:id:ldap:uid-attribute", "service:id:uid-pattern",
				"service:id:ldap:url", "service:id:ldap:company-pattern", "service:id:ldap:department-attribute", "service:id:ldap:user-dn");
		Assert.assertTrue(parameters.stream().map(ParameterVo::getId).allMatch(expected::contains));
	}

	@Test
	public void getNotProvidedParametersServiceToToolCreate() {
		final List<ParameterVo> parameters = resource.getNotProvidedParameters("service:id:ldap", SubscriptionMode.CREATE);
		Assert.assertEquals(21, parameters.size());
		final List<String> expected = Arrays.asList("service:id:ldap:base-dn", "service:id:ldap:companies-dn", "service:id:group",
				"service:id:ldap:groups-dn", "service:id:ldap:local-id-attribute", "service:id:ldap:locked-attribute", "service:id:ldap:locked-value",
				"service:id:ou", "service:id:parent-group", "service:id:ldap:password", "service:id:ldap:people-dn",
				"service:id:ldap:people-internal-dn", "service:id:ldap:people-class", "service:id:ldap:quarantine-dn", "service:id:ldap:referral",
				"service:id:ldap:uid-attribute", "service:id:uid-pattern", "service:id:ldap:url", "service:id:ldap:department-attribute",
				"service:id:ldap:company-pattern", "service:id:ldap:user-dn");
		Assert.assertTrue(parameters.stream().map(ParameterVo::getId).allMatch(expected::contains));
	}

	@Test
	public void getNotProvidedParametersServiceToNode() {
		final List<ParameterVo> parameters = resource.getNotProvidedParameters("service:id:ldap:dig", SubscriptionMode.LINK);
		Assert.assertEquals(1, parameters.size());
		Assert.assertEquals("service:id:group", parameters.get(0).getId());
	}

	@Test
	public void getNodeStatus() throws Exception {
		final List<EventVo> nodes = resource.getNodeStatus();
		Assert.assertEquals(2, nodes.size());
		Assert.assertTrue(nodes.get(0).getNode().endsWith("build") && NodeStatus.UP.name().equals(nodes.get(0).getValue())
				|| NodeStatus.DOWN.name().equals(nodes.get(0).getValue()));
		Assert.assertTrue(nodes.get(1).getNode().endsWith("build") && NodeStatus.UP.name().equals(nodes.get(1).getValue())
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
		Assert.assertEquals(resource.findAllNoParent().size() + 2, nodes.size());
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
		final List<NodeVo> result = resource.findAll(newFindAllParameters(), "sonar").getData();
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
		final TableItem<NodeVo> findAll = resource.findAll(newFindAllParameters(), null);
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
}
