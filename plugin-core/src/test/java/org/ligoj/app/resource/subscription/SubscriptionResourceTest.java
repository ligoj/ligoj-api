/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.subscription;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ForbiddenException;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ligoj.app.api.NodeStatus;
import org.ligoj.app.api.ServicePlugin;
import org.ligoj.app.api.SubscriptionMode;
import org.ligoj.app.dao.*;
import org.ligoj.app.iam.dao.DelegateOrgRepository;
import org.ligoj.app.iam.model.*;
import org.ligoj.app.model.*;
import org.ligoj.app.resource.AbstractOrgTest;
import org.ligoj.app.resource.ServicePluginLocator;
import org.ligoj.app.resource.node.ParameterValueCreateVo;
import org.ligoj.app.resource.node.sample.BugTrackerResource;
import org.ligoj.app.resource.node.sample.IdentityResource;
import org.ligoj.app.resource.node.sample.JiraBaseResource;
import org.ligoj.app.resource.node.sample.JiraPluginResource;
import org.ligoj.app.resource.plugin.LongTaskRunner;
import org.ligoj.bootstrap.MatcherUtil;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Test class of {@link SubscriptionResource}
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
class SubscriptionResourceTest extends AbstractOrgTest {

	@Autowired
	protected ProjectRepository projectRepository;

	protected int subscription;

	@Autowired
	private SubscriptionResource resource;

	@Autowired
	private NodeRepository nodeRepository;

	@Autowired
	private SubscriptionRepository repository;

	@Autowired
	private DelegateOrgRepository delegateOrgRepository;

	@Autowired
	private DelegateNodeRepository delegateNodeRepository;

	@Autowired
	private ParameterValueRepository parameterValueRepository;

	@Autowired
	private ServicePluginLocator servicePluginLocator;

	@BeforeEach
	void prepareSubscription() throws IOException {
		persistEntities("csv", new Class<?>[]{Event.class, DelegateNode.class}, StandardCharsets.UTF_8);
		this.subscription = getSubscription("MDA");
	}

	/**
	 * Return the subscription identifier of MDA. Assumes there is only one subscription for a service.
	 */
	private int getSubscription(final String project) {
		return getSubscription(project, BugTrackerResource.SERVICE_KEY);
	}

	@Test
	void getSubscriptionParameterValue() {
		Assertions.assertEquals("10074", parameterValueRepository.getSubscriptionParameterValue(subscription,
				JiraBaseResource.PARAMETER_PROJECT));
	}

	@Test
	void toStringTest() {
		final var entity = repository.findOneExpected(this.subscription);
		final var subscriptionStr = entity.toString();
		Assertions.assertTrue(subscriptionStr
				.startsWith("Subscription(super=Entity of type org.ligoj.app.model.Subscription with id: "));
		Assertions.assertTrue(subscriptionStr.contains("name=JIRA 4"));
		Assertions.assertTrue(new Subscription().toString().contains("project=null)"));
	}

	@Test
	void checkMandatoryParameters() {
		final List<ParameterValueCreateVo> parameters = new ArrayList<>();
		final List<Parameter> acceptedParameters = new ArrayList<>();
		final var parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter("p");
		parameters.add(parameterValue);
		final var parameter = new Parameter();
		parameter.setId("p");
		acceptedParameters.add(parameter);
		resource.checkMandatoryParameters(parameters, acceptedParameters, null);
	}

	@Test
	void checkMandatoryParametersMandatory() {
		final List<ParameterValueCreateVo> parameters = new ArrayList<>();
		final List<Parameter> acceptedParameters = new ArrayList<>();
		final var parameter = new Parameter();
		parameter.setId("p");
		parameter.setMandatory(true);
		acceptedParameters.add(parameter);
		Assertions.assertThrows(ValidationJsonException.class, () -> resource.checkMandatoryParameters(parameters, acceptedParameters, null));
	}

