package org.ligoj.app.resource.node;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.persistence.EntityNotFoundException;
import javax.transaction.Transactional;

import org.jasypt.encryption.StringEncryptor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.ligoj.app.AbstractAppTest;
import org.ligoj.app.dao.ParameterRepository;
import org.ligoj.app.dao.ParameterValueRepository;
import org.ligoj.app.iam.model.ReceiverType;
import org.ligoj.app.model.DelegateNode;
import org.ligoj.app.model.Node;
import org.ligoj.app.model.Parameter;
import org.ligoj.app.model.ParameterValue;
import org.ligoj.app.model.Project;
import org.ligoj.app.model.Subscription;
import org.ligoj.app.resource.node.sample.BugTrackerResource;
import org.ligoj.app.resource.node.sample.BuildResource;
import org.ligoj.app.resource.node.sample.JiraBaseResource;
import org.ligoj.bootstrap.core.resource.BusinessException;
import org.ligoj.bootstrap.core.resource.TechnicalException;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.ligoj.bootstrap.model.system.SystemRole;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * {@link ParameterValueResource} test cases.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ParameterValueResourceTest extends AbstractAppTest {

	@Autowired
	private ParameterValueRepository repository;

	@Autowired
	private ParameterRepository parameterRepository;

	@Autowired
	private ParameterValueResource resource;

	@Autowired
	private StringEncryptor encryptor;

	@Before
	public void prepare() throws IOException {
		persistEntities("csv", new Class[] { Node.class, Parameter.class, Project.class, Subscription.class, ParameterValue.class },
				StandardCharsets.UTF_8.name());
		persistSystemEntities();

		// For JPA coverage
		repository.findAll().iterator().next().getSubscription();
	}

	@Test
	public void createText() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_2").getId());
		parameterValue.setText("value");
		final ParameterValue entity = resource.createInternal(parameterValue);

		Assert.assertEquals(parameterValue.getParameter(), entity.getParameter().getId());
		Assert.assertEquals("value", entity.getData());
	}

	@Test
	public void createSecured() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("service:bt:jira:jdbc-url").getId());
		parameterValue.setText("value");
		final ParameterValue entity = resource.createInternal(parameterValue);
		Assert.assertTrue(
				entity.toString().startsWith("ParameterValue(parameter=AbstractBusinessEntity(id=service:bt:jira:jdbc-url), data="));
		Assert.assertEquals(parameterValue.getParameter(), entity.getParameter().getId());
		Assert.assertNotEquals("value", entity.getData());
		Assert.assertEquals("value", encryptor.decrypt(entity.getData()));
	}

	@Test
	public void createAlreadySecured() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("service:bt:jira:jdbc-url").getId());
		parameterValue.setText(encryptor.encrypt("value"));
		final ParameterValue entity = resource.createInternal(parameterValue);

		Assert.assertEquals(parameterValue.getParameter(), entity.getParameter().getId());
		Assert.assertNotEquals("value", entity.getData());
		Assert.assertEquals(parameterValue.getText(), entity.getData());
		Assert.assertEquals("value", encryptor.decrypt(entity.getData()));
	}

	@Test
	public void createTextPattern() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_17").getId());
		parameterValue.setText("va-l-u-9e");
		final ParameterValue entity = resource.createInternal(parameterValue);

		Assert.assertEquals(parameterValue.getParameter(), entity.getParameter().getId());
		Assert.assertEquals("va-l-u-9e", entity.getData());
	}

	@Test
	public void createTextEmptyPattern() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_170").getId());
		parameterValue.setText("va-l-u-9e");
		final ParameterValue entity = resource.createInternal(parameterValue);

		Assert.assertEquals(parameterValue.getParameter(), entity.getParameter().getId());
		Assert.assertEquals("va-l-u-9e", entity.getData());
	}

	@Test(expected = ValidationJsonException.class)
	public void createTextPatternFailed() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_17").getId());
		parameterValue.setText("1a");
		final ParameterValue entity = resource.createInternal(parameterValue);

		Assert.assertEquals(parameterValue.getParameter(), entity.getParameter().getId());
		Assert.assertEquals("1a", entity.getData());
	}

	@Test
	public void createTextEmpty() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_17").getId());
		final ParameterValue entity = resource.createInternal(parameterValue);

		Assert.assertEquals(parameterValue.getParameter(), entity.getParameter().getId());
		Assert.assertNull(entity.getData());
	}

	@Test
	public void createIndex() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_3").getId());
		parameterValue.setIndex(1);
		final ParameterValue entity = resource.createInternal(parameterValue);
		Assert.assertEquals(parameterValue.getParameter(), entity.getParameter().getId());
		Assert.assertEquals("1", entity.getData());
	}

	@Test(expected = ValidationJsonException.class)
	public void createIndexNull() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_3").getId());
		resource.createInternal(parameterValue);
	}

	@Test(expected = ValidationJsonException.class)
	public void createIndexMin() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_3").getId());
		parameterValue.setIndex(-1);
		resource.createInternal(parameterValue);
	}

	@Test(expected = ValidationJsonException.class)
	public void createIndexMax() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_3").getId());
		parameterValue.setIndex(3);
		resource.createInternal(parameterValue);
	}

	@Test
	public void createInteger() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_4").getId());
		parameterValue.setInteger(1);
		final ParameterValue entity = resource.createInternal(parameterValue);
		Assert.assertEquals(parameterValue.getParameter(), entity.getParameter().getId());
		Assert.assertEquals("1", entity.getData());
	}

	@Test
	public void createIntegerNoConstraint() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_19").getId());
		parameterValue.setInteger(-100);
		final ParameterValue entity = resource.createInternal(parameterValue);
		Assert.assertEquals(parameterValue.getParameter(), entity.getParameter().getId());
		Assert.assertEquals("-100", entity.getData());
	}

	@Test(expected = ValidationJsonException.class)
	public void createIntegerNull() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_4").getId());
		resource.createInternal(parameterValue);
	}

	@Test(expected = ValidationJsonException.class)
	public void createIntegerMin() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_4").getId());
		parameterValue.setInteger(-1);
		final ParameterValue entity = resource.createInternal(parameterValue);
		Assert.assertEquals(parameterValue.getParameter(), entity.getParameter().getId());
		Assert.assertEquals("1", entity.getData());
	}

	@Test(expected = ValidationJsonException.class)
	public void createIntegerMax() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_4").getId());
		parameterValue.setInteger(100);
		resource.createInternal(parameterValue);
	}

	@Test(expected = ValidationJsonException.class)
	public void createMissingProject() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_4").getId());
		parameterValue.setInteger(0);
		resource.createInternal(parameterValue);
	}

	@Test
	public void createBoolean() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_5").getId());
		parameterValue.setBool(Boolean.TRUE);
		final ParameterValue entity = resource.createInternal(parameterValue);
		Assert.assertEquals(parameterValue.getParameter(), entity.getParameter().getId());
		Assert.assertEquals(parameterValue.getBool().toString(), entity.getData());
	}

	@Test(expected = ValidationJsonException.class)
	public void createBooleanNull() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_5").getId());
		resource.createInternal(parameterValue);
	}

	@Test
	public void createDate() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_6").getId());
		parameterValue.setDate(new Date());
		final ParameterValue entity = resource.createInternal(parameterValue);
		Assert.assertEquals(parameterValue.getParameter(), entity.getParameter().getId());
		Assert.assertEquals(String.valueOf(parameterValue.getDate().getTime()), entity.getData());
	}

	@Test(expected = ValidationJsonException.class)
	public void createDateInvalid() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_6").getId());
		final Date date = new Date();
		date.setTime(0);
		parameterValue.setDate(date);
		resource.createInternal(parameterValue);
	}

	@Test(expected = ValidationJsonException.class)
	public void createDateNull() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_6").getId());
		resource.createInternal(parameterValue);
	}

	@Test
	public void createTags() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_22").getId());
		final List<String> tags = new ArrayList<>();
		tags.add("value1");
		tags.add("valueX");
		parameterValue.setTags(tags);
		final ParameterValue entity = resource.createInternal(parameterValue);
		Assert.assertEquals(parameterValue.getParameter(), entity.getParameter().getId());
		Assert.assertEquals("[\"VALUE1\",\"VALUEX\"]", entity.getData());
	}

	@Test(expected = ValidationJsonException.class)
	public void createTagsEmpty() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_22").getId());
		final List<String> tags = new ArrayList<>();
		tags.add("\t");
		parameterValue.setTags(tags);
		resource.createInternal(parameterValue);
	}

	@Test(expected = ValidationJsonException.class)
	public void createTagsNull() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_22").getId());
		resource.createInternal(parameterValue);
	}

	@Test
	public void createMultiple() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_23").getId());
		final List<Integer> tags = new ArrayList<>();
		tags.add(1);
		tags.add(2);
		parameterValue.setSelections(tags);
		final ParameterValue entity = resource.createInternal(parameterValue);
		Assert.assertEquals(parameterValue.getParameter(), entity.getParameter().getId());
		Assert.assertEquals("[1,2]", entity.getData());
	}

	@Test(expected = ValidationJsonException.class)
	public void createMultipleNull() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_23").getId());
		resource.createInternal(parameterValue);
	}

	@Test(expected = ValidationJsonException.class)
	public void createMultipleInvalidIndex1() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_23").getId());
		final List<Integer> tags = new ArrayList<>();
		tags.add(-1);
		parameterValue.setSelections(tags);
		resource.createInternal(parameterValue);
	}

	@Test(expected = ValidationJsonException.class)
	public void createMultipleInvalidIndex2() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_23").getId());
		final List<Integer> tags = new ArrayList<>();
		tags.add(8);
		parameterValue.setSelections(tags);
		resource.createInternal(parameterValue);
	}

	@Test(expected = ValidationJsonException.class)
	public void createTooManyValues() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_23").getId());
		final List<Integer> tags = new ArrayList<>();
		parameterValue.setSelections(tags);
		parameterValue.setText("ignore but dirty");
		resource.createInternal(parameterValue);
	}

	@Test(expected = TechnicalException.class)
	public void createSelectJSonError() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_22").getId());
		@SuppressWarnings("cast")
		final List<String> badList = (List<String>) Mockito.mock(List.class);
		Mockito.when(badList.isEmpty()).thenReturn(Boolean.FALSE);
		Mockito.when(badList.iterator()).thenThrow(new IllegalStateException());
		parameterValue.setTags(badList);
		ParameterValueResource.toData(parameterValue);
	}

	@Test
	public void findText() {
		final Parameter parameter = parameterRepository.findOne("c_2");
		final ParameterValue parameterValueEntity = newParameterValue("value", parameter);
		em.persist(parameterValueEntity);
		em.flush();

		final ParameterValueVo valueVo = resource.toVo(parameterValueEntity);
		Assert.assertEquals(parameter.getId(), valueVo.getParameter().getId());
		Assert.assertEquals(parameterValueEntity.getData(), valueVo.getText());
		Assert.assertNotNull(valueVo.getCreatedDate());
		Assert.assertNotNull(valueVo.getCreatedBy());
	}

	@Test
	public void findInteger() {
		final Parameter parameter = parameterRepository.findOne("c_4");
		final ParameterValue parameterValueEntity = newParameterValue("1", parameter);
		em.persist(parameterValueEntity);
		em.flush();

		final ParameterValueVo valueVo = resource.toVo(parameterValueEntity);
		Assert.assertEquals(parameter.getId(), valueVo.getParameter().getId());
		Assert.assertEquals(1, valueVo.getInteger().intValue());
		Assert.assertNotNull(valueVo.getCreatedDate());
		Assert.assertNotNull(valueVo.getCreatedBy());
	}

	@Test
	public void findIntegerNoData() {
		final Parameter parameter = parameterRepository.findOne("c_4");
		parameter.setData(null);
		final ParameterValue parameterValueEntity = newParameterValue("1", parameter);
		em.persist(parameterValueEntity);
		em.flush();

		final ParameterValueVo valueVo = resource.toVo(parameterValueEntity);
		Assert.assertEquals(parameter.getId(), valueVo.getParameter().getId());
		Assert.assertEquals(1, valueVo.getInteger().intValue());
		Assert.assertNotNull(valueVo.getCreatedDate());
		Assert.assertNotNull(valueVo.getCreatedBy());
	}

	@Test
	public void findSelect() {
		final Parameter parameter = parameterRepository.findOne("c_3");
		final ParameterValue parameterValueEntity = newParameterValue("1", parameter);
		em.persist(parameterValueEntity);
		em.flush();

		final ParameterValueVo valueVo = resource.toVo(parameterValueEntity);
		Assert.assertEquals(parameter.getId(), valueVo.getParameter().getId());
		Assert.assertEquals(1, valueVo.getIndex().intValue());
		Assert.assertNotNull(valueVo.getCreatedDate());
		Assert.assertNotNull(valueVo.getCreatedBy());
	}

	@Test
	public void findBool() {
		final Parameter parameter = parameterRepository.findOne("c_5");
		final ParameterValue parameterValueEntity = newParameterValue(Boolean.TRUE.toString(), parameter);
		em.persist(parameterValueEntity);
		em.flush();
		final ParameterValueVo valueVo = resource.toVo(parameterValueEntity);
		Assert.assertEquals(parameter.getId(), valueVo.getParameter().getId());
		Assert.assertTrue(valueVo.getBool());
		Assert.assertNotNull(valueVo.getCreatedDate());
		Assert.assertNotNull(valueVo.getCreatedBy());
	}

	@Test
	public void findDate() {
		final Parameter parameter = parameterRepository.findOne("c_6");
		final ParameterValue parameterValueEntity = newParameterValue(String.valueOf(new Date().getTime()), parameter);
		em.persist(parameterValueEntity);
		em.flush();

		final ParameterValueVo valueVo = resource.toVo(parameterValueEntity);
		Assert.assertEquals(parameter.getId(), valueVo.getParameter().getId());
		Assert.assertTrue(valueVo.getParameter().isMandatory());
		Assert.assertEquals(parameterValueEntity.getData(), String.valueOf(valueVo.getDate().getTime()));
		Assert.assertNotNull(valueVo.getCreatedDate());
		Assert.assertNotNull(valueVo.getCreatedBy());
	}

	@Test
	public void findMultiple() {
		final Parameter parameter = parameterRepository.findOne("c_23");
		final ParameterValue parameterValueEntity = newParameterValue("[0,2]", parameter);
		em.persist(parameterValueEntity);
		em.flush();

		final ParameterValueVo valueVo = resource.toVo(parameterValueEntity);
		Assert.assertEquals(parameter.getId(), valueVo.getParameter().getId());
		Assert.assertEquals("[0, 2]", valueVo.getSelections().toString());
		Assert.assertNotNull(valueVo.getCreatedDate());
		Assert.assertNotNull(valueVo.getCreatedBy());
	}

	@Test
	public void findTags() {
		final Parameter parameter = parameterRepository.findOne("c_22");
		final ParameterValue parameterValueEntity = newParameterValue("[\"A\",\"B\"]", parameter);
		em.persist(parameterValueEntity);
		em.flush();

		final ParameterValueVo valueVo = resource.toVo(parameterValueEntity);
		Assert.assertEquals(parameter.getId(), valueVo.getParameter().getId());
		Assert.assertEquals("[A, B]", valueVo.getTags().toString());
		Assert.assertNotNull(valueVo.getCreatedDate());
		Assert.assertNotNull(valueVo.getCreatedBy());
	}

	@Test
	public void findWithNode() {
		final Parameter parameter = parameterRepository.findOne("c_20");
		final ParameterValue parameterValueEntity = newParameterValue("true", parameter);
		parameterValueEntity.setNode(em.find(Node.class, "service:bt:jira:6"));
		em.persist(parameterValueEntity);
		em.flush();

		final ParameterValueVo valueVo = resource.toVo(parameterValueEntity);
		Assert.assertEquals(parameter.getId(), valueVo.getParameter().getId());
		Assert.assertEquals("service:bt:jira:6", valueVo.getNode().getId());
	}

	@Test(expected = TechnicalException.class)
	public void findInvalidJSonData() {
		final Parameter parameter = parameterRepository.findOne("c_22");
		final ParameterValue parameterValueEntity = newParameterValue("'", parameter);
		resource.toVo(parameterValueEntity);
	}

	@Test(expected = TechnicalException.class)
	public void findSelectJSonError() {
		final Parameter parameter = parameterRepository.findOne("c_4");
		parameter.setData("'{");
		final ParameterValue parameterValueEntity = newParameterValue("'", parameter);
		resource.toVo(parameterValueEntity);
	}

	private SystemRole newUser(final String name, final int id) {
		final SystemRole user = new SystemRole();
		user.setName(name);
		user.setId(id);
		return user;
	}

	private ParameterValue newParameterValue(final String data, final Parameter parameter) {
		final ParameterValue parameterValueEntity = new ParameterValue();
		parameterValueEntity.setParameter(parameter);
		parameterValueEntity.setData(data);
		return parameterValueEntity;
	}

	@Test
	public void toMap() {
		final List<SystemRole> values = new ArrayList<>();
		values.add(newUser("u1", 1));
		values.add(newUser("u2", 2));
		final Map<Integer, SystemRole> valuesAsMap = resource.toMap(values);
		Assert.assertEquals(2, valuesAsMap.size());
		Assert.assertEquals("u1", valuesAsMap.get(1).getName());
		Assert.assertEquals("u2", valuesAsMap.get(2).getName());
	}

	@Test
	public void toMapValues() {
		final List<ParameterValue> values = new ArrayList<>();
		final Parameter p1 = new Parameter();
		p1.setId("p1");
		final Parameter p2 = new Parameter();
		p2.setId("p2");
		values.add(newParameterValue("u1", p1));
		values.add(newParameterValue("u2", p2));
		final Map<String, String> valuesAsMap = resource.toMapValues(values);
		Assert.assertEquals(2, valuesAsMap.size());
		Assert.assertEquals("u1", valuesAsMap.get("p1"));
		Assert.assertEquals("u2", valuesAsMap.get("p2"));
	}

	@Test
	public void getNonSecuredParameters() throws Exception {
		final Map<String, String> parameters = resource.getNonSecuredSubscriptionParameters(getSubscription("MDA"));
		Assert.assertNull(parameters.get("service:bt:jira:jdbc-user"));
		Assert.assertNull(parameters.get("service:bt:jira:jdbc-password"));
		Assert.assertNull(parameters.get("service:bt:jira:jdbc-url"));
		Assert.assertNull(parameters.get("service:bt:jira:jdbc-driver"));
		Assert.assertNull(parameters.get("service:bt:jira:user"));
		Assert.assertNull(parameters.get("service:bt:jira:password"));
		Assert.assertEquals("10074", parameters.get("service:bt:jira:project"));
		Assert.assertEquals("MDA", parameters.get("service:bt:jira:pkey"));
		Assert.assertEquals("http://localhost:8120", parameters.get("service:bt:jira:url"));
	}

	@Test
	public void getParametersWithFilteredNull() throws Exception {
		final Map<String, String> parameters = resource.getSubscriptionParameters(getSubscription("MDA"));
		// User and password are empty, so not returned
		Assert.assertEquals(5, parameters.size());
		Assert.assertEquals("jdbc:hsqldb:mem:dataSource", parameters.get("service:bt:jira:jdbc-url"));
		Assert.assertEquals("org.hsqldb.jdbc.JDBCDriver", parameters.get("service:bt:jira:jdbc-driver"));
		Assert.assertEquals("10074", parameters.get("service:bt:jira:project"));
		Assert.assertEquals("MDA", parameters.get("service:bt:jira:pkey"));
		Assert.assertEquals("http://localhost:8120", parameters.get("service:bt:jira:url"));
	}

	@Test
	public void getParameters() throws Exception {
		final Map<String, String> parameters = resource.getSubscriptionParameters(getSubscription("gStack", BuildResource.SERVICE_KEY));
		Assert.assertEquals(4, parameters.size());
		Assert.assertEquals("junit", parameters.get("service:build:jenkins:user"));
		Assert.assertEquals("http://localhost:8120", parameters.get("service:build:jenkins:url"));
		Assert.assertEquals("secret", parameters.get("service:build:jenkins:api-token"));
		Assert.assertEquals("gfi-bootstrap", parameters.get("service:build:jenkins:job"));
	}

	@Test
	public void deleteBySubscription() throws Exception {
		final int subscription = getSubscription("MDA");

		// There are 5 parameters from the node, and 2 from the subscription
		Assert.assertEquals(7, repository.findAllBySubscription(subscription).size());
		resource.deleteBySubscription(subscription);
		em.flush();
		em.clear();

		// Check there are only 5 parameters, and only from the node
		final List<ParameterValue> parameters = repository.findAllBySubscription(subscription);
		Assert.assertEquals(5, parameters.size());
		Assert.assertEquals(5, parameters.stream().map(ParameterValue::getSubscription).filter(Objects::isNull).count());
	}

	@Test
	public void createInternal() throws Exception {
		em.createQuery("DELETE Parameter WHERE id LIKE ?1").setParameter(1, "c_%").executeUpdate();

		final List<ParameterValueCreateVo> parameters = new ArrayList<>();
		final ParameterValueCreateVo parameterValueEditionVo = new ParameterValueCreateVo();
		parameterValueEditionVo.setParameter(JiraBaseResource.PARAMETER_PROJECT);
		parameterValueEditionVo.setInteger(10074);
		parameters.add(parameterValueEditionVo);
		final Node node = new Node();
		node.setName("create-test");
		node.setId("create-test-id");
		em.persist(node);
		em.flush();
		em.clear();
		resource.create(parameters, node);

		em.flush();
		em.clear();

		final List<ParameterValue> values = repository.findAllBy("node.id", "create-test-id");
		Assert.assertEquals(1, values.size());
		Assert.assertEquals("10074", values.get(0).getData());
		Assert.assertEquals(JiraBaseResource.PARAMETER_PROJECT, values.get(0).getParameter().getId());
	}

	@Test(expected = EntityNotFoundException.class)
	public void deleteNotExist() throws Exception {
		resource.delete(-1);
	}

	@Test(expected = EntityNotFoundException.class)
	public void deleteNotVisible() throws Exception {
		initSpringSecurityContext("any");
		resource.delete(repository.findBy("parameter.id", "service:kpi:sonar:user").getId());
	}

	@Test(expected = EntityNotFoundException.class)
	public void deleteMandatoryUsed() throws Exception {
		final Integer id = repository.findBy("parameter.id", "service:kpi:sonar:project").getId();
		resource.delete(id);
	}

	@Test
	public void deleteMandatoryUnused() throws Exception {
		final ParameterValue value = newParameterValue();
		Assert.assertTrue(repository.exists(value.getId()));
		resource.delete(value.getId());
		Assert.assertFalse(repository.exists(value.getId()));
	}

	/**
	 * Create a new {@link ParameterValue} linked to a new {@link Node} without
	 * subscription.
	 */
	private ParameterValue newParameterValue() {
		final Node node = new Node();
		node.setName("create-test");
		node.setId("service:kpi:sonar:temp");
		node.setRefined(em.find(Node.class, "service:kpi:sonar"));
		em.persist(node);
		final ParameterValue value = new ParameterValue();
		value.setParameter(em.find(Parameter.class, "service:kpi:sonar:user"));
		value.setData("user");
		value.setNode(node);
		em.persist(value);
		em.flush();
		em.clear();
		return value;
	}

	@Test
	public void deleteNotMandatory() throws Exception {
		final Integer id = repository.findBy("parameter.id", "service:id:ldap:quarantine-dn").getId();
		Assert.assertTrue(repository.exists(id));
		resource.delete(id);
		Assert.assertFalse(repository.exists(id));
	}

	@Test
	public void create() throws Exception {
		repository.delete(repository.findBy("parameter.id", "service:id:ldap:quarantine-dn"));
		em.flush();
		em.clear();
		final ParameterValueNodeVo parameterValue = new ParameterValueNodeVo();
		parameterValue.setParameter("service:id:ldap:quarantine-dn");
		parameterValue.setText("ou=quarantine2,dc=sample,dc=com");
		parameterValue.setNode("service:id:ldap:dig");
		final int id = resource.create(parameterValue);

		final ParameterValue value = repository.findOneExpected(id);
		Assert.assertEquals("service:id:ldap:quarantine-dn", value.getParameter().getId());
		Assert.assertEquals("ou=quarantine2,dc=sample,dc=com", value.getData());
		Assert.assertEquals("service:id:ldap:dig", value.getNode().getId());
	}

	@Test(expected = EntityNotFoundException.class)
	public void createNotVisible() throws Exception {
		initSpringSecurityContext("any");
		final ParameterValueNodeVo parameterValue = new ParameterValueNodeVo();
		parameterValue.setParameter("service:id:ldap:quarantine-dn");
		parameterValue.setText("ou=quarantine2,dc=sample,dc=com");
		parameterValue.setNode("service:id:ldap:dig");
		resource.create(parameterValue);
	}

	@Test(expected = BusinessException.class)
	public void createNotWritable() throws Exception {
		DelegateNode delegateNode = new DelegateNode();
		delegateNode.setReceiver("user2");
		delegateNode.setReceiverType(ReceiverType.USER);
		delegateNode.setNode("service:id:ldap");
		em.persist(delegateNode);
		initSpringSecurityContext("user2"); // Read only user
		final ParameterValueNodeVo parameterValue = new ParameterValueNodeVo();
		parameterValue.setParameter("service:id:ldap:quarantine-dn");
		parameterValue.setText("ou=quarantine2,dc=sample,dc=com");
		parameterValue.setNode("service:id:ldap:dig");
		resource.create(parameterValue);
	}

	@Test
	public void update() throws Exception {
		ParameterValue value = newParameterValue();
		final ParameterValueNodeUpdateVo parameterValue = new ParameterValueNodeUpdateVo();
		String parameter = value.getParameter().getId();
		parameterValue.setParameter(parameter);
		parameterValue.setText("user2");
		parameterValue.setId(value.getId());
		Assert.assertEquals("ParameterValueCreateVo(parameter=service:kpi:sonar:user)", parameterValue.toString());
		resource.update(parameterValue);

		value = repository.findOneExpected(value.getId());
		Assert.assertEquals(parameter, value.getParameter().getId());
		Assert.assertEquals("user2", encryptor.decrypt(value.getData()));
		Assert.assertNotEquals("user2", value.getData());
	}

	@Test(expected = EntityNotFoundException.class)
	public void updateNotVisible() throws Exception {
		initSpringSecurityContext("any");
		ParameterValue value = repository.findBy("parameter.id", "service:id:ldap:quarantine-dn");
		final ParameterValueNodeUpdateVo parameterValue = new ParameterValueNodeUpdateVo();
		parameterValue.setParameter(value.getParameter().getId());
		parameterValue.setIndex(1);
		parameterValue.setId(value.getId());
		resource.update(parameterValue);
	}

	@Test(expected = EntityNotFoundException.class)
	public void updateNotExists() throws Exception {
		final ParameterValueNodeUpdateVo parameterValue = new ParameterValueNodeUpdateVo();
		parameterValue.setParameter("service:id:ldap:quarantine-dn");
		parameterValue.setIndex(1);
		parameterValue.setId(-1);
		resource.update(parameterValue);
	}

	@Test(expected = EntityNotFoundException.class)
	public void updateSubscriptionValue() throws Exception {
		ParameterValue value = repository.findBy("parameter.id", "service:kpi:sonar:project");
		final ParameterValueNodeUpdateVo parameterValue = new ParameterValueNodeUpdateVo();
		parameterValue.setParameter("service:kpi:sonar:project");
		parameterValue.setInteger(10);
		parameterValue.setId(value.getId());
		resource.update(parameterValue);
	}

	@Test(expected = BusinessException.class)
	public void updateSubscribedNode() throws Exception {
		ParameterValue value = repository.findBy("parameter.id", "service:kpi:sonar:user");
		final ParameterValueNodeUpdateVo parameterValue = new ParameterValueNodeUpdateVo();
		parameterValue.setParameter("service:kpi:sonar:user");
		parameterValue.setText("any");
		parameterValue.setId(value.getId());
		resource.update(parameterValue);
	}

	/**
	 * Return the subscription identifier of MDA. Assumes there is only one
	 * subscription for a service.
	 */
	protected int getSubscription(final String project) {
		return getSubscription(project, BugTrackerResource.SERVICE_KEY);
	}

	@Test(expected = BusinessException.class)
	public void checkOwnershipDisjunction() {
		final Node node = new Node();
		node.setId("service:id");
		final Parameter parameter = new Parameter();
		parameter.setOwner(node);
		final Node node2 = new Node();
		node2.setId("service:other");
		final Node node3 = new Node();
		node3.setId("service:other:sub");
		node3.setRefined(node2);
		resource.checkOwnership(parameter, node3);
	}

	@Test
	public void checkOwnership() {
		final Node node = new Node();
		node.setId("service:id");
		final Parameter parameter = new Parameter();
		parameter.setOwner(node);
		final Node node2 = new Node();
		node2.setId("service:id:sub");
		node2.setRefined(node);
		resource.checkOwnership(parameter, node2);
	}

	@Test(expected = BusinessException.class)
	public void updateListUntouchedNotExist() throws Exception {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setUntouched(true);
		parameterValue.setParameter("any");
		final List<ParameterValueCreateVo> values = Collections.singletonList(parameterValue);
		final Node node = new Node();
		node.setId("service:id:ldap");
		resource.update(values, node);
	}

	@Test
	public void updateListUntouchedExists() throws Exception {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setUntouched(true);
		parameterValue.setParameter("service:id:ldap:quarantine-dn");
		final List<ParameterValueCreateVo> values = Collections.singletonList(parameterValue);
		final Node node = new Node();
		node.setId("service:id:ldap:dig");
		resource.update(values, node);
	}
}
