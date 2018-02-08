package org.ligoj.app.resource.subscription;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.persistence.EntityNotFoundException;
import javax.transaction.Transactional;
import javax.ws.rs.ForbiddenException;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ligoj.app.MatcherUtil;
import org.ligoj.app.api.ConfigurationVo;
import org.ligoj.app.api.NodeStatus;
import org.ligoj.app.api.ServicePlugin;
import org.ligoj.app.api.SubscriptionMode;
import org.ligoj.app.api.SubscriptionStatusWithData;
import org.ligoj.app.dao.ParameterValueRepository;
import org.ligoj.app.dao.ProjectRepository;
import org.ligoj.app.dao.SubscriptionRepository;
import org.ligoj.app.dao.TaskSampleSubscriptionRepository;
import org.ligoj.app.model.DelegateNode;
import org.ligoj.app.model.Event;
import org.ligoj.app.model.Parameter;
import org.ligoj.app.model.Project;
import org.ligoj.app.model.Subscription;
import org.ligoj.app.model.TaskSampleSubscription;
import org.ligoj.app.resource.AbstractOrgTest;
import org.ligoj.app.resource.ServicePluginLocator;
import org.ligoj.app.resource.node.EventVo;
import org.ligoj.app.resource.node.ParameterValueCreateVo;
import org.ligoj.app.resource.node.sample.BugTrackerResource;
import org.ligoj.app.resource.node.sample.IdentityResource;
import org.ligoj.app.resource.node.sample.JiraBaseResource;
import org.ligoj.app.resource.node.sample.JiraPluginResource;
import org.ligoj.app.resource.plugin.LongTaskRunner;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Test class of {@link SubscriptionResource}
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
public class SubscriptionResourceTest extends AbstractOrgTest {

	@Autowired
	protected ProjectRepository projectRepository;

	protected int subscription;

	@Autowired
	private SubscriptionResource resource;

	@Autowired
	private SubscriptionRepository repository;

	@Autowired
	private ParameterValueRepository parameterValueRepository;

	@Autowired
	private ServicePluginLocator servicePluginLocator;

	@BeforeEach
	public void prepareSubscription() throws IOException {
		persistEntities("csv", new Class[] { Event.class, DelegateNode.class }, StandardCharsets.UTF_8.name());
		persistSystemEntities();
		this.subscription = getSubscription("MDA");
	}

	/**
	 * Return the subscription identifier of MDA. Assumes there is only one
	 * subscription for a service.
	 */
	protected int getSubscription(final String project) {
		return getSubscription(project, BugTrackerResource.SERVICE_KEY);
	}

	@Test
	public void getSubscriptionParameterValue() {
		Assertions.assertEquals("10074",
				parameterValueRepository.getSubscriptionParameterValue(subscription, JiraBaseResource.PARAMETER_PROJECT));
	}

	@Test
	public void toStringTest() {
		final Subscription subscription = repository.findOneExpected(this.subscription);
		final String subscriptionStr = subscription.toString();
		Assertions.assertTrue(subscriptionStr.startsWith("Subscription(super=Entity of type org.ligoj.app.model.Subscription with id: "));
		Assertions.assertTrue(
				subscriptionStr.endsWith(", node=AbstractNamedBusinessEntity(name=JIRA 4), project=AbstractNamedAuditedEntity(name=MDA))"));
		Assertions.assertEquals(
				"Subscription(super=Entity of type org.ligoj.app.model.Subscription with id: null, node=null, project=null)",
				new Subscription().toString());
	}

	@Test
	public void checkMandatoryParameters() {
		final List<ParameterValueCreateVo> parameters = new ArrayList<>();
		final List<Parameter> acceptedParameters = new ArrayList<>();
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter("p");
		parameters.add(parameterValue);
		final Parameter parameter = new Parameter();
		parameter.setId("p");
		acceptedParameters.add(parameter);
		resource.checkMandatoryParameters(parameters, acceptedParameters, null);
	}

