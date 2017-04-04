package org.ligoj.app.resource.subscription;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.persistence.EntityNotFoundException;
import javax.sql.DataSource;
import javax.transaction.Transactional;
import javax.ws.rs.ForbiddenException;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ligoj.app.MatcherUtil;
import org.ligoj.app.api.ConfigurationVo;
import org.ligoj.app.api.NodeStatus;
import org.ligoj.app.api.SubscriptionMode;
import org.ligoj.app.api.SubscriptionStatusWithData;
import org.ligoj.app.dao.ParameterValueRepository;
import org.ligoj.app.dao.ProjectRepository;
import org.ligoj.app.dao.SubscriptionRepository;
import org.ligoj.app.model.DelegateNode;
import org.ligoj.app.model.Event;
import org.ligoj.app.model.Parameter;
import org.ligoj.app.model.Project;
import org.ligoj.app.model.Subscription;
import org.ligoj.app.resource.AbstractOrgTest;
import org.ligoj.app.resource.ServicePluginLocator;
import org.ligoj.app.resource.node.EventVo;
import org.ligoj.app.resource.node.ParameterValueEditionVo;
import org.ligoj.app.resource.node.sample.BugTrackerResource;
import org.ligoj.app.resource.node.sample.IdentityResource;
import org.ligoj.app.resource.node.sample.JiraBaseResource;
import org.ligoj.app.resource.node.sample.JiraPluginResource;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Test class of {@link SubscriptionResource}
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
public class SubscriptionResourceTest extends AbstractOrgTest {

	@Autowired
	protected ProjectRepository projectRepository;

	protected static DataSource datasource;

	protected int subscription;

	@Autowired
	private SubscriptionResource resource;

	@Autowired
	private SubscriptionRepository repository;

	@Autowired
	private ParameterValueRepository parameterValueRepository;

	@Autowired
	private ServicePluginLocator servicePluginLocator;

	@Before
	public void prepareSubscription() throws IOException {
		persistEntities("csv", new Class[] { Event.class, DelegateNode.class }, StandardCharsets.UTF_8.name());
		persistSystemEntities();
		this.subscription = getSubscription("MDA");
	}

	/**
	 * Return the subscription identifier of MDA. Assumes there is only one subscription for a service.
	 */
	protected int getSubscription(final String project) {
		return getSubscription(project, BugTrackerResource.SERVICE_KEY);
	}

	@Test
	public void getSubscriptionParameterValue() {
		Assert.assertEquals("10074", parameterValueRepository.getSubscriptionParameterValue(subscription, JiraBaseResource.PARAMETER_PROJECT));
	}

	@Test
	public void toStringTest() {
		final Subscription subscription = repository.findOneExpected(this.subscription);
		final String subscriptionStr = subscription.toString();
		Assert.assertTrue(subscriptionStr.startsWith("Subscription(super=Entity of type org.ligoj.app.model.Subscription with id: "));
		Assert.assertTrue(subscriptionStr.endsWith(", node=AbstractNamedBusinessEntity(name=JIRA 4), project=AbstractNamedAuditedEntity(name=MDA))"));
		Assert.assertEquals("Subscription(super=Entity of type org.ligoj.app.model.Subscription with id: null, node=null, project=null)",
				new Subscription().toString());
	}

	@Test
	public void checkMandatoryParameters() {
		final List<ParameterValueEditionVo> parameters = new ArrayList<>();
		final List<Parameter> acceptedParameters = new ArrayList<>();
		final ParameterValueEditionVo parameterValue = new ParameterValueEditionVo();
		parameterValue.setParameter("p");
		parameters.add(parameterValue);
		final Parameter parameter = new Parameter();
		parameter.setId("p");
		acceptedParameters.add(parameter);
		resource.checkMandatoryParameters(parameters, acceptedParameters, null);
	}

