/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.log;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ligoj.app.AbstractAppTest;
import org.ligoj.app.dao.UserLogRepository;
import org.ligoj.app.model.UserLog;
import org.ligoj.bootstrap.core.security.SecurityHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * {@link UserLogResource} test cases.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
class UserLogResourceTest extends AbstractAppTest {

	@Autowired
	private UserLogResource resource;

	@Autowired
	private UserLogRepository repository;

	@Test
	void log() {
		initSpringSecurityContext("alice");
		final var count = repository.count();

		final var vo = new UserLogEditionVo();
		vo.setMessage("Boom");
		vo.setUrl("/home");
		resource.log(vo);

		Assertions.assertEquals(count + 1, repository.count());
		final var entity = repository.findAll().getFirst();
		Assertions.assertEquals("alice", entity.getUser());
		Assertions.assertEquals("Boom", entity.getMessage());
		Assertions.assertEquals("/home", entity.getUrl());
		Assertions.assertNotNull(entity.getDate());
		Assertions.assertTrue(entity.getDate().plusSeconds(5).isAfter(Instant.now()));
	}

	@Test
	void logTruncatesMessage() {
		initSpringSecurityContext("alice");
		final var vo = new UserLogEditionVo();
		vo.setMessage("x".repeat(2500));
		vo.setUrl("/home");
		resource.log(vo);

		final var entity = repository.findAll().getFirst();
		Assertions.assertEquals(2000, entity.getMessage().length());
	}

	@Test
	void findAll() {
		initSpringSecurityContext(DEFAULT_USER, new SimpleGrantedAuthority(SecurityHelper.ADMIN));
		final var now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		newLog("alice", now.minus(2, ChronoUnit.DAYS), "old");
		newLog("bob", now, "recent");

		final var uriInfo = newUriInfoDesc("date");
		final var result = resource.findAll(uriInfo, null, null);

		Assertions.assertEquals(2, result.getData().size());
		// Sorted by date descending: most recent first
		Assertions.assertEquals("recent", result.getData().get(0).getMessage());
		Assertions.assertEquals("bob", result.getData().get(0).getUser());
		Assertions.assertEquals("/page", result.getData().get(0).getUrl());
		Assertions.assertNotNull(result.getData().get(0).getId());
		Assertions.assertEquals("old", result.getData().get(1).getMessage());
	}

	@Test
	void findAllFilterFrom() {
		initSpringSecurityContext(DEFAULT_USER, new SimpleGrantedAuthority(SecurityHelper.ADMIN));
		final var now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		newLog("alice", now.minus(2, ChronoUnit.DAYS), "old");
		newLog("bob", now, "recent");

		// Keep only logs at or after yesterday
		final var from = now.minus(1, ChronoUnit.DAYS).toEpochMilli();
		final var result = resource.findAll(newUriInfoDesc("date"), from, null);

		Assertions.assertEquals(1, result.getData().size());
		Assertions.assertEquals("recent", result.getData().getFirst().getMessage());
	}

	@Test
	void findAllFilterTo() {
		initSpringSecurityContext(DEFAULT_USER, new SimpleGrantedAuthority(SecurityHelper.ADMIN));
		final var now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		newLog("alice", now.minus(2, ChronoUnit.DAYS), "old");
		newLog("bob", now, "recent");

		// Keep only logs at or before yesterday
		final var to = now.minus(1, ChronoUnit.DAYS).toEpochMilli();
		final var result = resource.findAll(newUriInfoDesc("date"), null, to);

		Assertions.assertEquals(1, result.getData().size());
		Assertions.assertEquals("old", result.getData().getFirst().getMessage());
	}

	private void newLog(final String user, final Instant date, final String message) {
		final var entity = new UserLog();
		entity.setUser(user);
		entity.setDate(date);
		entity.setMessage(message);
		entity.setUrl("/page");
		em.persist(entity);
		em.flush();
	}
}
