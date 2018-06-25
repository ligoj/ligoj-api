/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ligoj.app.AbstractAppTest;
import org.ligoj.app.dao.ParameterRepository;
import org.ligoj.app.dao.ParameterValueRepository;
import org.ligoj.app.dao.ProjectRepository;
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
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * {@link ParameterValueResource} test cases.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
public class ParameterValueResourceTest extends AbstractAppTest {

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
	public void prepare() throws IOException {
		persistEntities("csv",
				new Class[] { Node.class, Parameter.class, Project.class, Subscription.class, ParameterValue.class },
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

		Assertions.assertEquals(parameterValue.getParameter(), entity.getParameter().getId());
		Assertions.assertEquals("value", entity.getData());
	}

	@Test
	public void createSecured() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("service:bt:jira:jdbc-url").getId());
		parameterValue.setText("value");
		final ParameterValue entity = resource.createInternal(parameterValue);
		Assertions.assertTrue(entity.toString()
				.startsWith("ParameterValue(parameter=AbstractBusinessEntity(id=service:bt:jira:jdbc-url), data="));
		Assertions.assertEquals(parameterValue.getParameter(), entity.getParameter().getId());
		Assertions.assertNotEquals("value", entity.getData());
		Assertions.assertEquals("value", encryptor.decrypt(entity.getData()));
	}

	@Test
	public void createAlreadySecured() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("service:bt:jira:jdbc-url").getId());
		parameterValue.setText(encryptor.encrypt("value"));
		final ParameterValue entity = resource.createInternal(parameterValue);

		Assertions.assertEquals(parameterValue.getParameter(), entity.getParameter().getId());
		Assertions.assertNotEquals("value", entity.getData());
		Assertions.assertEquals(parameterValue.getText(), entity.getData());
		Assertions.assertEquals("value", encryptor.decrypt(entity.getData()));
	}

	@Test
	public void createTextPattern() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_17").getId());
		parameterValue.setText("va-l-u-9e");
		final ParameterValue entity = resource.createInternal(parameterValue);

		Assertions.assertEquals(parameterValue.getParameter(), entity.getParameter().getId());
		Assertions.assertEquals("va-l-u-9e", entity.getData());
	}

	@Test
	public void createTextEmptyPattern() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_170").getId());
		parameterValue.setText("va-l-u-9e");
		final ParameterValue entity = resource.createInternal(parameterValue);

		Assertions.assertEquals(parameterValue.getParameter(), entity.getParameter().getId());
		Assertions.assertEquals("va-l-u-9e", entity.getData());
	}

	@Test
	public void createTextPatternFailed() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_17").getId());
		parameterValue.setText("1a");
		Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.createInternal(parameterValue);
		});
	}

	@Test
	public void createTextEmpty() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_17").getId());
		Assertions.assertNull(resource.createInternal(parameterValue));
	}

	@Test
	public void createIndex() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_3").getId());
		parameterValue.setIndex(1);
		final ParameterValue entity = resource.createInternal(parameterValue);
		Assertions.assertEquals(parameterValue.getParameter(), entity.getParameter().getId());
		Assertions.assertEquals("1", entity.getData());
	}

	@Test
	public void createIndexNull() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_3").getId());
		Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.createInternal(parameterValue);
		});
	}

	@Test
	public void createIndexMin() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_3").getId());
		parameterValue.setIndex(-1);
		Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.createInternal(parameterValue);
		});
	}

	@Test
	public void createIndexMax() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_3").getId());
		parameterValue.setIndex(3);
		Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.createInternal(parameterValue);
		});
	}

	@Test
	public void createInteger() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_4").getId());
		parameterValue.setInteger(1);
		final ParameterValue entity = resource.createInternal(parameterValue);
		Assertions.assertEquals(parameterValue.getParameter(), entity.getParameter().getId());
		Assertions.assertEquals("1", entity.getData());
	}

	@Test
	public void createIntegerNoConstraint() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_19").getId());
		parameterValue.setInteger(-100);
		final ParameterValue entity = resource.createInternal(parameterValue);
		Assertions.assertEquals(parameterValue.getParameter(), entity.getParameter().getId());
		Assertions.assertEquals("-100", entity.getData());
	}

	@Test
	public void createIntegerNull() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_4").getId());
		Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.createInternal(parameterValue);
		});
	}

	@Test
	public void createIntegerMin() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_4").getId());
		parameterValue.setInteger(-1);
		Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.createInternal(parameterValue);
		});
	}

	@Test
	public void createIntegerMax() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_4").getId());
		parameterValue.setInteger(100);
		Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.createInternal(parameterValue);
		});
	}

	@Test
	public void createMissingProject() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_4").getId());
		parameterValue.setInteger(0);
		Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.createInternal(parameterValue);
		});
	}

	@Test
	public void createBoolean() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_5").getId());
		parameterValue.setBool(Boolean.TRUE);
		final ParameterValue entity = resource.createInternal(parameterValue);
		Assertions.assertEquals(parameterValue.getParameter(), entity.getParameter().getId());
		Assertions.assertEquals(parameterValue.getBool().toString(), entity.getData());
	}

	@Test
	public void createBooleanNull() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_5").getId());
		Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.createInternal(parameterValue);
		});
	}

	@Test
	public void createDate() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_6").getId());
		parameterValue.setDate(new Date());
		final ParameterValue entity = resource.createInternal(parameterValue);
		Assertions.assertEquals(parameterValue.getParameter(), entity.getParameter().getId());
		Assertions.assertEquals(String.valueOf(parameterValue.getDate().getTime()), entity.getData());
	}

	@Test
	public void createDateInvalid() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_6").getId());
		final Date date = new Date();
		date.setTime(0);
		parameterValue.setDate(date);
		Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.createInternal(parameterValue);
		});
	}

	@Test
	public void createDateNull() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_6").getId());
		Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.createInternal(parameterValue);
		});
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
		Assertions.assertEquals(parameterValue.getParameter(), entity.getParameter().getId());
		Assertions.assertEquals("[\"VALUE1\",\"VALUEX\"]", entity.getData());
	}

	@Test
	public void createTagsEmpty() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_22").getId());
		final List<String> tags = new ArrayList<>();
		tags.add("\t");
		parameterValue.setTags(tags);
		Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.createInternal(parameterValue);
		});
	}

	@Test
	public void createTagsNull() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_22").getId());
		Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.createInternal(parameterValue);
		});
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
		Assertions.assertEquals(parameterValue.getParameter(), entity.getParameter().getId());
		Assertions.assertEquals("[1,2]", entity.getData());
	}

	@Test
	public void createMultipleNull() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_23").getId());
		Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.createInternal(parameterValue);
		});
	}

	@Test
	public void createMultipleInvalidIndex1() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_23").getId());
		final List<Integer> tags = new ArrayList<>();
		tags.add(-1);
		parameterValue.setSelections(tags);
		Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.createInternal(parameterValue);
		});
	}

	@Test
	public void createMultipleInvalidIndex2() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_23").getId());
		final List<Integer> tags = new ArrayList<>();
		tags.add(8);
		parameterValue.setSelections(tags);
		Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.createInternal(parameterValue);
		});
	}

	@Test
	public void createTooManyValues() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_23").getId());
		final List<Integer> tags = new ArrayList<>();
		parameterValue.setSelections(tags);
		parameterValue.setText("ignore but dirty");
		Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.createInternal(parameterValue);
		});
	}

	@Test
	public void createSelectJSonError() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setParameter(parameterRepository.findOne("c_22").getId());
		@SuppressWarnings("unchecked")
		final List<String> badList = Mockito.mock(List.class);
		Mockito.when(badList.isEmpty()).thenReturn(Boolean.FALSE);
		Mockito.when(badList.iterator()).thenThrow(new IllegalStateException());
		parameterValue.setTags(badList);
		Assertions.assertThrows(TechnicalException.class, () -> {
			ParameterValueResource.toData(parameterValue);
		});
	}

	@Test
	public void findText() {
		final Parameter parameter = parameterRepository.findOne("c_2");
		final ParameterValue parameterValueEntity = newParameterValue("value", parameter);
		em.persist(parameterValueEntity);
		em.flush();

		final ParameterValueVo valueVo = resource.toVo(parameterValueEntity);
		Assertions.assertEquals(parameter.getId(), valueVo.getParameter().getId());
		Assertions.assertEquals(parameterValueEntity.getData(), valueVo.getText());
		Assertions.assertNotNull(valueVo.getCreatedDate());
		Assertions.assertNotNull(valueVo.getCreatedBy());
	}

	@Test
	public void findInteger() {
		final Parameter parameter = parameterRepository.findOne("c_4");
		final ParameterValue parameterValueEntity = newParameterValue("1", parameter);
		em.persist(parameterValueEntity);
		em.flush();

		final ParameterValueVo valueVo = resource.toVo(parameterValueEntity);
		Assertions.assertEquals(parameter.getId(), valueVo.getParameter().getId());
		Assertions.assertEquals(1, valueVo.getInteger().intValue());
		Assertions.assertNotNull(valueVo.getCreatedDate());
		Assertions.assertNotNull(valueVo.getCreatedBy());
	}

	@Test
	public void findIntegerNoData() {
		final Parameter parameter = parameterRepository.findOne("c_4");
		parameter.setData(null);
		final ParameterValue parameterValueEntity = newParameterValue("1", parameter);
		em.persist(parameterValueEntity);
		em.flush();

		final ParameterValueVo valueVo = resource.toVo(parameterValueEntity);
		Assertions.assertEquals(parameter.getId(), valueVo.getParameter().getId());
		Assertions.assertEquals(1, valueVo.getInteger().intValue());
		Assertions.assertNotNull(valueVo.getCreatedDate());
		Assertions.assertNotNull(valueVo.getCreatedBy());
	}

	@Test
	public void findSelect() {
		final Parameter parameter = parameterRepository.findOne("c_3");
		final ParameterValue parameterValueEntity = newParameterValue("1", parameter);
		em.persist(parameterValueEntity);
		em.flush();

		final ParameterValueVo valueVo = resource.toVo(parameterValueEntity);
		Assertions.assertEquals(parameter.getId(), valueVo.getParameter().getId());
		Assertions.assertEquals(1, valueVo.getIndex().intValue());
		Assertions.assertNotNull(valueVo.getCreatedDate());
		Assertions.assertNotNull(valueVo.getCreatedBy());
	}

	@Test
	public void findBool() {
		final Parameter parameter = parameterRepository.findOne("c_5");
		final ParameterValue parameterValueEntity = newParameterValue(Boolean.TRUE.toString(), parameter);
		em.persist(parameterValueEntity);
		em.flush();
		final ParameterValueVo valueVo = resource.toVo(parameterValueEntity);
		Assertions.assertEquals(parameter.getId(), valueVo.getParameter().getId());
		Assertions.assertTrue(valueVo.getBool());
		Assertions.assertNotNull(valueVo.getCreatedDate());
		Assertions.assertNotNull(valueVo.getCreatedBy());
	}

	@Test
	public void findDate() {
		final Parameter parameter = parameterRepository.findOne("c_6");
		final ParameterValue parameterValueEntity = newParameterValue(String.valueOf(new Date().getTime()), parameter);
		em.persist(parameterValueEntity);
		em.flush();

		final ParameterValueVo valueVo = resource.toVo(parameterValueEntity);
		Assertions.assertEquals(parameter.getId(), valueVo.getParameter().getId());
		Assertions.assertTrue(valueVo.getParameter().isMandatory());
		Assertions.assertEquals(parameterValueEntity.getData(), String.valueOf(valueVo.getDate().getTime()));
		Assertions.assertNotNull(valueVo.getCreatedDate());
		Assertions.assertNotNull(valueVo.getCreatedBy());
	}

	@Test
	public void findMultiple() {
		final Parameter parameter = parameterRepository.findOne("c_23");
		final ParameterValue parameterValueEntity = newParameterValue("[0,2]", parameter);
		em.persist(parameterValueEntity);
		em.flush();

		final ParameterValueVo valueVo = resource.toVo(parameterValueEntity);
		Assertions.assertEquals(parameter.getId(), valueVo.getParameter().getId());
		Assertions.assertEquals("[0, 2]", valueVo.getSelections().toString());
		Assertions.assertNotNull(valueVo.getCreatedDate());
		Assertions.assertNotNull(valueVo.getCreatedBy());
	}

	@Test
	public void findTags() {
		final Parameter parameter = parameterRepository.findOne("c_22");
		final ParameterValue parameterValueEntity = newParameterValue("[\"A\",\"B\"]", parameter);
		em.persist(parameterValueEntity);
		em.flush();

		final ParameterValueVo valueVo = resource.toVo(parameterValueEntity);
		Assertions.assertEquals(parameter.getId(), valueVo.getParameter().getId());
		Assertions.assertEquals("[A, B]", valueVo.getTags().toString());
		Assertions.assertNotNull(valueVo.getCreatedDate());
		Assertions.assertNotNull(valueVo.getCreatedBy());
	}

	@Test
	public void findWithNode() {
		final Parameter parameter = parameterRepository.findOne("c_20");
		final ParameterValue parameterValueEntity = newParameterValue("true", parameter);
		parameterValueEntity.setNode(em.find(Node.class, "service:bt:jira:6"));
		em.persist(parameterValueEntity);
		em.flush();

		final ParameterValueVo valueVo = resource.toVo(parameterValueEntity);
		Assertions.assertEquals(parameter.getId(), valueVo.getParameter().getId());
		Assertions.assertEquals("service:bt:jira:6", valueVo.getNode().getId());
	}

	@Test
	public void findInvalidJSonData() {
		final Parameter parameter = parameterRepository.findOne("c_22");
		final ParameterValue parameterValueEntity = newParameterValue("'", parameter);
		Assertions.assertThrows(TechnicalException.class, () -> {
			resource.toVo(parameterValueEntity);
		});
	}

	@Test
	public void findSelectJSonError() {
		final Parameter parameter = parameterRepository.findOne("c_4");
		parameter.setData("'{");
		final ParameterValue parameterValueEntity = newParameterValue("'", parameter);
		Assertions.assertThrows(TechnicalException.class, () -> {
			resource.toVo(parameterValueEntity);
		});
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
		Assertions.assertEquals(2, valuesAsMap.size());
		Assertions.assertEquals("u1", valuesAsMap.get(1).getName());
		Assertions.assertEquals("u2", valuesAsMap.get(2).getName());
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
		Assertions.assertEquals(2, valuesAsMap.size());
		Assertions.assertEquals("u1", valuesAsMap.get("p1"));
		Assertions.assertEquals("u2", valuesAsMap.get("p2"));
	}

	@Test
	public void getNonSecuredParameters() {
		final Map<String, String> parameters = resource.getNonSecuredSubscriptionParameters(getSubscription("MDA"));
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
	public void getParametersWithFilteredNull() {
		final Map<String, String> parameters = resource.getSubscriptionParameters(getSubscription("MDA"));
		// User and password are empty, so not returned
		Assertions.assertEquals(5, parameters.size());
		Assertions.assertEquals("jdbc:hsqldb:mem:dataSource", parameters.get("service:bt:jira:jdbc-url"));
		Assertions.assertEquals("org.hsqldb.jdbc.JDBCDriver", parameters.get("service:bt:jira:jdbc-driver"));
		Assertions.assertEquals("10074", parameters.get("service:bt:jira:project"));
		Assertions.assertEquals("MDA", parameters.get("service:bt:jira:pkey"));
		Assertions.assertEquals("http://localhost:8120", parameters.get("service:bt:jira:url"));
	}

	@Test
	public void getParameters() {
		final Map<String, String> parameters = resource
				.getSubscriptionParameters(getSubscription("gStack", BuildResource.SERVICE_KEY));
		Assertions.assertEquals(4, parameters.size());
		Assertions.assertEquals("junit", parameters.get("service:build:jenkins:user"));
		Assertions.assertEquals("http://localhost:8120", parameters.get("service:build:jenkins:url"));
		Assertions.assertEquals("secret", parameters.get("service:build:jenkins:api-token"));
		Assertions.assertEquals("gfi-bootstrap", parameters.get("service:build:jenkins:job"));
	}

	@Test
	public void deleteBySubscription() {
		final int subscription = getSubscription("MDA");

		// There are 5 parameters from the node, and 2 from the subscription
		Assertions.assertEquals(7, repository.findAllBySubscription(subscription).size());
		resource.deleteBySubscription(subscription);
		em.flush();
		em.clear();

		// Check there are only 5 parameters, and only from the node
		final List<ParameterValue> parameters = repository.findAllBySubscription(subscription);
		Assertions.assertEquals(5, parameters.size());
		Assertions.assertEquals(5,
				parameters.stream().map(ParameterValue::getSubscription).filter(Objects::isNull).count());
	}

	@Test
	public void createInternal() {
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
		Assertions.assertEquals(1, values.size());
		Assertions.assertEquals("10074", values.get(0).getData());
		Assertions.assertEquals(JiraBaseResource.PARAMETER_PROJECT, values.get(0).getParameter().getId());
	}

	@Test
	public void deleteNotExist() {
		Assertions.assertThrows(EntityNotFoundException.class, () -> {
			resource.delete(-1);
		});
	}

	@Test
	public void deleteNotVisible() {
		initSpringSecurityContext("any");
		Assertions.assertThrows(EntityNotFoundException.class, () -> {
			resource.delete(repository.findBy("parameter.id", "service:kpi:sonar:user").getId());
		});
	}

	@Test
	public void deleteNotExists() {
		final Integer id = repository.findBy("parameter.id", "service:kpi:sonar:project").getId();
		Assertions.assertThrows(EntityNotFoundException.class, () -> {
			resource.delete(id);
		});
	}

	@Test
	public void deleteMandatoryUsed() {
		Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.delete(repository.findBy("parameter.id", "service:kpi:sonar:user").getId());
		});
	}

	@Test
	public void deleteMandatoryUnused() {
		final ParameterValue value = newParameterValue();
		Assertions.assertTrue(repository.existsById(value.getId()));
		resource.delete(value.getId());
		Assertions.assertFalse(repository.existsById(value.getId()));
	}

	/**
	 * Create a new {@link ParameterValue} linked to a new {@link Node} without subscription.
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
	public void deleteNotMandatory() {
		final Integer id = repository.findBy("parameter.id", "service:id:ldap:quarantine-dn").getId();
		Assertions.assertTrue(repository.existsById(id));
		resource.delete(id);
		Assertions.assertFalse(repository.existsById(id));
	}

	/**
	 * Return the subscription identifier of MDA. Assumes there is only one subscription for a service.
	 */
	protected int getSubscription(final String project) {
		return getSubscription(project, BugTrackerResource.SERVICE_KEY);
	}

	@Test
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
		Assertions.assertThrows(BusinessException.class, () -> {
			resource.checkOwnership(parameter, node3);
		});
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

	@Test
	public void updateListUntouchedNotExist() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setUntouched(true);
		parameterValue.setParameter("any");
		final List<ParameterValueCreateVo> values = Collections.singletonList(parameterValue);
		final Node node = new Node();
		node.setId("service:id:ldap");
		Assertions.assertThrows(BusinessException.class, () -> {
			resource.update(values, node);
		});
	}

	@Test
	public void updateListUntouchedExists() {
		final ParameterValueCreateVo parameterValue = new ParameterValueCreateVo();
		parameterValue.setUntouched(true);
		parameterValue.setParameter("service:id:ldap:quarantine-dn");
		final List<ParameterValueCreateVo> values = Collections.singletonList(parameterValue);
		final Node node = new Node();
		node.setId("service:id:ldap:dig");
		resource.update(values, node);
	}

	@Test
	public void findAll() {
		final int projectId = projectRepository.findByName("MDA").getId();
		final List<ParameterValueVo> parameterValues = new ArrayList<>(
				resource.findAll(projectId, "service:bt:jira:pkey", "service:bt:jira:4", "MD"));
		Assertions.assertEquals(1, parameterValues.size());

		final ParameterValueVo pValue = parameterValues.get(0);
		Assertions.assertEquals("MDA", pValue.getText());

	}
}