	@Test
	void checkMandatoryParametersMandatoryNotMode() {
		final List<ParameterValueCreateVo> parameters = new ArrayList<>();
		final List<Parameter> acceptedParameters = new ArrayList<>();
		final var parameter = new Parameter();
		parameter.setId("p");
		parameter.setMandatory(true);
		parameter.setMode(SubscriptionMode.CREATE);
		acceptedParameters.add(parameter);
		resource.checkMandatoryParameters(parameters, acceptedParameters, SubscriptionMode.LINK);
	}

	@Test
	void checkMandatoryParametersMandatoryMode() {
		final List<ParameterValueCreateVo> parameters = new ArrayList<>();
		final List<Parameter> acceptedParameters = new ArrayList<>();
		final var parameter = new Parameter();
		parameter.setId("p");
		parameter.setMandatory(true);
		parameter.setMode(SubscriptionMode.CREATE);
		acceptedParameters.add(parameter);
		Assertions.assertThrows(ValidationJsonException.class, () -> resource.checkMandatoryParameters(parameters, acceptedParameters, SubscriptionMode.CREATE));
	}

	@Test
	void getParameters() {
		final var parameters = resource.getParameters(subscription);
		Assertions.assertNull(parameters.get("service:bt:jira:jdbc-user"));
		Assertions.assertNull(parameters.get("service:bt:jira:jdbc-password"));
		Assertions.assertEquals("jdbc:hsqldb:mem:dataSource", parameters.get("service:bt:jira:jdbc-url"));
		Assertions.assertEquals("org.hsqldb.jdbc.JDBCDriver", parameters.get("service:bt:jira:jdbc-driver"));
		Assertions.assertEquals("10074", parameters.get("service:bt:jira:project"));
		Assertions.assertEquals("MDA", parameters.get("service:bt:jira:pkey"));
		Assertions.assertEquals("http://localhost:8120", parameters.get("service:bt:jira:url"));
	}

	@Test
	void getConfiguration() throws Exception {
		final int entity = repository.findByExpected("node.id", "service:vm:vcloud:sample").getId();
		final var configuration = resource.getConfiguration(entity);

		// Not secured parameter
		Assertions.assertEquals("http://localhost:8120", configuration.getParameters().get("service:vm:vcloud:url"));

		// Secured parameter
		Assertions.assertNull(configuration.getParameters().get("service:vm:vcloud:user"));
		Assertions.assertEquals(entity, configuration.getSubscription());
		Assertions.assertEquals("Jupiter", configuration.getProject().getName());
		Assertions.assertEquals("service:vm:vcloud:sample", configuration.getNode().getId());
		Assertions.assertNotNull(configuration.getConfiguration());
	}

	@Test
	void getConfigurationNone() throws Exception {
		final int entity = repository.findByExpected("node.id", "service:km:confluence:dig").getId();
		final var configuration = resource.getConfiguration(entity);
		Assertions.assertEquals(entity, configuration.getSubscription());
		Assertions.assertEquals("Jupiter", configuration.getProject().getName());
		Assertions.assertEquals("service:km:confluence:dig", configuration.getNode().getId());

		// No configuration for Confluence
		Assertions.assertNull(configuration.getConfiguration());
	}

	@Test
	void getNonSecuredParameters() {
		final var parameters = resource.getNonSecuredParameters(getSubscription("Jupiter"));
		Assertions.assertNull(parameters.get("service:bt:jira:jdbc-user"));
		Assertions.assertNull(parameters.get("service:bt:jira:jdbc-password"));
		Assertions.assertNull(parameters.get("service:bt:jira:jdbc-url"));
		Assertions.assertNull(parameters.get("service:bt:jira:jdbc-driver"));
		Assertions.assertNull(parameters.get("service:bt:jira:user"));
		Assertions.assertNull(parameters.get("service:bt:jira:password"));
		Assertions.assertEquals("10000", parameters.get("service:bt:jira:project"));
		Assertions.assertEquals("JUPITER", parameters.get("service:bt:jira:pkey"));
		Assertions.assertEquals("http://localhost:8120", parameters.get("service:bt:jira:url"));
	}

