/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.node;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.UriInfo;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ligoj.app.dao.DelegateNodeRepository;
import org.ligoj.app.iam.model.ReceiverType;
import org.ligoj.app.model.*;
import org.ligoj.bootstrap.AbstractJpaTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class of {@link DelegateNodeResource}
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
class DelegateNodeResourceTest extends AbstractJpaTest {

	@Autowired
	private DelegateNodeRepository repository;

	@Autowired
	private DelegateNodeResource resource;

	@BeforeEach
	void prepare() throws IOException {
		persistEntities("csv", new Class<?>[] { Node.class, Parameter.class, Project.class, Subscription.class,
				ParameterValue.class, DelegateNode.class }, StandardCharsets.UTF_8);
	}

	private void createNotFound(final String user, final String node) {
		initSpringSecurityContext(user);
		final var delegate = new DelegateNode();
		delegate.setNode(node);
		delegate.setReceiver("user1");
		Assertions.assertThrows(NotFoundException.class, () -> resource.create(delegate));
	}

	@Test
	void createNotExistsUser() {
		createNotFound("any", "service");
	}

	@Test
	void createNoRightAtThisLevel() {
		createNotFound("user1", "service:build");
	}

	@Test
	void createNoRightAtThisLevel2() {
		createNotFound("user1", "");
	}

	@Test
	void createExactNode() {
		initSpringSecurityContext("user1");
		final var delegate = new DelegateNode();
		delegate.setNode("service:build:jenkins");
		delegate.setReceiver("user1");
		Assertions.assertTrue(resource.create(delegate) > 0);
	}

	@Test
	void createSubNode() {
		initSpringSecurityContext("user1");
		final var delegate = new DelegateNode();
		delegate.setNode("service:build:jenkins:dig");
		delegate.setReceiver("user1");
		Assertions.assertTrue(resource.create(delegate) > 0);
	}

	@Test
	void createSubNodeMaxiRight() {
		initSpringSecurityContext("user1");
		final var delegate = new DelegateNode();
		delegate.setNode("service:build:jenkins:dig");
		delegate.setCanAdmin(true);
		delegate.setCanWrite(true);
		delegate.setCanSubscribe(true);
		delegate.setReceiver("user1");
		Assertions.assertTrue(resource.create(delegate) > 0);
	}

	@Test
	void createWriteNotAdmin() {

		// Add a special right on for a node
		final var delegate = new DelegateNode();
		delegate.setNode("service:build:jenkins");
		delegate.setReceiver("user2");
		delegate.setCanWrite(true);
		repository.saveAndFlush(delegate);

		initSpringSecurityContext("user2");
		final var newDelegate = new DelegateNode();
		newDelegate.setNode("service:build:jenkins:dig");
		newDelegate.setReceiver("user2");
		Assertions.assertThrows(NotFoundException.class, () -> resource.create(newDelegate));
	}

	@Test
	void createGrantRefused() {

		// Add a special right on for a node
		final var delegate = new DelegateNode();
		delegate.setNode("service:build:jenkins");
		delegate.setReceiver("user2");
		delegate.setCanAdmin(true);
		repository.saveAndFlush(delegate);

		initSpringSecurityContext("user2");
		final var newDelegate = new DelegateNode();
		newDelegate.setNode("service:build:jenkins:dig");
		newDelegate.setReceiver("user2");
		newDelegate.setCanWrite(true);
		Assertions.assertThrows(jakarta.ws.rs.NotFoundException.class, () -> resource.create(newDelegate));
	}

	@Test
	void createSubNodeMiniRight() {

		// Add a special right on for a node
		final var delegate = new DelegateNode();
		delegate.setNode("service:build:jenkins");
		delegate.setReceiver("user2");
		delegate.setCanAdmin(true);
		repository.saveAndFlush(delegate);

		initSpringSecurityContext("user2");
		final var newDelegate = new DelegateNode();
		newDelegate.setNode("service:build:jenkins:dig");
		newDelegate.setReceiver("user2");
		newDelegate.setCanAdmin(true);
		Assertions.assertTrue(resource.create(newDelegate) > 0);
	}

