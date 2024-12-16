/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.node;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.jasypt.encryption.StringEncryptor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ligoj.app.AbstractAppTest;
import org.ligoj.app.dao.ParameterRepository;
import org.ligoj.app.dao.ParameterValueRepository;
import org.ligoj.app.dao.ProjectRepository;
import org.ligoj.app.model.*;
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
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * {@link ParameterValueResource} test cases.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
class ParameterValueResourceTest extends AbstractAppTest {

	@Autowired
	private ParameterValueRepository repository;

	@Autowired
	private ParameterRepository parameterRepository;

	@Autowired
	private ParameterValueResource resource;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private StringEncryptor encryptor;

	@BeforeEach
	void prepare() throws IOException {
		persistEntities("csv",
				new Class<?>[]{Node.class, Parameter.class, Project.class, Subscription.class, ParameterValue.class},
				StandardCharsets.UTF_8);
		persistSystemEntities();

		// For JPA coverage
		//noinspection ResultOfMethodCallIgnored
		repository.findAll().getFirst().getSubscription();
	}

	private void assertValue(final String name, final String value) {
		final var parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne(name).getId());
		parameterValue.setText(value);
		final var entity = resource.createInternal(parameterValue);

		Assertions.assertEquals(parameterValue.getParameter(), entity.getParameter().getId());
		Assertions.assertEquals(value, entity.getData());
	}

	@Test
	void createText() {
		assertValue("c_2", "value");
	}

	@Test
	void createSecured() {
		final var parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("service:bt:jira:jdbc-url").getId());
		parameterValue.setText("value");
		final var entity = resource.createInternal(parameterValue);
		Assertions.assertTrue(entity.toString().contains("id=service:bt:jira:jdbc-url"));
		Assertions.assertEquals(parameterValue.getParameter(), entity.getParameter().getId());
		Assertions.assertNotEquals("value", entity.getData());
		Assertions.assertEquals("value", encryptor.decrypt(entity.getData()));
	}

	@Test
	void createAlreadySecured() {
		final var parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("service:bt:jira:jdbc-url").getId());
		parameterValue.setText(encryptor.encrypt("value"));
		final var entity = resource.createInternal(parameterValue);

		Assertions.assertEquals(parameterValue.getParameter(), entity.getParameter().getId());
		Assertions.assertNotEquals("value", entity.getData());
		Assertions.assertEquals(parameterValue.getText(), entity.getData());
		Assertions.assertEquals("value", encryptor.decrypt(entity.getData()));
	}

	@Test
	void createTextPattern() {
		assertValue("c_17", "va-l-u-9e");
	}

	@Test
	void createTextEmptyPattern() {
		assertValue("c_170", "va-l-u-9e");
	}

	@Test
	void createTextPatternFailed() {
		final var parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_17").getId());
		parameterValue.setText("1a");
		Assertions.assertThrows(ValidationJsonException.class, () -> resource.createInternal(parameterValue));
	}

	@Test
	void createTextEmpty() {
		final var parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_17").getId());
		Assertions.assertNull(resource.createInternal(parameterValue));
	}

	@Test
	void createIndex() {
		final var parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_3").getId());
		parameterValue.setIndex(1);
		final var entity = resource.createInternal(parameterValue);
		Assertions.assertEquals(parameterValue.getParameter(), entity.getParameter().getId());
		Assertions.assertEquals("1", entity.getData());
	}

	private void createIndexError(final String name, final Integer index) {
		final var parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne(name).getId());
		parameterValue.setIndex(index);
		Assertions.assertThrows(ValidationJsonException.class, () -> resource.createInternal(parameterValue));
	}

	private void createNull(final String name) {
		final var parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne(name).getId());
		Assertions.assertThrows(ValidationJsonException.class, () -> resource.createInternal(parameterValue));
	}

	@Test
	void createIndexNull() {
		createNull("c_3");
	}

	@Test
	void createIndexMin() {
		createIndexError("c_3", -1);
	}

	@Test
	void createIndexMax() {
		createIndexError("c_3", 3);
	}

	@Test
	void createInteger() {
		final var parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_4").getId());
		parameterValue.setInteger(1);
		final var entity = resource.createInternal(parameterValue);
		Assertions.assertEquals(parameterValue.getParameter(), entity.getParameter().getId());
		Assertions.assertEquals("1", entity.getData());
	}

	@Test
	void createIntegerNoConstraint() {
		final var parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_19").getId());
		parameterValue.setInteger(-100);
		final var entity = resource.createInternal(parameterValue);
		Assertions.assertEquals(parameterValue.getParameter(), entity.getParameter().getId());
		Assertions.assertEquals("-100", entity.getData());
	}

	private void createIntegerError(final String name, final Integer integer) {
		final var parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne(name).getId());
		parameterValue.setInteger(integer);
		Assertions.assertThrows(ValidationJsonException.class, () -> resource.createInternal(parameterValue));
	}

	@Test
	void createIntegerNull() {
		createNull("c_4");
	}

	@Test
	void createIntegerMin() {
		createIntegerError("c_4", -1);
	}

	@Test
	void createIntegerMax() {
		createIntegerError("c_4", 100);
	}

	@Test
	void createMissingProject() {
		createIntegerError("c_4", 0);
	}

	@Test
	void createBoolean() {
		final var parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_5").getId());
		parameterValue.setBool(Boolean.TRUE);
		final var entity = resource.createInternal(parameterValue);
		Assertions.assertEquals(parameterValue.getParameter(), entity.getParameter().getId());
		Assertions.assertEquals(parameterValue.getBool().toString(), entity.getData());
	}

	@Test
	void createBooleanNull() {
		createNull("c_5");
	}

	@Test
	void createDate() {
		final var parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_6").getId());
		parameterValue.setDate(new Date());
		final var entity = resource.createInternal(parameterValue);
		Assertions.assertEquals(parameterValue.getParameter(), entity.getParameter().getId());
		Assertions.assertEquals(String.valueOf(parameterValue.getDate().getTime()), entity.getData());
	}

	@Test
	void createDateInvalid() {
		final var parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_6").getId());
		final var date = new Date();
		date.setTime(0);
		parameterValue.setDate(date);
		Assertions.assertThrows(ValidationJsonException.class, () -> resource.createInternal(parameterValue));
	}

	@Test
	void createDateNull() {
		createNull("c_6");
	}

	@Test
	void createTags() {
		final var parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_22").getId());
		final List<String> tags = new ArrayList<>();
		tags.add("value_1");
		tags.add("value_X");
		parameterValue.setTags(tags);
		final var entity = resource.createInternal(parameterValue);
		Assertions.assertEquals(parameterValue.getParameter(), entity.getParameter().getId());
		Assertions.assertEquals("[\"VALUE_1\",\"VALUE_X\"]", entity.getData());
	}

	@Test
	void createTagsEmpty() {
		final var parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_22").getId());
		final List<String> tags = new ArrayList<>();
		tags.add("\t");
		parameterValue.setTags(tags);
		Assertions.assertThrows(ValidationJsonException.class, () -> resource.createInternal(parameterValue));
	}

	@Test
	void createTagsNull() {
		createNull("c_22");
	}

	@Test
	void createMultiple() {
		final var parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_23").getId());
		final List<Integer> tags = new ArrayList<>();
		tags.add(1);
		tags.add(2);
		parameterValue.setSelections(tags);
		final var entity = resource.createInternal(parameterValue);
		Assertions.assertEquals(parameterValue.getParameter(), entity.getParameter().getId());
		Assertions.assertEquals("[1,2]", entity.getData());
	}

	@Test
	void createMultipleNull() {
		createNull("c_23");
	}

	@Test
	void createMultipleInvalidIndex1() {
		final var parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_23").getId());
		final List<Integer> tags = new ArrayList<>();
		tags.add(-1);
		parameterValue.setSelections(tags);
		Assertions.assertThrows(ValidationJsonException.class, () -> resource.createInternal(parameterValue));
	}

	@Test
	void createMultipleInvalidIndex2() {
		final var parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_23").getId());
		final List<Integer> tags = new ArrayList<>();
		tags.add(8);
		parameterValue.setSelections(tags);
		Assertions.assertThrows(ValidationJsonException.class, () -> resource.createInternal(parameterValue));
	}

	@Test
	void createTooManyValues() {
		final var parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_23").getId());
		final List<Integer> tags = new ArrayList<>();
		parameterValue.setSelections(tags);
		parameterValue.setText("ignore but dirty");
		Assertions.assertThrows(ValidationJsonException.class, () -> resource.createInternal(parameterValue));
	}

	@Test
	void createSelectJSonError() {
		final var parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_22").getId());
		@SuppressWarnings("unchecked") final List<String> badList = Mockito.mock(List.class);
		Mockito.when(badList.isEmpty()).thenReturn(Boolean.FALSE);
		Mockito.when(badList.iterator()).thenThrow(new IllegalStateException());
		parameterValue.setTags(badList);
		Assertions.assertThrows(TechnicalException.class, () -> ParameterValueResource.toData(parameterValue));
	}

	@Test
	void findText() {
		final var parameter = parameterRepository.findOne("c_2");
		final var parameterValueEntity = newParameterValue("value", parameter);
		em.persist(parameterValueEntity);
		em.flush();

		final var valueVo = resource.toVo(parameterValueEntity);
		Assertions.assertEquals(parameter.getId(), valueVo.getParameter().getId());
		Assertions.assertEquals(parameterValueEntity.getData(), valueVo.getText());
		Assertions.assertNotNull(valueVo.getCreatedDate());
		Assertions.assertNotNull(valueVo.getCreatedBy());
	}

	@Test
	void findInteger() {
		final var parameter = parameterRepository.findOne("c_4");
		final var parameterValueEntity = newParameterValue("1", parameter);
		em.persist(parameterValueEntity);
		em.flush();

		final var valueVo = resource.toVo(parameterValueEntity);
		Assertions.assertEquals(parameter.getId(), valueVo.getParameter().getId());
		Assertions.assertEquals(1, valueVo.getInteger().intValue());
		Assertions.assertNotNull(valueVo.getCreatedDate());
		Assertions.assertNotNull(valueVo.getCreatedBy());
	}

	@Test
	void findIntegerNoData() {
		final var parameter = parameterRepository.findOne("c_4");
		parameter.setData(null);
		final var parameterValueEntity = newParameterValue("1", parameter);
		em.persist(parameterValueEntity);
		em.flush();

		final var valueVo = resource.toVo(parameterValueEntity);
		Assertions.assertEquals(parameter.getId(), valueVo.getParameter().getId());
		Assertions.assertEquals(1, valueVo.getInteger().intValue());
		Assertions.assertNotNull(valueVo.getCreatedDate());
		Assertions.assertNotNull(valueVo.getCreatedBy());
	}

	@Test
	void findSelect() {
		final var parameter = parameterRepository.findOne("c_3");
		final var parameterValueEntity = newParameterValue("1", parameter);
		em.persist(parameterValueEntity);
		em.flush();

		final var valueVo = resource.toVo(parameterValueEntity);
		Assertions.assertEquals(parameter.getId(), valueVo.getParameter().getId());
		Assertions.assertEquals(1, valueVo.getIndex().intValue());
		Assertions.assertNotNull(valueVo.getCreatedDate());
		Assertions.assertNotNull(valueVo.getCreatedBy());
	}

	@Test
	void findBool() {
		final var parameter = parameterRepository.findOne("c_5");
		final var parameterValueEntity = newParameterValue(Boolean.TRUE.toString(), parameter);
		em.persist(parameterValueEntity);
		em.flush();
		final var valueVo = resource.toVo(parameterValueEntity);
		Assertions.assertEquals(parameter.getId(), valueVo.getParameter().getId());
		Assertions.assertTrue(valueVo.getBool());
		Assertions.assertNotNull(valueVo.getCreatedDate());
		Assertions.assertNotNull(valueVo.getCreatedBy());
	}

	@Test
	void findDate() {
		final var parameter = parameterRepository.findOne("c_6");
		final var parameterValueEntity = newParameterValue(String.valueOf(new Date().getTime()), parameter);
		em.persist(parameterValueEntity);
		em.flush();

		final var valueVo = resource.toVo(parameterValueEntity);
		Assertions.assertEquals(parameter.getId(), valueVo.getParameter().getId());
		Assertions.assertTrue(valueVo.getParameter().isMandatory());
		Assertions.assertEquals(parameterValueEntity.getData(), String.valueOf(valueVo.getDate().getTime()));
		Assertions.assertNotNull(valueVo.getCreatedDate());
		Assertions.assertNotNull(valueVo.getCreatedBy());
	}

	@Test
	void findMultiple() {
		final var parameter = parameterRepository.findOne("c_23");
		final var parameterValueEntity = newParameterValue("[0,2]", parameter);
		em.persist(parameterValueEntity);
		em.flush();

		final var valueVo = resource.toVo(parameterValueEntity);
		Assertions.assertEquals(parameter.getId(), valueVo.getParameter().getId());
		Assertions.assertEquals("[0, 2]", valueVo.getSelections().toString());
		Assertions.assertNotNull(valueVo.getCreatedDate());
		Assertions.assertNotNull(valueVo.getCreatedBy());
	}

	@Test
	void findTags() {
		final var parameter = parameterRepository.findOne("c_22");
		final var parameterValueEntity = newParameterValue("[\"A\",\"B\"]", parameter);
		em.persist(parameterValueEntity);
		em.flush();

		final var valueVo = resource.toVo(parameterValueEntity);
		Assertions.assertEquals(parameter.getId(), valueVo.getParameter().getId());
		Assertions.assertEquals("[A, B]", valueVo.getTags().toString());
		Assertions.assertNotNull(valueVo.getCreatedDate());
		Assertions.assertNotNull(valueVo.getCreatedBy());
	}

	@Test
	void findWithNode() {
		final var parameter = parameterRepository.findOne("c_20");
		final var parameterValueEntity = newParameterValue("true", parameter);
		parameterValueEntity.setNode(em.find(Node.class, "service:bt:jira:6"));
		em.persist(parameterValueEntity);
		em.flush();

		final var valueVo = resource.toVo(parameterValueEntity);
		Assertions.assertEquals(parameter.getId(), valueVo.getParameter().getId());
		Assertions.assertEquals("service:bt:jira:6", valueVo.getNode().getId());
	}

	@Test
	void findInvalidJSonData() {
		final var parameter = parameterRepository.findOne("c_22");
		final var parameterValueEntity = newParameterValue("'", parameter);
		Assertions.assertThrows(TechnicalException.class, () -> resource.toVo(parameterValueEntity));
	}

	@Test
	void findSelectJSonError() {
		final var parameter = parameterRepository.findOne("c_4");
		parameter.setData("'{");
		final var parameterValueEntity = newParameterValue("'", parameter);
		Assertions.assertThrows(TechnicalException.class, () -> resource.toVo(parameterValueEntity));
	}

	private SystemRole newUser(final String name, final int id) {
		final var user = new SystemRole();
		user.setName(name);
		user.setId(id);
		return user;
	}

	private ParameterValue newParameterValue(final String data, final Parameter parameter) {
		final var parameterValueEntity = new ParameterValue();
		parameterValueEntity.setParameter(parameter);
		parameterValueEntity.setData(data);
		return parameterValueEntity;
	}

	/**
	 * Create a new {@link ParameterValue} linked to a new {@link Node} without subscription.
	 */
	private ParameterValue newParameterValue() {
		final var node = new Node();
		node.setName("create-test");
		node.setId("service:kpi:sonar:temp");
		node.setRefined(em.find(Node.class, "service:kpi:sonar"));
		em.persist(node);
		final var value = new ParameterValue();
		value.setParameter(em.find(Parameter.class, "service:kpi:sonar:user"));
		value.setData("user");
		value.setNode(node);
		em.persist(value);
		em.flush();
		em.clear();
		return value;
	}

	@Test
	void toMap() {
		final List<SystemRole> values = new ArrayList<>();
		values.add(newUser("u1", 1));
		values.add(newUser("u2", 2));
		final Map<Integer, SystemRole> valuesAsMap = NodeHelper.toMap(values);
		Assertions.assertEquals(2, valuesAsMap.size());
		Assertions.assertEquals("u1", valuesAsMap.get(1).getName());
		Assertions.assertEquals("u2", valuesAsMap.get(2).getName());
	}

	@Test
	void toMapValues() {
		final List<ParameterValue> values = new ArrayList<>();
		final var p1 = new Parameter();
		p1.setId("p1");
		final var p2 = new Parameter();
		p2.setId("p2");
		values.add(newParameterValue("u1", p1));
		values.add(newParameterValue("u2", p2));
		final var valuesAsMap = resource.toMapValues(values);
		Assertions.assertEquals(2, valuesAsMap.size());
		Assertions.assertEquals("u1", valuesAsMap.get("p1"));
		Assertions.assertEquals("u2", valuesAsMap.get("p2"));
	}

	@Test
	void getNonSecuredParameters() {
		final var parameters = resource.getNonSecuredSubscriptionParameters(getSubscription("MDA"));
		Assertions.assertNull(parameters.get("service:bt:jira:jdbc-user"));
		Assertions.assertNull(parameters.get("service:bt:jira:jdbc-password"));
		Assertions.assertNull(parameters.get("service:bt:jira:jdbc-url"));
		Assertions.assertNull(parameters.get("service:bt:jira:jdbc-driver"));
		Assertions.assertNull(parameters.get("service:bt:jira:user"));
		Assertions.assertNull(parameters.get("service:bt:jira:password"));
		Assertions.assertEquals("10074", parameters.get("service:bt:jira:project"));
		Assertions.assertEquals("MDA", parameters.get("service:bt:jira:pkey"));
		Assertions.assertEquals("http://localhost:8120", parameters.get("service:bt:jira:url"));
	}

	@Test
	void getParametersWithFilteredNull() {
		final var parameters = resource.getSubscriptionParameters(getSubscription("MDA"));
		// User and password are empty, so not returned
		Assertions.assertEquals(5, parameters.size());
		Assertions.assertEquals("jdbc:hsqldb:mem:dataSource", parameters.get("service:bt:jira:jdbc-url"));
		Assertions.assertEquals("org.hsqldb.jdbc.JDBCDriver", parameters.get("service:bt:jira:jdbc-driver"));
		Assertions.assertEquals("10074", parameters.get("service:bt:jira:project"));
		Assertions.assertEquals("MDA", parameters.get("service:bt:jira:pkey"));
		Assertions.assertEquals("http://localhost:8120", parameters.get("service:bt:jira:url"));
	}

	@Test
	void getParameters() {
		final var parameters = resource.getSubscriptionParameters(getSubscription("Jupiter", BuildResource.SERVICE_KEY));
		Assertions.assertEquals(4, parameters.size());
		Assertions.assertEquals("junit", parameters.get("service:build:jenkins:user"));
		Assertions.assertEquals("http://localhost:8120", parameters.get("service:build:jenkins:url"));
		Assertions.assertEquals("secret", parameters.get("service:build:jenkins:api-token"));
		Assertions.assertEquals("ligoj-bootstrap", parameters.get("service:build:jenkins:job"));
	}

	@Test
	void deleteBySubscription() {
		final var subscription = getSubscription("MDA");

		// There are 5 parameters from the node, and 2 from the subscription
		Assertions.assertEquals(7, repository.findAllBySubscription(subscription).size());
		resource.deleteBySubscription(subscription);
		em.flush();
		em.clear();

		// Check there are only 5 parameters, and only from the node
		final var parameters = repository.findAllBySubscription(subscription);
		Assertions.assertEquals(5, parameters.size());
		Assertions.assertEquals(5,
				parameters.stream().map(ParameterValue::getSubscription).filter(Objects::isNull).count());
	}

	@Test
	void createInternal() {
		em.createQuery("DELETE Parameter WHERE id LIKE ?1").setParameter(1, "c_%").executeUpdate();

		final List<ParameterValueCreateVo> parameters = new ArrayList<>();
		final var parameterValueEditionVo = new ParameterValueCreateVo();
		parameterValueEditionVo.setParameter(JiraBaseResource.PARAMETER_PROJECT);
		parameterValueEditionVo.setInteger(10074);
		parameters.add(parameterValueEditionVo);
		final var node = new Node();
		node.setName("create-test");
		node.setId("create-test-id");
		em.persist(node);
		em.flush();
		em.clear();
		resource.create(parameters, node);

		em.flush();
		em.clear();

		final var values = repository.findAllBy("node.id", "create-test-id");
		Assertions.assertEquals(1, values.size());
		Assertions.assertEquals("10074", values.getFirst().getData());
		Assertions.assertEquals(JiraBaseResource.PARAMETER_PROJECT, values.getFirst().getParameter().getId());
	}

	@Test
	void deleteNotExist() {
		Assertions.assertThrows(EntityNotFoundException.class, () -> resource.delete(-1));
	}

	@Test
	void deleteNotVisible() {
		initSpringSecurityContext("any");
		final var id = repository.findBy("parameter.id", "service:kpi:sonar:user").getId();
		Assertions.assertThrows(EntityNotFoundException.class, () -> resource.delete(id));
	}

	@Test
	void deleteNotExists() {
		final var id = repository.findBy("parameter.id", "service:kpi:sonar:project").getId();
		Assertions.assertThrows(EntityNotFoundException.class, () -> resource.delete(id));
	}

	@Test
	void deleteMandatoryUsed() {
		final var id = repository.findBy("parameter.id", "service:kpi:sonar:user").getId();
		Assertions.assertThrows(ValidationJsonException.class, () -> resource.delete(id));
	}

	@Test
	void deleteMandatoryUnused() {
		final var value = newParameterValue();
		Assertions.assertTrue(repository.existsById(value.getId()));
		resource.delete(value.getId());
		Assertions.assertFalse(repository.existsById(value.getId()));
	}

	@Test
	void deleteNotMandatory() {
		final var id = repository.findBy("parameter.id", "service:id:ldap:quarantine-dn").getId();
		Assertions.assertTrue(repository.existsById(id));
		resource.delete(id);
		Assertions.assertFalse(repository.existsById(id));
	}

	/**
	 * Return the subscription identifier of MDA. Assumes there is only one subscription for a service.
	 */
	private int getSubscription(final String project) {
		return getSubscription(project, BugTrackerResource.SERVICE_KEY);
	}

	@Test
	void checkOwnershipDisjunction() {
		final var node = new Node();
		node.setId("service:id");
		final var parameter = new Parameter();
		parameter.setOwner(node);
		final var node2 = new Node();
		node2.setId("service:other");
		final var node3 = new Node();
		node3.setId("service:other:sub");
		node3.setRefined(node2);
		Assertions.assertThrows(BusinessException.class, () -> resource.checkOwnership(parameter, node3));
	}

	@Test
	void checkOwnership() {
		final var node = new Node();
		node.setId("service:id");
		final var parameter = new Parameter();
		parameter.setOwner(node);
		final var node2 = new Node();
		node2.setId("service:id:sub");
		node2.setRefined(node);
		resource.checkOwnership(parameter, node2);
	}

	@Test
	void updateListUntouchedNotExist() {
		final var parameterValue = new ParameterValueCreateVo();
		parameterValue.setUntouched(true);
		parameterValue.setParameter("any");
		final List<ParameterValueCreateVo> values = Collections.singletonList(parameterValue);
		final var node = new Node();
		node.setId("service:id:ldap");
		Assertions.assertThrows(BusinessException.class, () -> resource.update(values, node));
	}

	@Test
	void updateListUntouchedExists() {
		final var parameterValue = new ParameterValueCreateVo();
		parameterValue.setUntouched(true);
		parameterValue.setParameter("service:id:ldap:quarantine-dn");
		final List<ParameterValueCreateVo> values = Collections.singletonList(parameterValue);
		final var node = new Node();
		node.setId("service:id:ldap:dig");
		resource.update(values, node);
	}

	@Test
	void findAll() {
		final int projectId = projectRepository.findByName("MDA").getId();
		final List<ParameterValueVo> parameterValues = new ArrayList<>(
				resource.findAll(projectId, "service:bt:jira:pkey", "service:bt:jira:4", "MD"));
		Assertions.assertEquals(1, parameterValues.size());

		final var pValue = parameterValues.getFirst();
		Assertions.assertEquals("MDA", pValue.getText());

	}
}