	@Test(expected = ValidationJsonException.class)
	public void checkMandatoryParametersMandatory() {
		final List<ParameterValueEditionVo> parameters = new ArrayList<>();
		final List<Parameter> acceptedParameters = new ArrayList<>();
		final Parameter parameter = new Parameter();
		parameter.setId("p");
		parameter.setMandatory(true);
		acceptedParameters.add(parameter);
		resource.checkMandatoryParameters(parameters, acceptedParameters, null);
	}

	@Test
	public void checkMandatoryParametersMandatoryNotMode() {
		final List<ParameterValueEditionVo> parameters = new ArrayList<>();
		final List<Parameter> acceptedParameters = new ArrayList<>();
		final Parameter parameter = new Parameter();
		parameter.setId("p");
		parameter.setMandatory(true);
		parameter.setMode(SubscriptionMode.CREATE);
		acceptedParameters.add(parameter);
		resource.checkMandatoryParameters(parameters, acceptedParameters, SubscriptionMode.LINK);
	}

	@Test(expected = ValidationJsonException.class)
	public void checkMandatoryParametersMandatoryMode() {
		final List<ParameterValueEditionVo> parameters = new ArrayList<>();
		final List<Parameter> acceptedParameters = new ArrayList<>();
		final Parameter parameter = new Parameter();
		parameter.setId("p");
		parameter.setMandatory(true);
		parameter.setMode(SubscriptionMode.CREATE);
		acceptedParameters.add(parameter);
		resource.checkMandatoryParameters(parameters, acceptedParameters, SubscriptionMode.CREATE);
	}

	@Test
	public void getParameters() throws Exception {
		final Map<String, String> parameters = resource.getParameters(subscription);
		Assert.assertNull(parameters.get("service:bt:jira:jdbc-user"));
		Assert.assertNull(parameters.get("service:bt:jira:jdbc-password"));
		Assert.assertEquals("jdbc:hsqldb:mem:dataSource", parameters.get("service:bt:jira:jdbc-url"));
		Assert.assertEquals("org.hsqldb.jdbc.JDBCDriver", parameters.get("service:bt:jira:jdbc-driver"));
		Assert.assertEquals("10074", parameters.get("service:bt:jira:project"));
		Assert.assertEquals("MDA", parameters.get("service:bt:jira:pkey"));
		Assert.assertEquals("http://localhost:8120", parameters.get("service:bt:jira:url"));
	}

	@Test
	public void getConfiguration() throws Exception {
		final int subscription = repository.findByExpected("node.id", "service:vm:vcloud:sample").getId();
		final ConfigurationVo configuration = resource.getConfiguration(subscription);

		// Not secured parameter
		Assert.assertEquals("http://localhost:8120", configuration.getParameters().get("service:vm:vcloud:url"));

		// Secured parameter
		Assert.assertNull("http://localhost:8120", configuration.getParameters().get("service:vm:vcloud:user"));
		Assert.assertEquals(subscription, configuration.getSubscription());
		Assert.assertEquals("gStack", configuration.getProject().getName());
		Assert.assertEquals("service:vm:vcloud:sample", configuration.getNode().getId());
		Assert.assertNotNull(configuration.getConfiguration());
	}

	@Test
	public void getConfigurationNone() throws Exception {
		final int subscription = repository.findByExpected("node.id", "service:km:confluence:dig").getId();
		final ConfigurationVo configuration = resource.getConfiguration(subscription);
		Assert.assertEquals(subscription, configuration.getSubscription());
		Assert.assertEquals("gStack", configuration.getProject().getName());
		Assert.assertEquals("service:km:confluence:dig", configuration.getNode().getId());

		// No configuration for Confluence
		Assert.assertNull(configuration.getConfiguration());
	}