	@Test
	void getParametersNotVisibleProject() {
		initSpringSecurityContext("any");
		Assertions.assertThrows(EntityNotFoundException.class, () -> resource.getParameters(subscription));
	}

	/**
	 * Not a {@link LongTaskRunner} implementation -> does nothing
	 */
	@Test
	void deleteTasksNoTaskRunner() {
		resource.deleteTasks(Mockito.mock(ServicePlugin.class), 0);
	}

	@Autowired
	private TaskSampleSubscriptionRepository taskSampleRepository;

	@Test
	void deleteTasksSubscription() {
		final var sampleResource = registerSingleton("taskSampleResource",
				applicationContext.getAutowireCapableBeanFactory().createBean(TaskSampleSubscriptionResource.class));

		try {
			final var entity = new TaskSampleSubscription();
			entity.setLocked(repository.findOne(subscription));
			entity.setStart(new Date());
			entity.setAuthor(DEFAULT_USER);
			taskSampleRepository.saveAndFlush(entity);
			Assertions.assertNotNull(taskSampleRepository.findNotFinishedByLocked(subscription));
			entity.setEnd(new Date());
			taskSampleRepository.saveAndFlush(entity);
			Assertions.assertNull(taskSampleRepository.findNotFinishedByLocked(subscription));
			Assertions.assertEquals(1, taskSampleRepository.count());
			resource.deleteTasks(sampleResource, subscription);
			Assertions.assertNull(taskSampleRepository.findNotFinishedByLocked(subscription));
			Assertions.assertEquals(0, taskSampleRepository.count());
		} finally {
			destroySingleton("taskSampleResource");
		}
	}

	@Test
	void deleteNotVisibleProject() {
		final var one = repository.findOne(subscription);
		final int project = one.getProject().getId();
		Assertions.assertEquals(1, repository.findAllByProject(project).size());
		em.clear();
		initSpringSecurityContext("any");
		Assertions.assertThrows(EntityNotFoundException.class, () -> resource.delete(subscription));
	}

	@Test
	void deleteByAdmin() throws Exception {
		initSpringSecurityContext("junit");
		assertDelete();
	}

	/**
	 * Can delete since team leader
	 */
	@Test
	void deleteByTeamLeader() throws Exception {
		initSpringSecurityContext("fdaugan");
		delegateOrgRepository.findAll().forEach(d -> d.setCanWrite(false));
		projectRepository.findAll().forEach(d -> d.setTeamLeader("fdaugan"));
		newDelegateNode();
		assertDelete();
	}

	/**
	 * Delete since manage the main group
	 */
	@Test
	void deleteByGroupManager() throws Exception {
		initSpringSecurityContext("fdaugan");
		delegateOrgRepository.findAll().forEach(d -> d.setCanAdmin(false));
		projectRepository.findAll().forEach(d -> d.setTeamLeader(null));

		// Persist the delegate and the related group to the project
		final var delegate = prepareDelegate();
		delegate.setCanWrite(true);
		em.flush();

		assertDelete();
	}

	/**
	 * Project is visible and the related group is writable but principal has no right on node.
	 */
	@Test
	void deleteNoProjectRight() {
		initSpringSecurityContext("fdaugan");
		delegateNodeRepository.findAll().forEach(d -> d.setCanSubscribe(false));
		projectRepository.findAll().forEach(d -> d.setTeamLeader(null));

		// Persist the delegate and the related group to the project
		final var delegate = prepareDelegate();
		delegate.setCanWrite(true);
		em.flush();
		em.clear();
		Assertions.assertThrows(ForbiddenException.class, () -> resource.delete(subscription));
	}

	/**
	 * Project is visible and the related group is not writable but principal has right on node.
	 */
	@Test
	void deleteNoDelegateSubscribe() {
		initSpringSecurityContext("fdaugan");
		delegateOrgRepository.findAll().forEach(d -> d.setCanAdmin(false));
		projectRepository.findAll().forEach(d -> d.setTeamLeader(null));

		// Persist the delegate and the related group to the project
		final var delegate = prepareDelegate();
		delegate.setCanWrite(false);

		newDelegateNode();

		em.flush();
		em.clear();
		Assertions.assertThrows(ForbiddenException.class, () -> resource.delete(subscription));
	}