	@Test
	void updateNoChange() {

		// Add a special right on for a node
		final var delegate = new DelegateNode();
		delegate.setNode("service:build:jenkins");
		delegate.setReceiver("user2");
		delegate.setCanAdmin(true);
		repository.saveAndFlush(delegate);

		initSpringSecurityContext("user2");
		final var newDelegate = new DelegateNode();
		newDelegate.setNode("service:build:jenkins");
		newDelegate.setReceiver("user2");
		newDelegate.setCanAdmin(true);
		resource.update(newDelegate);
	}

	@Test
	void updateSubNodeReduceRight() {

		// Add a special right on for a node
		final var delegate = new DelegateNode();
		delegate.setNode("service:build:jenkins");
		delegate.setReceiver("user2");
		delegate.setCanAdmin(true);
		repository.saveAndFlush(delegate);

		initSpringSecurityContext("user2");
		final var newDelegate = new DelegateNode();
		newDelegate.setNode("service:build:jenkins");
		newDelegate.setReceiver("user2");
		resource.update(newDelegate);
	}

	@Test
	void deleteSubNode() {
		final int user1Delegate = repository.findBy("receiver", "user1").getId();
		resource.delete(user1Delegate);
		Assertions.assertFalse(repository.existsById(user1Delegate));
	}

	@Test
	void deleteSameLevel() {
		final int user1Delegate = repository.findBy("receiver", "fdaugan").getId();
		resource.delete(user1Delegate);
		Assertions.assertFalse(repository.existsById(user1Delegate));
	}

	@Test
	void deleteNotRight() {
		final int user1Delegate = repository.findBy("receiver", DEFAULT_USER).getId();

		initSpringSecurityContext("user1");
		Assertions.assertThrows(NotFoundException.class, () -> resource.delete(user1Delegate));
	}

	@Test
	void createIgnoresIdentifier() {
		// A creation never overwrites an existing delegate, here the one of another user on the root node
		final var other = repository.findBy("receiver", DEFAULT_USER);
		initSpringSecurityContext("user1");
		final var delegate = new DelegateNode();
		delegate.setId(other.getId());
		delegate.setNode("service:build:jenkins:dig");
		delegate.setReceiver("user1");
		Assertions.assertNotEquals(other.getId(), resource.create(delegate));
		em.flush();
		em.clear();
		Assertions.assertEquals("service", repository.findOneExpected(other.getId()).getName());
	}

	@Test
	void updateNotManagedDelegate() {
		// The replaced delegate must also be managed: here, it is the one of another user on the root node
		final var other = repository.findBy("receiver", DEFAULT_USER);
		initSpringSecurityContext("user1");
		final var delegate = new DelegateNode();
		delegate.setId(other.getId());
		delegate.setNode("service:build:jenkins");
		delegate.setReceiver("user1");
		Assertions.assertThrows(NotFoundException.class, () -> resource.update(delegate));
		em.flush();
		em.clear();
		Assertions.assertEquals("service", repository.findOneExpected(other.getId()).getName());
	}

	@Test
	void updateManagedDelegate() {
		final var delegate = repository.findBy("receiver", "user1");
		initSpringSecurityContext("user1");
		final var vo = new DelegateNode();
		vo.setId(delegate.getId());
		vo.setNode("service:build:jenkins:dig");
		vo.setReceiver("user1");
		resource.update(vo);
		em.flush();
		em.clear();
		Assertions.assertEquals("service:build:jenkins:dig", repository.findOneExpected(delegate.getId()).getName());
	}