	@Test
	public void getNonSecuredParameters() throws Exception {
		final Map<String, String> parameters = resource.getNonSecuredParameters(getSubscription("gStack"));
		Assert.assertNull(parameters.get("service:bt:jira:jdbc-user"));
		Assert.assertNull(parameters.get("service:bt:jira:jdbc-password"));
		Assert.assertNull(parameters.get("service:bt:jira:jdbc-url"));
		Assert.assertNull(parameters.get("service:bt:jira:jdbc-driver"));
		Assert.assertNull(parameters.get("service:bt:jira:user"));
		Assert.assertNull(parameters.get("service:bt:jira:password"));
		Assert.assertEquals("10000", parameters.get("service:bt:jira:project"));
		Assert.assertEquals("GSTACK", parameters.get("service:bt:jira:pkey"));
		Assert.assertEquals("http://localhost:8120", parameters.get("service:bt:jira:url"));
	}

	@Test(expected = EntityNotFoundException.class)
	public void getParametersNotVisibleProject() throws Exception {
		initSpringSecurityContext("any");
		resource.getParameters(subscription);
	}

	@Test
	public void delete() throws Exception {
		initSpringSecurityContext("fdaugan");
		final Subscription one = repository.findOne(subscription);
		final int project = one.getProject().getId();
		Assert.assertEquals(1, repository.findAllByProject(project).size());
		em.clear();
		resource.delete(subscription);
		em.flush();
		em.clear();

		Assert.assertTrue(repository.findAllByProject(project).isEmpty());
		Assert.assertNull(repository.findOne(subscription));
	}

	@Test(expected = EntityNotFoundException.class)
	public void deleteNotVisibleProject() throws Exception {
		final Subscription one = repository.findOne(subscription);
		final int project = one.getProject().getId();
		Assert.assertEquals(1, repository.findAllByProject(project).size());
		em.clear();
		initSpringSecurityContext("any");
		resource.delete(subscription);
	}

	@Test(expected = EntityNotFoundException.class)
	public void createNotVisibleProject() throws Exception {

		// Test a creation by another user than the team leader and a manager
		initSpringSecurityContext("any");
		create();
	}

	/**
	 * Test a creation by another user than the team leader. The current user is administrator, can see all projects.
	 */
	@Test
	public void createByAdmin() throws Exception {
		initSpringSecurityContext(DEFAULT_USER);
		create();
	}

	@Test
	public void create() throws Exception {
		em.createQuery("DELETE Parameter WHERE id LIKE ?1").setParameter(1, "c_%").executeUpdate();

		final SubscriptionEditionVo vo = new SubscriptionEditionVo();
		final List<ParameterValueEditionVo> parameters = new ArrayList<>();
		final ParameterValueEditionVo parameterValueEditionVo = new ParameterValueEditionVo();
		parameterValueEditionVo.setParameter(JiraBaseResource.PARAMETER_PROJECT);
		parameterValueEditionVo.setInteger(10074);
		parameters.add(parameterValueEditionVo);
		final ParameterValueEditionVo parameterValueEditionVo2 = new ParameterValueEditionVo();
		parameterValueEditionVo2.setParameter(JiraBaseResource.PARAMETER_PKEY);
		parameterValueEditionVo2.setText("MDA");
		parameters.add(parameterValueEditionVo2);

		vo.setParameters(parameters);
		vo.setNode("service:bt:jira:4");
		vo.setProject(em.createQuery("SELECT id FROM Project WHERE name='gStack'", Integer.class).getSingleResult());

		em.flush();
		em.clear();

		final int subscription = resource.create(vo);
		em.flush();
		em.clear();

		Assert.assertEquals("10074", parameterValueRepository.getSubscriptionParameterValue(subscription, JiraBaseResource.PARAMETER_PROJECT));
		Assert.assertEquals("MDA", parameterValueRepository.getSubscriptionParameterValue(subscription, JiraBaseResource.PARAMETER_PKEY));
	}