	@Test
	public void checkMandatoryParametersMandatory() {
		final List<ParameterValueCreateVo> parameters = new ArrayList<>();
		final List<Parameter> acceptedParameters = new ArrayList<>();
		final Parameter parameter = new Parameter();
		parameter.setId("p");
		parameter.setMandatory(true);
		acceptedParameters.add(parameter);
		Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.checkMandatoryParameters(parameters, acceptedParameters, null);
		});
	}

	@Test
	public void checkMandatoryParametersMandatoryNotMode() {
		final List<ParameterValueCreateVo> parameters = new ArrayList<>();
		final List<Parameter> acceptedParameters = new ArrayList<>();
		final Parameter parameter = new Parameter();
		parameter.setId("p");
		parameter.setMandatory(true);
		parameter.setMode(SubscriptionMode.CREATE);
		acceptedParameters.add(parameter);
		resource.checkMandatoryParameters(parameters, acceptedParameters, SubscriptionMode.LINK);
	}

	@Test
	public void checkMandatoryParametersMandatoryMode() {
		final List<ParameterValueCreateVo> parameters = new ArrayList<>();
		final List<Parameter> acceptedParameters = new ArrayList<>();
		final Parameter parameter = new Parameter();
		parameter.setId("p");
		parameter.setMandatory(true);
		parameter.setMode(SubscriptionMode.CREATE);
		acceptedParameters.add(parameter);
		Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.checkMandatoryParameters(parameters, acceptedParameters, SubscriptionMode.CREATE);
		});
	}

	@Test
	public void getParameters() {
		final Map<String, String> parameters = resource.getParameters(subscription);
		Assertions.assertNull(parameters.get("service:bt:jira:jdbc-user"));
		Assertions.assertNull(parameters.get("service:bt:jira:jdbc-password"));
		Assertions.assertEquals("jdbc:hsqldb:mem:dataSource", parameters.get("service:bt:jira:jdbc-url"));
		Assertions.assertEquals("org.hsqldb.jdbc.JDBCDriver", parameters.get("service:bt:jira:jdbc-driver"));
		Assertions.assertEquals("10074", parameters.get("service:bt:jira:project"));
		Assertions.assertEquals("MDA", parameters.get("service:bt:jira:pkey"));
		Assertions.assertEquals("http://localhost:8120", parameters.get("service:bt:jira:url"));
	}

	@Test
	public void getConfiguration() throws Exception {
		final int subscription = repository.findByExpected("node.id", "service:vm:vcloud:sample").getId();
		final ConfigurationVo configuration = resource.getConfiguration(subscription);

		// Not secured parameter
		Assertions.assertEquals("http://localhost:8120", configuration.getParameters().get("service:vm:vcloud:url"));

		// Secured parameter
		Assertions.assertNull(configuration.getParameters().get("service:vm:vcloud:user"));
		Assertions.assertEquals(subscription, configuration.getSubscription());
		Assertions.assertEquals("gStack", configuration.getProject().getName());
		Assertions.assertEquals("service:vm:vcloud:sample", configuration.getNode().getId());
		Assertions.assertNotNull(configuration.getConfiguration());
	}

	@Test
	public void getConfigurationNone() throws Exception {
		final int subscription = repository.findByExpected("node.id", "service:km:confluence:dig").getId();
		final ConfigurationVo configuration = resource.getConfiguration(subscription);
		Assertions.assertEquals(subscription, configuration.getSubscription());
		Assertions.assertEquals("gStack", configuration.getProject().getName());
		Assertions.assertEquals("service:km:confluence:dig", configuration.getNode().getId());

		// No configuration for Confluence
		Assertions.assertNull(configuration.getConfiguration());
	}

	@Test
	public void getNonSecuredParameters() {
		final Map<String, String> parameters = resource.getNonSecuredParameters(getSubscription("gStack"));
		Assertions.assertNull(parameters.get("service:bt:jira:jdbc-user"));
		Assertions.assertNull(parameters.get("service:bt:jira:jdbc-password"));
		Assertions.assertNull(parameters.get("service:bt:jira:jdbc-url"));
		Assertions.assertNull(parameters.get("service:bt:jira:jdbc-driver"));
		Assertions.assertNull(parameters.get("service:bt:jira:user"));
		Assertions.assertNull(parameters.get("service:bt:jira:password"));
		Assertions.assertEquals("10000", parameters.get("service:bt:jira:project"));
		Assertions.assertEquals("GSTACK", parameters.get("service:bt:jira:pkey"));
		Assertions.assertEquals("http://localhost:8120", parameters.get("service:bt:jira:url"));
	}

	@Test
	public void getParametersNotVisibleProject() {
		initSpringSecurityContext("any");
		Assertions.assertThrows(EntityNotFoundException.class, () -> {
			resource.getParameters(subscription);
		});
	}

	/**
	 * Not a {@link LongTaskRunner} implementation -> does nothing
	 */
	@Test
	public void deleteTasksNoTaskRunner() {
		resource.deleteTasks(Mockito.mock(ServicePlugin.class), 0);
	}

	@Autowired
	private TaskSampleSubscriptionRepository taskSampleRepository;

	@Test
	public void deleteTasksSubscription() throws Exception {
		final TaskSampleSubscriptionResource sampleResource = registerSingleton("taskSampleResource",
				applicationContext.getAutowireCapableBeanFactory().createBean(TaskSampleSubscriptionResource.class));

		try {
			final TaskSampleSubscription entity = new TaskSampleSubscription();
			entity.setLocked(repository.findOne(subscription));
			entity.setStart(new Date());
			entity.setAuthor(DEFAULT_USER);
			taskSampleRepository.saveAndFlush(entity);
			Assertions.assertNotNull(taskSampleRepository.findNotFinishedByLocked(subscription));
			entity.setEnd(new Date());
			taskSampleRepository.saveAndFlush(entity);
			Assertions.assertNull(taskSampleRepository.findNotFinishedByLocked(subscription));
			em.flush();
			em.clear();
			Assertions.assertEquals(1, taskSampleRepository.count());
			resource.deleteTasks(sampleResource, subscription);
			Assertions.assertNull(taskSampleRepository.findNotFinishedByLocked(subscription));
			Assertions.assertEquals(0, taskSampleRepository.count());
		} finally {
			destroySingleton("taskSampleResource");
		}
	}

	@Test
	public void delete() throws Exception {
		initSpringSecurityContext("fdaugan");
		final Subscription one = repository.findOne(subscription);
		final int project = one.getProject().getId();
		Assertions.assertEquals(1, repository.findAllByProject(project).size());
		em.clear();
		resource.delete(subscription);
		em.flush();
		em.clear();

		Assertions.assertTrue(repository.findAllByProject(project).isEmpty());
		Assertions.assertNull(repository.findOne(subscription));
	}

	@Test
	public void deleteNotVisibleProject() {
		final Subscription one = repository.findOne(subscription);
		final int project = one.getProject().getId();
		Assertions.assertEquals(1, repository.findAllByProject(project).size());
		em.clear();
		initSpringSecurityContext("any");
		Assertions.assertThrows(EntityNotFoundException.class, () -> {
			resource.delete(subscription);
		});
	}

	@Test
	public void createNotVisibleProject() {

		// Test a creation by another user than the team leader and a manager
		initSpringSecurityContext("any");
		Assertions.assertThrows(EntityNotFoundException.class, () -> {
			create();
		});
	}

	/**
	 * Test a creation by another user than the team leader. The current user is
	 * administrator, can see all projects.
	 */
	@Test
	public void createByAdmin() throws Exception {
		initSpringSecurityContext(DEFAULT_USER);
		create();
	}

	@Test
	public void create() throws Exception {
		final int subscription = resource.create(newCreateVo());
		em.flush();
		em.clear();

		Assertions.assertEquals("10074",
				parameterValueRepository.getSubscriptionParameterValue(subscription, JiraBaseResource.PARAMETER_PROJECT));
		Assertions.assertEquals("MDA",
				parameterValueRepository.getSubscriptionParameterValue(subscription, JiraBaseResource.PARAMETER_PKEY));
	}

	/**
	 * The project is visible for user "alongchu" since he is in the main group
	 * "gfi-gstack" of the project "gfi-gstack", however he is neither the team
	 * leader of this project, neither an administrator, neither a manger of the
	 * group "gfi-gstack".
	 */
	@Test
	public void createNotManagedProject() {
		initSpringSecurityContext("alongchu");
		final SubscriptionEditionVo vo = new SubscriptionEditionVo();
		vo.setParameters(new ArrayList<>());
		vo.setNode("service:bt:jira:4");
		vo.setProject(em.createQuery("SELECT id FROM Project WHERE name='gStack'", Integer.class).getSingleResult());

		em.flush();
		em.clear();

		Assertions.assertThrows(ForbiddenException.class, () -> {
			resource.create(vo);
		});
	}

	@Test
	public void createNotSubscribeRight() {
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

		Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.create(vo);
		});
	}

	@Test
	public void createCreateModeNotSupported() {
		final SubscriptionEditionVo vo = newCreateVo();
		vo.setMode(SubscriptionMode.CREATE);
		Assertions.assertThrows(NotImplementedException.class, () -> {
			resource.create(vo);
		});
	}

	private SubscriptionEditionVo newCreateVo() {
		em.createQuery("DELETE Parameter WHERE id LIKE ?1").setParameter(1, "c_%").executeUpdate();

		final SubscriptionEditionVo vo = new SubscriptionEditionVo();
		final List<ParameterValueCreateVo> parameters = new ArrayList<>();
		final ParameterValueCreateVo parameterValueEditionVo = new ParameterValueCreateVo();
		parameterValueEditionVo.setParameter(JiraBaseResource.PARAMETER_PROJECT);
		parameterValueEditionVo.setInteger(10074);
		parameters.add(parameterValueEditionVo);
		final ParameterValueCreateVo parameterValueEditionVo2 = new ParameterValueCreateVo();
		parameterValueEditionVo2.setParameter(JiraBaseResource.PARAMETER_PKEY);
		parameterValueEditionVo2.setText("MDA");
		parameters.add(parameterValueEditionVo2);
		vo.setParameters(parameters);
		vo.setNode("service:bt:jira:4");
		vo.setProject(em.createQuery("SELECT id FROM Project WHERE name='gStack'", Integer.class).getSingleResult());

		// Ensure LDAP cache is loaded
		em.flush();
		em.clear();
		return vo;
	}

	@Test
	public void createCreateMode() throws Exception {
		// Prepare data
		final int subscription = createCreateBase("gfi-gstack", "gfi-gstack-client");
		Assertions.assertEquals("gfi-gstack",
				parameterValueRepository.getSubscriptionParameterValue(subscription, IdentityResource.PARAMETER_PARENT_GROUP));
	}

	private int createCreateBase(final String parent, final String group) throws Exception {
		em.createQuery("DELETE Parameter WHERE id LIKE ?1").setParameter(1, "c_%").executeUpdate();
		final SubscriptionEditionVo vo = new SubscriptionEditionVo();
		final List<ParameterValueCreateVo> parameters = new ArrayList<>();
		final ParameterValueCreateVo parameterValueEditionVo = new ParameterValueCreateVo();
		parameterValueEditionVo.setParameter(IdentityResource.PARAMETER_OU);
		parameterValueEditionVo.setText("gfi");
		parameters.add(parameterValueEditionVo);
		final ParameterValueCreateVo parameterValueEditionVo2 = new ParameterValueCreateVo();
		parameterValueEditionVo2.setParameter(IdentityResource.PARAMETER_PARENT_GROUP);
		parameterValueEditionVo2.setText(parent);
		parameters.add(parameterValueEditionVo2);
		final ParameterValueCreateVo parameterValueEditionVo3 = new ParameterValueCreateVo();
		parameterValueEditionVo3.setParameter(IdentityResource.PARAMETER_GROUP);
		parameterValueEditionVo3.setText(group);
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

		Assertions.assertEquals(group,
				parameterValueRepository.getSubscriptionParameterValue(subscription, IdentityResource.PARAMETER_GROUP));
		Assertions.assertEquals("gfi", parameterValueRepository.getSubscriptionParameterValue(subscription, IdentityResource.PARAMETER_OU));
		return subscription;
	}

	/**
	 * Create mode, blank optional parameter
	 */
	@Test
	public void createCreateModeBlank() throws Exception {
		final int subscription = createCreateBase("", "gfi-gstack-client2");
		Assertions
				.assertNull(parameterValueRepository.getSubscriptionParameterValue(subscription, IdentityResource.PARAMETER_PARENT_GROUP));
	}

	@Test
	public void createNotAcceptedParameter() {
		final SubscriptionEditionVo vo = newCreateVoBadParameters();
		final ParameterValueCreateVo parameterValueEditionVo2 = new ParameterValueCreateVo();
		parameterValueEditionVo2.setParameter("service:bt:jira:pkey");
		parameterValueEditionVo2.setText("MYPROJECT");
		vo.getParameters().add(parameterValueEditionVo2);

		final ParameterValueCreateVo parameterValueEditionVo3 = new ParameterValueCreateVo();
		parameterValueEditionVo3.setParameter("service:bt:jira:jdbc-password");
		parameterValueEditionVo3.setInteger(1007400);
		vo.getParameters().add(parameterValueEditionVo3);

		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.create(vo);
		}), JiraBaseResource.PARAMETER_JDBC_PASSSWORD, "not-accepted-parameter");
	}

	@Test
	public void createMissingParameter() {
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.create(newCreateVoBadParameters());
		}), "service:bt:jira:pkey", "NotNull");
	}

	private SubscriptionEditionVo newCreateVoBadParameters() {
		em.createQuery("DELETE Parameter WHERE id LIKE ?1").setParameter(1, "c_%").executeUpdate();

		final Project project = new Project();
		project.setName("TEST");
		project.setPkey("test");
		project.setTeamLeader(getAuthenticationName());
		em.persist(project);
		em.flush();
		em.clear();

		final SubscriptionEditionVo vo = new SubscriptionEditionVo();
		final List<ParameterValueCreateVo> parameters = new ArrayList<>();
		final ParameterValueCreateVo parameterValueEditionVo = new ParameterValueCreateVo();
		parameterValueEditionVo.setParameter("service:bt:jira:project");
		parameterValueEditionVo.setInteger(1007400);
		parameters.add(parameterValueEditionVo);

		vo.setParameters(parameters);
		vo.setNode("service:bt:jira:4");
		vo.setProject(project.getId());
		return vo;
	}

	@Test
	public void findAll() {
		final SubscriptionListVo subscriptionList = resource.findAll();

		// Check nodes
		final Collection<SubscribedNodeVo> nodes = subscriptionList.getNodes();
		Assertions.assertTrue(nodes.size() > 30);
		final List<SubscribedNodeVo> subscribedNodes = new ArrayList<>(nodes);
		Assertions.assertEquals("service:bt", subscribedNodes.get(0).getId());
		Assertions.assertNull(subscribedNodes.get(0).getRefined());
		Assertions.assertEquals("Bug Tracker", subscribedNodes.get(0).getName());
		Assertions.assertEquals("functional", subscribedNodes.get(0).getTag());
		Assertions.assertEquals("fa fa-suitcase", subscribedNodes.get(0).getTagUiClasses());

		Assertions.assertEquals("service:bt:jira", subscribedNodes.get(1).getId());
		Assertions.assertEquals("service:bt", subscribedNodes.get(1).getRefined());
		Assertions.assertEquals("JIRA", subscribedNodes.get(1).getName());
		Assertions.assertNull(subscribedNodes.get(1).getTag());
		Assertions.assertNull(subscribedNodes.get(1).getTagUiClasses());

		// Check subscriptions
		Assertions.assertTrue(subscriptionList.getSubscriptions().size() >= 13);
		final List<SubscriptionLightVo> subscriptions = new ArrayList<>(subscriptionList.getSubscriptions());

		// Check project order

		// GSTACK Project
		final SubscriptionLightVo subscription0 = subscriptions.get(0);
		Assertions.assertTrue(subscription0.getId() > 0);
		Assertions.assertTrue(subscription0.getProject() > 0);
		Assertions.assertEquals("gStack", subscriptionList.getProjects().stream().filter(p -> p.getId().equals(subscription0.getProject()))
				.findFirst().get().getName());

		// MDA Project (last subscription), only JIRA4 subscription
		final SubscriptionLightVo subscription = subscriptions.get(subscriptions.size() - 1);
		Assertions.assertTrue(subscription.getId() > 0);
		Assertions.assertTrue(subscription.getProject() > 0);
		Assertions.assertEquals("MDA", subscriptionList.getProjects().stream().filter(p -> p.getId().equals(subscription.getProject()))
				.findFirst().get().getName());
		Assertions.assertEquals("service:bt:jira:4", subscription.getNode());

		// Check projects
		final Collection<SubscribingProjectVo> projects = subscriptionList.getProjects();
		Assertions.assertEquals(2, projects.size());

		// Check subscription project
		final SubscribingProjectVo projectVo = projects.stream().filter(p -> Objects.equals(p.getId(), subscription.getProject()))
				.findFirst().get();
		Assertions.assertEquals("MDA", projectVo.getName());
		Assertions.assertEquals("mda", projectVo.getPkey());

		// Check subscription node
		final SubscribedNodeVo nodeVo = nodes.stream().filter(p -> Objects.equals(p.getId(), subscription.getNode())).findFirst().get();
		Assertions.assertEquals("JIRA 4", nodeVo.getName());
		Assertions.assertEquals("service:bt:jira:4", nodeVo.getId());

	}

	@Test
	public void refreshStatuses() throws IOException {
		persistEntities("csv", new Class[] { Event.class }, StandardCharsets.UTF_8.name());
		final int projectId = projectRepository.findByName("MDA").getId();
		final Map<Integer, EventVo> subscriptionStatus = resource.getStatusByProject(projectId);
		Assertions.assertEquals(1, subscriptionStatus.size());
		final Map<Integer, SubscriptionStatusWithData> statuses = resource.refreshStatuses(Collections.singleton(subscription));
		Assertions.assertEquals(1, statuses.size());
		final SubscriptionStatusWithData status = statuses.get(subscription);
		Assertions.assertEquals(subscription, status.getId().intValue());
		Assertions.assertEquals(NodeStatus.UP, status.getStatus());
		Assertions.assertEquals("service:bt:jira:4", status.getNode());
		Assertions.assertEquals(projectId, status.getProject().intValue());
		Assertions.assertNotNull(status.getParameters());
		Assertions.assertEquals(3, status.getParameters().size());
		Assertions.assertEquals("10074", status.getParameters().get("service:bt:jira:project"));
		Assertions.assertEquals("MDA", status.getParameters().get("service:bt:jira:pkey"));
		Assertions.assertEquals("http://localhost:8120", status.getParameters().get("service:bt:jira:url"));
	}

	@Test
	public void getStatusByProject() throws IOException {
		persistEntities("csv", new Class[] { Event.class }, StandardCharsets.UTF_8.name());
		final int projectId = projectRepository.findByName("gStack").getId();
		final Map<Integer, EventVo> subscriptionStatus = resource.getStatusByProject(projectId);
		Assertions.assertEquals(1, subscriptionStatus.size());
		Assertions.assertEquals("JIRA 6", subscriptionStatus.values().iterator().next().getNode().getName());
	}

	@Test
	public void checkSubscriptionStatus() {
		JiraPluginResource service = servicePluginLocator.getResource("service:bt:jira:4", JiraPluginResource.class);
		final SubscriptionStatusWithData status = service.checkSubscriptionStatus(null);
		Assertions.assertNotNull(status);
		Assertions.assertEquals("value", status.getData().get("property"));
	}

	@Test
	public void servicePlugin() {
		JiraPluginResource service = servicePluginLocator.getResource("service:bt:jira:4", JiraPluginResource.class);
		Assertions.assertEquals("Jira", service.getName());
		Assertions.assertNull(service.getVendor());
		Assertions.assertNull(service.getVersion());
		Assertions.assertEquals(0, service.compareTo(service));
	}

	@Test
	public void checkStatus() {
		Assertions.assertTrue(servicePluginLocator.getResource("service:bt:jira:4", JiraPluginResource.class).checkStatus(null));
	}

}
