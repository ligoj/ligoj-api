/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.node;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.transaction.Transactional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ligoj.app.AbstractAppTest;
import org.ligoj.app.api.CredentialExpiryLevel;
import org.ligoj.app.dao.NodeRepository;
import org.ligoj.app.model.CredentialExpirySource;
import org.ligoj.app.model.Node;
import org.ligoj.bootstrap.resource.system.configuration.ConfigurationResource;
import org.ligoj.bootstrap.resource.system.session.ApplicationSettings;
import org.ligoj.bootstrap.resource.system.session.SessionSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Test class of {@link CredentialExpiryPolicy}
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
class CredentialExpiryPolicyTest extends AbstractAppTest {

	private static final Instant NOW = Instant.parse("2026-11-20T10:00:00Z");

	@Autowired
	private CredentialExpiryPolicy policy;

	@Autowired
	private ConfigurationResource configuration;

	@BeforeEach
	void prepare() throws IOException {
		persistEntities("csv", new Class<?>[] { Node.class }, StandardCharsets.UTF_8);
		persistSystemEntities();
	}

	@AfterEach
	void cleanCache() {
		// The configuration cache is not rolled back with the transaction
		clearAllCache();
	}

	@Test
	void getLevel() {
		Assertions.assertNull(CredentialExpiryPolicy.getLevel(null, NOW, 14));
		// Expiring now is already expired
		Assertions.assertEquals(CredentialExpiryLevel.EXPIRED, CredentialExpiryPolicy.getLevel(NOW, NOW, 14));
		Assertions.assertEquals(CredentialExpiryLevel.EXPIRED, CredentialExpiryPolicy.getLevel(NOW.minusSeconds(1), NOW, 14));
		Assertions.assertEquals(CredentialExpiryLevel.WARNING,
				CredentialExpiryPolicy.getLevel(NOW.plus(14, ChronoUnit.DAYS), NOW, 14));
		Assertions.assertNull(CredentialExpiryPolicy.getLevel(NOW.plus(14, ChronoUnit.DAYS).plusSeconds(1), NOW, 14));
	}

	@Test
	void getWarningDays() {
		Assertions.assertEquals(CredentialExpiryPolicy.DEFAULT_WARNING_DAYS, policy.getWarningDays());
		configuration.put(CredentialExpiryPolicy.CONF_WARNING_DAYS, "30");
		Assertions.assertEquals(30, policy.getWarningDays());
	}

	@Autowired
	private NodeRepository repository;

	private void expire(final String node, final Instant expiry) {
		repository.updateCredentialExpiry(node, expiry, CredentialExpirySource.MANUAL);
	}

	private SessionSettings newSettings() {
		final var settings = new SessionSettings();
		settings.setUserSettings(new HashMap<>());
		// Injected by Spring in a session: no setter
		ReflectionTestUtils.setField(settings, "applicationSettings", new ApplicationSettings());
		return settings;
	}

	private Object decorate() {
		final var settings = newSettings();
		policy.decorate(settings);
		return settings.getUserSettings().get("warnings");
	}

	@Test
	void decorateFeature() {
		// Every user's UI knows the API stores the credential expiry: without it, the node dialog neither shows nor sends it
		initSpringSecurityContext("any");
		final var settings = newSettings();
		policy.decorate(settings);
		Assertions.assertEquals("true", settings.getApplicationSettings().getData().get("credential-expiry"));
	}

	@Test
	void decorateAdministrator() {
		final var now = Instant.now();
		final var soon = now.plus(3, ChronoUnit.DAYS);
		expire("service:kpi:sonar:bpr", now.minus(1, ChronoUnit.DAYS));
		expire("service:build:jenkins:bpr", soon);
		// Beyond the 14 days: no warning
		expire("service:bt:jira:6", now.plus(30, ChronoUnit.DAYS));
		initSpringSecurityContextAdmin(DEFAULT_USER);

		Assertions.assertEquals(List.of(
				Map.of("code", "node-credential-expired", "parameters", Map.of("count", "1", "node", "SonarQube DIG"),
						"link", "/system/node"),
				Map.of("code", "node-credential-expiring", "parameters", Map.of("count", "1", "days", "14", "node",
						"Jenkins BPR", "date", LocalDate.ofInstant(soon, ZoneOffset.UTC).toString()), "link", "/system/node")),
				decorate());
	}

	@Test
	void decorateNotAdministrator() {
		expire("service:kpi:sonar:bpr", Instant.now().minus(1, ChronoUnit.DAYS));
		initSpringSecurityContext("any");
		Assertions.assertNull(decorate());
	}

	@Test
	void decorateNothingToWarn() {
		initSpringSecurityContextAdmin(DEFAULT_USER);
		Assertions.assertNull(decorate());
	}
}