	/**
	 * The project is visible for user "alongchu" since he is in the main group "gfi-gstack" of the project
	 * "gfi-gstack", however he is neither the team leader of this project, neither an administrator, neither a manger
	 * of the group "gfi-gstack".
	 */
	@Test(expected = ForbiddenException.class)
	public void createNotManagedProject() throws Exception {
		initSpringSecurityContext("alongchu");
		final SubscriptionEditionVo vo = new SubscriptionEditionVo();
		vo.setParameters(new ArrayList<>());
		vo.setNode("service:bt:jira:4");
		vo.setProject(em.createQuery("SELECT id FROM Project WHERE name='gStack'", Integer.class).getSingleResult());

		em.flush();
		em.clear();

		resource.create(vo);
	}

	@Test(expected = ValidationJsonException.class)
	public void createNotSubscribeRight() throws Exception {
		// This users sees only Jenkins nodes
		initSpringSecurityContext("user1");

		// Make the project visible for this user
		final Project project = projectRepository.findByName("gStack");
		project.setTeamLeader("user1");
		em.merge(project);

		final SubscriptionEditionVo vo = new SubscriptionEditionVo();
		vo.setNode("service:bt:jira:4");
		vo.setProject(project.getId());
		em.flush();
		em.clear();

		// Ensure LDAP cache is loaded
		em.flush();
		em.clear();

		resource.create(vo);
	}

	@Test(expected = NotImplementedException.class)
	public void createCreateModeNotSupported() throws Exception {
		em.createQuery("DELETE Parameter WHERE id LIKE ?1").setParameter(1, "c_%").executeUpdate();

		final SubscriptionEditionVo vo = new SubscriptionEditionVo();
		final List<ParameterValueEditionVo> parameters = new ArrayList<>();
		final ParameterValueEditionVo parameterValueEditionVo = new ParameterValueEditionVo();
		parameterValueEditionVo.setParameter(JiraBaseResource.PARAMETER_PROJECT);
		parameterValueEditionVo.setInteger(10074);
		parameters.add(parameterValueEditionVo);
		final ParameterValueEditionVo parameterValueEditionVo2 = new ParameterValueEditionVo();
		parameterValueEditionVo2.setParameter(JiraBaseResource.PARAMETER_PKEY);
		parameterValueEditionVo2.setText("MDA");
		parameters.add(parameterValueEditionVo2);

		vo.setMode(SubscriptionMode.CREATE);
		vo.setParameters(parameters);
		vo.setNode("service:bt:jira:4");
		vo.setProject(em.createQuery("SELECT id FROM Project WHERE name='gStack'", Integer.class).getSingleResult());

		// Ensure LDAP cache is loaded
		em.flush();
		em.clear();

		resource.create(vo);
	}

	@Test
	public void createCreateMode() throws Exception {
		// Prepare data
		em.createQuery("DELETE Parameter WHERE id LIKE ?1").setParameter(1, "c_%").executeUpdate();
		final SubscriptionEditionVo vo = new SubscriptionEditionVo();
		final List<ParameterValueEditionVo> parameters = new ArrayList<>();
		final ParameterValueEditionVo parameterValueEditionVo = new ParameterValueEditionVo();
		parameterValueEditionVo.setParameter(IdentityResource.PARAMETER_OU);
		parameterValueEditionVo.setText("gfi");
		parameters.add(parameterValueEditionVo);
		final ParameterValueEditionVo parameterValueEditionVo2 = new ParameterValueEditionVo();
		parameterValueEditionVo2.setParameter(IdentityResource.PARAMETER_PARENT_GROUP);
		parameterValueEditionVo2.setText("gfi-gstack");
		parameters.add(parameterValueEditionVo2);
		final ParameterValueEditionVo parameterValueEditionVo3 = new ParameterValueEditionVo();
		parameterValueEditionVo3.setParameter(IdentityResource.PARAMETER_GROUP);
		parameterValueEditionVo3.setText("gfi-gstack-client");
		parameters.add(parameterValueEditionVo3);

		vo.setMode(SubscriptionMode.CREATE);
		vo.setParameters(parameters);
		vo.setNode("service:id:ldap:dig");
		vo.setProject(em.createQuery("SELECT id FROM Project WHERE name='gStack'", Integer.class).getSingleResult());
		em.flush();
		em.clear();

		initSpringSecurityContext(DEFAULT_USER);
		final int subscription = resource.create(vo);
		em.flush();
		em.clear();

		Assert.assertEquals("gfi-gstack-client",
				parameterValueRepository.getSubscriptionParameterValue(subscription, IdentityResource.PARAMETER_GROUP));
		Assert.assertEquals("gfi", parameterValueRepository.getSubscriptionParameterValue(subscription, IdentityResource.PARAMETER_OU));
		Assert.assertEquals("gfi-gstack",
				parameterValueRepository.getSubscriptionParameterValue(subscription, IdentityResource.PARAMETER_PARENT_GROUP));
	}