	private DelegateNode newDelegateNode() {
		final var delegateNode = new DelegateNode();
		delegateNode.setReceiver("fdaugan");
		delegateNode.setReceiverType(ReceiverType.USER);
		delegateNode.setNode("service");
		delegateNode.setCanSubscribe(true);
		delegateNode.setCanWrite(false);
		delegateNode.setCanAdmin(false);
		delegateNode.setName("service");
		delegateNodeRepository.save(delegateNode);
		return delegateNode;
	}

	private DelegateOrg prepareDelegate() {

		// Persist the delegate and the related group to the project
		final var projectGroup = new CacheProjectGroup();
		final var group = new CacheGroup();
		group.setId("group-project");
		group.setName("group-project");
		group.setDescription("cn=group-project,ou=parent");
		em.persist(group);
		final var membership = new CacheMembership();
		membership.setGroup(group);
		membership.setUser(em.find(CacheUser.class, "fdaugan"));
		em.persist(membership);
		projectGroup.setGroup(group);
		projectGroup.setProject(repository.findOne(subscription).getProject());
		em.persist(projectGroup);

		final var delegate = new DelegateOrg();
		delegate.setReceiver("fdaugan");
		delegate.setReceiverType(ReceiverType.USER);
		delegate.setReceiverDn("uid=fdaugan,ou=company");
		delegate.setType(DelegateType.GROUP);
		delegate.setName("group-project");
		delegate.setDn("cn=group-project,ou=parent");
		em.persist(delegate);
		em.flush();

		return delegate;
	}

