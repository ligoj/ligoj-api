/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.node;

import java.time.Instant;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ligoj.bootstrap.core.json.ObjectMapperTrim;

/**
 * Test class of {@link NodeEditionVo}: a client without the credential expiry feature does not send it.
 */
class NodeEditionVoTest {

	private final ObjectMapperTrim mapper = new ObjectMapperTrim();

	@Test
	void credentialExpiryAbsent() {
		final var vo = mapper.readValue("{\"id\":\"service:bt:jira:6\",\"name\":\"Jira\"}", NodeEditionVo.class);
		Assertions.assertFalse(vo.isCredentialExpirySet());
		Assertions.assertNull(vo.getCredentialExpiry());
	}

	@Test
	void credentialExpiryRemoved() {
		final var vo = mapper.readValue("{\"id\":\"service:bt:jira:6\",\"credentialExpiry\":null}", NodeEditionVo.class);
		Assertions.assertTrue(vo.isCredentialExpirySet());
		Assertions.assertNull(vo.getCredentialExpiry());
	}

	@Test
	void credentialExpiry() {
		final var vo = mapper.readValue("{\"id\":\"service:bt:jira:6\",\"credentialExpiry\":1795996800000}", NodeEditionVo.class);
		Assertions.assertTrue(vo.isCredentialExpirySet());
		Assertions.assertEquals(Instant.parse("2026-11-30T00:00:00Z"), vo.getCredentialExpiry());
	}
}