	/**
	 * Create mode, blank optional parameter
	 */
	@Test
	public void createCreateModeBlank() throws Exception {
		// Prepare data
		em.createQuery("DELETE Parameter WHERE id LIKE ?1").setParameter(1, "c_%").executeUpdate();

		final SubscriptionEditionVo vo = new SubscriptionEditionVo();
		final List<ParameterValueEditionVo> parameters = new ArrayList<>();
		final ParameterValueEditionVo parameterValueEditionVo = new ParameterValueEditionVo();
		parameterValueEditionVo.setParameter(IdentityResource.PARAMETER_OU);
		parameterValueEditionVo.setText("gfi");
		parameters.add(parameterValueEditionVo);
		final ParameterValueEditionVo parameterValueEditionVo2 = new ParameterValueEditionVo();
		parameterValueEditionVo2.setParameter(IdentityResource.PARAMETER_PARENT_GROUP);
		parameterValueEditionVo2.setText("");
		parameters.add(parameterValueEditionVo2);
		final ParameterValueEditionVo parameterValueEditionVo3 = new ParameterValueEditionVo();
		parameterValueEditionVo3.setParameter(IdentityResource.PARAMETER_GROUP);
		parameterValueEditionVo3.setText("gfi-gstack-client2");
		parameters.add(parameterValueEditionVo3);

		vo.setMode(SubscriptionMode.CREATE);
		vo.setParameters(parameters);
		vo.setNode("service:id:ldap:dig");
		vo.setProject(em.createQuery("SELECT id FROM Project WHERE name='gStack'", Integer.class).getSingleResult());

		em.flush();
		em.clear();

		initSpringSecurityContext(DEFAULT_USER);
		final int subscription = resource.create(vo);
		em.flush();
		em.clear();

		Assert.assertEquals("gfi-gstack-client2",
				parameterValueRepository.getSubscriptionParameterValue(subscription, IdentityResource.PARAMETER_GROUP));
		Assert.assertEquals("gfi", parameterValueRepository.getSubscriptionParameterValue(subscription, IdentityResource.PARAMETER_OU));
		Assert.assertNull(parameterValueRepository.getSubscriptionParameterValue(subscription, IdentityResource.PARAMETER_PARENT_GROUP));
	}

	@Test
	public void createNotAcceptedParameter() throws Exception {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher(JiraBaseResource.PARAMETER_JDBC_PASSSWORD, "not-accepted-parameter"));

		em.createQuery("DELETE Parameter WHERE id LIKE ?1").setParameter(1, "c_%").executeUpdate();

		final Project project = new Project();
		project.setName("TEST");
		project.setPkey("test");
		project.setTeamLeader(getAuthenticationName());
		em.persist(project);
		em.flush();
		em.clear();

		final SubscriptionEditionVo vo = new SubscriptionEditionVo();
		final List<ParameterValueEditionVo> parameters = new ArrayList<>();
		final ParameterValueEditionVo parameterValueEditionVo = new ParameterValueEditionVo();
		parameterValueEditionVo.setParameter("service:bt:jira:project");
		parameterValueEditionVo.setInteger(1007400);
		parameters.add(parameterValueEditionVo);