	private void assertDelete() throws Exception {
		final var one = repository.findOne(subscription);
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
	void createNotVisibleProject() {

		// Test a creation by another user than the team leader and a manager
		initSpringSecurityContext("any");
		Assertions.assertThrows(EntityNotFoundException.class, this::create);
	}

	/**
	 * Test a creation by another user than the team leader. The current user is administrator, can see all projects.
	 */
	@Test
	void createByAdmin() throws Exception {
		initSpringSecurityContext(DEFAULT_USER);
		create();
	}

	@Test
	void create() throws Exception {
		final var entity = resource.create(newCreateVo());
		em.flush();
		em.clear();

		Assertions.assertEquals("10074", parameterValueRepository.getSubscriptionParameterValue(entity,
				JiraBaseResource.PARAMETER_PROJECT));
		Assertions.assertEquals("MDA",
				parameterValueRepository.getSubscriptionParameterValue(subscription, JiraBaseResource.PARAMETER_PKEY));
		Assertions.assertEquals(SubscriptionMode.LINK, repository.findOneExpected(entity).getMode());
	}

	/**
	 * The project is visible for user "admin-test" since he is in the main group "ligoj-jupiter" of the project
	 * "ligoj-jupiter", however he is neither the team leader of this project, neither an administrator, neither a manger
	 * of the group "ligoj-jupiter".
	 */
	@Test
	void createNotManagedProject() {
		initSpringSecurityContext("admin-test");
		final var vo = new SubscriptionEditionVo();
		vo.setParameters(new ArrayList<>());
		vo.setNode("service:bt:jira:4");
		vo.setProject(em.createQuery("SELECT id FROM Project WHERE name='Jupiter'", Integer.class).getSingleResult());

		Assertions.assertThrows(ForbiddenException.class, () -> resource.create(vo));
	}

	/**
	 * Principal is the team leader of target project, but can subscribe no node.
	 */
	@Test
	void createNotSubscribeRightOnThisNode() {
		initSpringSecurityContext("user1");
		delegateNodeRepository.findAll().forEach(d -> d.setCanSubscribe(false));

		// Make the project visible for this user
		final var project = projectRepository.findByName("Jupiter");
		project.setTeamLeader("user1");
		em.merge(project);

		final var vo = new SubscriptionEditionVo();
		vo.setNode("service:bt:jira:4");
		vo.setProject(project.getId());
		Assertions.assertThrows(ForbiddenException.class, () -> resource.create(vo));
	}

	/**
	 * Principal is the team leader of target project, can subscribe to at least one node but not the target one.
	 */
	@Test
	void createNoSubscribeRight() {
		initSpringSecurityContext("user1");
		delegateNodeRepository.findAll().forEach(d -> d.setCanSubscribe(false));
		var delegateNode = newDelegateNode();
		delegateNode.setReceiver("user1");
		delegateNode.setNode("service:some:other");

		// Make the project visible for this user
		final var project = projectRepository.findByName("Jupiter");
		project.setTeamLeader("user1");
		em.merge(project);

		final var vo = new SubscriptionEditionVo();
		vo.setNode("service:bt:jira:4");
		vo.setProject(project.getId());
		Assertions.assertThrows(ValidationJsonException.class, () -> resource.create(vo));
	}

	@Test
	void createCreateModeNotSupported() {
		final var vo = newCreateVo();
		vo.setMode(SubscriptionMode.CREATE);
		Assertions.assertThrows(NotImplementedException.class, () -> resource.create(vo));
	}

	@Test
	void createNoneModeNotAllowed() {
		final var vo = newCreateVo();
		nodeRepository.findOne("service:bt:jira:4").setMode(SubscriptionMode.NONE);
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> resource.create(vo)),
				"node", "invalid-mode");
	}

	private SubscriptionEditionVo newCreateVo() {
		em.createQuery("DELETE Parameter WHERE id LIKE ?1").setParameter(1, "c_%").executeUpdate();

		final var vo = new SubscriptionEditionVo();
		final var parameters = new ArrayList<ParameterValueCreateVo>();
		final var parameterValueEditionVo = new ParameterValueCreateVo();
		parameterValueEditionVo.setParameter(JiraBaseResource.PARAMETER_PROJECT);
		parameterValueEditionVo.setInteger(10074);
		parameters.add(parameterValueEditionVo);
		final var parameterValueEditionVo2 = new ParameterValueCreateVo();
		parameterValueEditionVo2.setParameter(JiraBaseResource.PARAMETER_PKEY);
		parameterValueEditionVo2.setText("MDA");
		parameters.add(parameterValueEditionVo2);
		vo.setParameters(parameters);
		vo.setNode("service:bt:jira:4");
		vo.setMode(SubscriptionMode.LINK);
		vo.setProject(em.createQuery("SELECT id FROM Project WHERE name='Jupiter'", Integer.class).getSingleResult());
		return vo;
	}

	@Test
	void createCreateMode() throws Exception {
		// Prepare data
		final var entity = createCreateBase("ligoj-jupiter", "ligoj-jupiter-client");
		Assertions.assertEquals("ligoj-jupiter", parameterValueRepository.getSubscriptionParameterValue(entity,
				IdentityResource.PARAMETER_PARENT_GROUP));
	}

	private int createCreateBase(final String parent, final String group) throws Exception {
		em.createQuery("DELETE Parameter WHERE id LIKE ?1").setParameter(1, "c_%").executeUpdate();
		final var vo = new SubscriptionEditionVo();
		final var parameters = getParameterValueCreateVos(parent, group);

		vo.setMode(SubscriptionMode.CREATE);
		vo.setParameters(parameters);
		vo.setNode("service:id:ldap:dig");
		vo.setProject(em.createQuery("SELECT id FROM Project WHERE name='Jupiter'", Integer.class).getSingleResult());

		initSpringSecurityContext(DEFAULT_USER);
		final var entity = resource.create(vo);

		Assertions.assertEquals(group,
				parameterValueRepository.getSubscriptionParameterValue(entity, IdentityResource.PARAMETER_GROUP));
		Assertions.assertEquals("ligoj",
				parameterValueRepository.getSubscriptionParameterValue(entity, IdentityResource.PARAMETER_OU));
		return entity;
	}

	private static ArrayList<ParameterValueCreateVo> getParameterValueCreateVos(String parent, String group) {
		final var parameters = new ArrayList<ParameterValueCreateVo>();
		final var parameterValueEditionVo = new ParameterValueCreateVo();
		parameterValueEditionVo.setParameter(IdentityResource.PARAMETER_OU);
		parameterValueEditionVo.setText("ligoj");
		parameters.add(parameterValueEditionVo);
		final var parameterValueEditionVo2 = new ParameterValueCreateVo();
		parameterValueEditionVo2.setParameter(IdentityResource.PARAMETER_PARENT_GROUP);
		parameterValueEditionVo2.setText(parent);
		parameters.add(parameterValueEditionVo2);
		final var parameterValueEditionVo3 = new ParameterValueCreateVo();
		parameterValueEditionVo3.setParameter(IdentityResource.PARAMETER_GROUP);
		parameterValueEditionVo3.setText(group);
		parameters.add(parameterValueEditionVo3);
		return parameters;
	}

	/**
	 * Create mode, blank optional parameter
	 */
	@Test
	void createCreateModeBlank() throws Exception {
		final var entity = createCreateBase("", "ligoj-jupiter-client2");
		Assertions.assertNull(parameterValueRepository.getSubscriptionParameterValue(entity, IdentityResource.PARAMETER_PARENT_GROUP));
	}

	@Test
	void createNotAcceptedParameter() {
		final var vo = newCreateVoBadParameters();
		final var parameterValueEditionVo2 = new ParameterValueCreateVo();
		parameterValueEditionVo2.setParameter("service:bt:jira:pkey");
		parameterValueEditionVo2.setText("MY_PROJECT");
		vo.getParameters().add(parameterValueEditionVo2);

		final var parameterValueEditionVo3 = new ParameterValueCreateVo();
		parameterValueEditionVo3.setParameter("service:bt:jira:jdbc-password");
		parameterValueEditionVo3.setInteger(1007400);
		vo.getParameters().add(parameterValueEditionVo3);

		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> resource.create(vo)),
				JiraBaseResource.PARAMETER_JDBC_PASSWORD, "not-accepted-parameter");
	}

	@Test
	void createMissingParameter() {
		final var newCreateVoBadParameters = newCreateVoBadParameters();
		MatcherUtil.assertThrows(
				Assertions.assertThrows(ValidationJsonException.class, () -> resource.create(newCreateVoBadParameters)),
				"service:bt:jira:pkey", "NotNull");
	}

	private SubscriptionEditionVo newCreateVoBadParameters() {
		em.createQuery("DELETE Parameter WHERE id LIKE ?1").setParameter(1, "c_%").executeUpdate();

		final var project = new Project();
		project.setName("TEST");
		project.setPkey("test");
		project.setTeamLeader(getAuthenticationName());
		em.persist(project);

		final var vo = new SubscriptionEditionVo();
		final var parameters = new ArrayList<ParameterValueCreateVo>();
		final var parameterValueEditionVo = new ParameterValueCreateVo();
		parameterValueEditionVo.setParameter("service:bt:jira:project");
		parameterValueEditionVo.setInteger(1007400);
		parameters.add(parameterValueEditionVo);

		vo.setParameters(parameters);
		vo.setNode("service:bt:jira:4");
		vo.setProject(project.getId());
		vo.setMode(SubscriptionMode.CREATE);
		return vo;
	}

	@Test
	void findAll() {
		final var subscriptionList = resource.findAll();

		// Check nodes
		final var nodes = subscriptionList.getNodes();
		Assertions.assertTrue(nodes.size() > 30);
		final var subscribedNodes = new ArrayList<>(nodes);
		Assertions.assertEquals("service:bt", subscribedNodes.getFirst().getId());
		Assertions.assertNull(subscribedNodes.getFirst().getRefined());
		Assertions.assertEquals("Bug Tracker", subscribedNodes.getFirst().getName());
		Assertions.assertEquals("functional", subscribedNodes.getFirst().getTag());
		Assertions.assertEquals("fa fa-suitcase", subscribedNodes.getFirst().getTagUiClasses());

		Assertions.assertEquals("service:bt:jira", subscribedNodes.get(1).getId());
		Assertions.assertEquals("service:bt", subscribedNodes.get(1).getRefined());
		Assertions.assertEquals("JIRA", subscribedNodes.get(1).getName());
		Assertions.assertNull(subscribedNodes.get(1).getTag());
		Assertions.assertNull(subscribedNodes.get(1).getTagUiClasses());

		// Check subscriptions
		Assertions.assertTrue(subscriptionList.getSubscriptions().size() >= 13);
		final var subscriptions = new ArrayList<>(subscriptionList.getSubscriptions());

		// Check project order

		// G-STACK Project
		final var subscription0 = subscriptions.getFirst();
		Assertions.assertTrue(subscription0.getId() > 0);
		Assertions.assertTrue(subscription0.getProject() > 0);
		Assertions.assertEquals("Jupiter", subscriptionList.getProjects().stream()
				.filter(p -> p.getId().equals(subscription0.getProject())).findFirst().get().getName());

		// MDA Project (last subscription), only JIRA4 subscription
		final var entity = subscriptions.getLast();
		Assertions.assertTrue(entity.getId() > 0);
		Assertions.assertTrue(entity.getProject() > 0);
		Assertions.assertEquals("MDA", subscriptionList.getProjects().stream()
				.filter(p -> p.getId().equals(entity.getProject())).findFirst().get().getName());
		Assertions.assertEquals("service:bt:jira:4", entity.getNode());

		// Check projects
		final var projects = subscriptionList.getProjects();
		Assertions.assertEquals(2, projects.size());

		// Check subscription project
		final var projectVo = projects.stream().filter(p -> Objects.equals(p.getId(), entity.getProject()))
				.findFirst().get();
		Assertions.assertEquals("MDA", projectVo.getName());
		Assertions.assertEquals("mda", projectVo.getPkey());

		// Check subscription node
		final var nodeVo = nodes.stream().filter(p -> Objects.equals(p.getId(), entity.getNode())).findFirst()
				.get();
		Assertions.assertEquals("JIRA 4", nodeVo.getName());
		Assertions.assertEquals("service:bt:jira:4", nodeVo.getId());

	}

	@Test
	void refreshStatuses() throws IOException {
		persistEntities("csv", new Class<?>[]{Event.class}, StandardCharsets.UTF_8);
		final var projectId = projectRepository.findByName("MDA").getId();
		final var subscriptionStatus = resource.getStatusByProject(projectId);
		Assertions.assertEquals(1, subscriptionStatus.size());
		final var statuses = resource.refreshStatuses(Collections.singleton(subscription));
		Assertions.assertEquals(1, statuses.size());
		final var status = statuses.get(subscription);
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
	void getStatusByProject() throws IOException {
		persistEntities("csv", new Class<?>[]{Event.class}, StandardCharsets.UTF_8);
		final var projectId = projectRepository.findByName("Jupiter").getId();
		final var subscriptionStatus = resource.getStatusByProject(projectId);
		Assertions.assertEquals(1, subscriptionStatus.size());
		Assertions.assertEquals("JIRA 6", subscriptionStatus.values().iterator().next().getNode().getName());
	}

	@Test
	void checkSubscriptionStatus() {
		final var service = servicePluginLocator.getResource("service:bt:jira:4", JiraPluginResource.class);
		final var status = service.checkSubscriptionStatus(null);
		Assertions.assertNotNull(status);
		Assertions.assertEquals("value", status.getData().get("property"));
	}

	@Test
	void servicePlugin() {
		final var service = servicePluginLocator.getResource("service:bt:jira:4", JiraPluginResource.class);
		Assertions.assertEquals("Jira", service.getName());
		Assertions.assertNull(service.getVendor());
		Assertions.assertNull(service.getVersion());
		Assertions.assertEquals(0, service.compareTo(service));
	}

	@Test
	void checkStatus() {
		Assertions.assertTrue(
				servicePluginLocator.getResource("service:bt:jira:4", JiraPluginResource.class).checkStatus(null));
	}

}