	@Test
	void deleteNotAdmin() {
		// A delegate without the admin flag on the node does not allow to delete the delegates of this node
		final var subscriber = new DelegateNode();
		subscriber.setNode("service:build:jenkins");
		subscriber.setReceiver("user2");
		subscriber.setCanSubscribe(true);
		repository.saveAndFlush(subscriber);
		final int user1Delegate = repository.findBy("receiver", "user1").getId();
		initSpringSecurityContext("user2");
		Assertions.assertThrows(NotFoundException.class, () -> resource.delete(user1Delegate));
		Assertions.assertTrue(repository.existsById(user1Delegate));
	}

	@Test
	void findByIdNotVisible() {
		final int rootDelegate = repository.findBy("receiver", DEFAULT_USER).getId();
		initSpringSecurityContext("any");
		Assertions.assertThrows(NotFoundException.class, () -> resource.findById(rootDelegate));
	}

	@Test
	void findAllCriteriaUser() {
		final var items = resource.findAll(newUriInfo(), DEFAULT_USER);
		Assertions.assertEquals(1, items.getData().size());
		Assertions.assertEquals(1, items.getRecordsFiltered());
		Assertions.assertEquals(1, items.getRecordsTotal());
		final var delegateNode = items.getData().getFirst();
		Assertions.assertEquals(DEFAULT_USER, delegateNode.getReceiver());
		Assertions.assertEquals(ReceiverType.USER, delegateNode.getReceiverType());
		Assertions.assertEquals("service", delegateNode.getName());
		Assertions.assertTrue(delegateNode.isCanAdmin());
		Assertions.assertTrue(delegateNode.isCanWrite());
		Assertions.assertTrue(delegateNode.isCanSubscribe());
	}

	@Test
	void findById() {
		final var items = resource.findAll(newUriInfo(), "jenkins");
		final var delegateNode = resource.findById(items.getData().getFirst().getId());
		Assertions.assertEquals("service:build:jenkins", delegateNode.getName());
	}

	@Test
	void findAllCriteriaNode() {
		final var items = resource.findAll(newUriInfo(), "jenkins");
		Assertions.assertEquals(1, items.getData().size());
		Assertions.assertEquals(1, items.getRecordsFiltered());
		Assertions.assertEquals(1, items.getRecordsTotal());
		final var delegateNode = items.getData().getFirst();
		Assertions.assertEquals("user1", delegateNode.getReceiver());
		Assertions.assertEquals(ReceiverType.USER, delegateNode.getReceiverType());
		Assertions.assertEquals("service:build:jenkins", delegateNode.getName());
		Assertions.assertTrue(delegateNode.isCanAdmin());
		Assertions.assertTrue(delegateNode.isCanWrite());
		Assertions.assertTrue(delegateNode.isCanSubscribe());
	}

	@Test
	void findAllNoCriteriaOrder() {
		final var uriInfo = mock(UriInfo.class);
		when(uriInfo.getQueryParameters()).thenReturn(new MetadataMap<>());
		uriInfo.getQueryParameters().add("draw", "1");
		uriInfo.getQueryParameters().add("start", "0");
		uriInfo.getQueryParameters().add("length", "10");
		uriInfo.getQueryParameters().add("columns[0][data]", "receiver");
		uriInfo.getQueryParameters().add("order[0][column]", "0");
		uriInfo.getQueryParameters().add("order[0][dir]", "desc");

		final var items = resource.findAll(uriInfo, " ");
		Assertions.assertEquals(3, items.getData().size());
		Assertions.assertEquals(3, items.getRecordsFiltered());
		Assertions.assertEquals(3, items.getRecordsTotal());
		final var delegateNode = items.getData().get(1);
		Assertions.assertEquals(DEFAULT_USER, delegateNode.getReceiver());
		Assertions.assertEquals(ReceiverType.USER, delegateNode.getReceiverType());
		Assertions.assertEquals("service", delegateNode.getName());
		Assertions.assertTrue(delegateNode.isCanAdmin());
		Assertions.assertTrue(delegateNode.isCanWrite());
		Assertions.assertTrue(delegateNode.isCanSubscribe());
	}

}