		final ParameterValueEditionVo parameterValueEditionVo2 = new ParameterValueEditionVo();
		parameterValueEditionVo2.setParameter("service:bt:jira:pkey");
		parameterValueEditionVo2.setText("MYPROJECT");
		parameters.add(parameterValueEditionVo2);

		final ParameterValueEditionVo parameterValueEditionVo3 = new ParameterValueEditionVo();
		parameterValueEditionVo3.setParameter("service:bt:jira:jdbc-password");
		parameterValueEditionVo3.setInteger(1007400);
		parameters.add(parameterValueEditionVo3);

		vo.setParameters(parameters);
		vo.setNode("service:bt:jira:4");
		vo.setProject(project.getId());
		final int subscription = resource.create(vo);
		em.flush();
		em.clear();

		Assert.assertEquals("1007400", parameterValueRepository.getSubscriptionParameterValue(subscription, JiraBaseResource.PARAMETER_PROJECT));
		Assert.assertEquals("MYPROJECT", parameterValueRepository.getSubscriptionParameterValue(subscription, JiraBaseResource.PARAMETER_PKEY));
	}

	@Test
	public void createMissingParameter() throws Exception {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("service:bt:jira:pkey", "NotNull"));

		em.createQuery("DELETE Parameter WHERE id LIKE ?1").setParameter(1, "c_%").executeUpdate();

		final Project project = new Project();
		project.setName("TEST");
		project.setPkey("test");
		project.setTeamLeader(getAuthenticationName());
		em.persist(project);
		em.flush();
		em.clear();

		final SubscriptionEditionVo vo = new SubscriptionEditionVo();
		final List<ParameterValueEditionVo> parameters = new ArrayList<>();
		final ParameterValueEditionVo parameterValueEditionVo = new ParameterValueEditionVo();
		parameterValueEditionVo.setParameter("service:bt:jira:project");
		parameterValueEditionVo.setInteger(1007400);
		parameters.add(parameterValueEditionVo);

		vo.setParameters(parameters);
		vo.setNode("service:bt:jira:4");
		vo.setProject(project.getId());
		resource.create(vo);
	}

	@Test
	public void findAll() {
		final SubscriptionListVo subscriptionList = resource.findAll();

		// Check nodes
		final Collection<SubscribedNodeVo> nodes = subscriptionList.getNodes();
		Assert.assertTrue(nodes.size() > 30);
		final List<SubscribedNodeVo> subscribedNodes = new ArrayList<>(nodes);
		Assert.assertEquals("service:bt", subscribedNodes.get(0).getId());
		Assert.assertNull(subscribedNodes.get(0).getRefined());
		Assert.assertEquals("Bug Tracker", subscribedNodes.get(0).getName());
		Assert.assertEquals("functional", subscribedNodes.get(0).getTag());
		Assert.assertEquals("fa fa-suitcase", subscribedNodes.get(0).getTagUiClasses());

		Assert.assertEquals("service:bt:jira", subscribedNodes.get(1).getId());
		Assert.assertEquals("service:bt", subscribedNodes.get(1).getRefined());
		Assert.assertEquals("JIRA", subscribedNodes.get(1).getName());
		Assert.assertNull(subscribedNodes.get(1).getTag());
		Assert.assertNull(subscribedNodes.get(1).getTagUiClasses());

		// Check subscriptions
		Assert.assertTrue(subscriptionList.getSubscriptions().size() >= 13);
		final List<SubscriptionLightVo> subscriptions = new ArrayList<>(subscriptionList.getSubscriptions());

		// Check project order

		// GSTACK Project
		final SubscriptionLightVo subscription0 = subscriptions.get(0);
		Assert.assertTrue(subscription0.getId() > 0);
		Assert.assertTrue(subscription0.getProject() > 0);
		Assert.assertEquals("gStack",
				subscriptionList.getProjects().stream().filter(p -> p.getId().equals(subscription0.getProject())).findFirst().get().getName());

		// MDA Project (last subscription), only JIRA4 subscription
		final SubscriptionLightVo subscription = subscriptions.get(subscriptions.size() - 1);
		Assert.assertTrue(subscription.getId() > 0);
		Assert.assertTrue(subscription.getProject() > 0);
		Assert.assertEquals("MDA",
				subscriptionList.getProjects().stream().filter(p -> p.getId().equals(subscription.getProject())).findFirst().get().getName());
		Assert.assertEquals("service:bt:jira:4", subscription.getNode());

		// Check projects
		final Collection<SubscribingProjectVo> projects = subscriptionList.getProjects();
		Assert.assertEquals(2, projects.size());

		// Check subscription project
		final SubscribingProjectVo projectVo = projects.stream().filter(p -> Objects.equals(p.getId(), subscription.getProject())).findFirst().get();
		Assert.assertEquals("MDA", projectVo.getName());
		Assert.assertEquals("mda", projectVo.getPkey());

		// Check subscription node
		final SubscribedNodeVo nodeVo = nodes.stream().filter(p -> Objects.equals(p.getId(), subscription.getNode())).findFirst().get();
		Assert.assertEquals("JIRA 4", nodeVo.getName());
		Assert.assertEquals("service:bt:jira:4", nodeVo.getId());

	}

	@Test
	public void refreshStatuses() throws IOException {
		persistEntities("csv", new Class[] { Event.class }, StandardCharsets.UTF_8.name());
		final int projectId = projectRepository.findByName("MDA").getId();
		final Map<Integer, EventVo> subscriptionStatus = resource.getStatusByProject(projectId);
		Assert.assertEquals(1, subscriptionStatus.size());
		final Map<Integer, SubscriptionStatusWithData> statuses = resource.refreshStatuses(Collections.singleton(subscription));
		Assert.assertEquals(1, statuses.size());
		final SubscriptionStatusWithData status = statuses.get(subscription);
		Assert.assertEquals(subscription, status.getId().intValue());
		Assert.assertEquals(NodeStatus.UP, status.getStatus());
		Assert.assertEquals("service:bt:jira:4", status.getNode());
		Assert.assertEquals(projectId, status.getProject().intValue());
		Assert.assertNotNull(status.getParameters());
		Assert.assertEquals(3, status.getParameters().size());
		Assert.assertEquals("10074", status.getParameters().get("service:bt:jira:project"));
		Assert.assertEquals("MDA", status.getParameters().get("service:bt:jira:pkey"));
		Assert.assertEquals("http://localhost:8120", status.getParameters().get("service:bt:jira:url"));
	}

	@Test
	public void getStatusByProject() throws IOException {
		persistEntities("csv", new Class[] { Event.class }, StandardCharsets.UTF_8.name());
		final int projectId = projectRepository.findByName("gStack").getId();
		final Map<Integer, EventVo> subscriptionStatus = resource.getStatusByProject(projectId);
		Assert.assertEquals(1, subscriptionStatus.size());
		Assert.assertEquals("JIRA 6", subscriptionStatus.values().iterator().next().getLabel());
	}

	@Test
	public void checkSubscriptionStatus() throws Exception {
		JiraPluginResource service = servicePluginLocator.getResource("service:bt:jira:4", JiraPluginResource.class);
		final SubscriptionStatusWithData status = service.checkSubscriptionStatus(null);
		Assert.assertNotNull(status);
		Assert.assertEquals("value", status.getData().get("property"));
	}

	@Test
	public void servicePlugin() throws Exception {
		JiraPluginResource service = servicePluginLocator.getResource("service:bt:jira:4", JiraPluginResource.class);
		Assert.assertEquals("Jira", service.getName());
		Assert.assertNull(service.getVendor());
		Assert.assertNull(service.getVersion());
		Assert.assertEquals(0, service.compareTo(service));
	}

	@Test
	public void checkStatus() throws Exception {
		Assert.assertTrue(servicePluginLocator.getResource("service:bt:jira:4", JiraPluginResource.class).checkStatus(null));
	}

}
