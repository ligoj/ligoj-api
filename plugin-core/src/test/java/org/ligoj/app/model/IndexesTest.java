/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.model;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ligoj.app.AbstractAppTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

/**
 * Check the indexes of the hot columns are created with the schema.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
class IndexesTest extends AbstractAppTest {

	@Test
	void batchFetchSize() {
		// The eager associations of a result list are loaded by batches, not one query per row
		Assertions.assertEquals("50", String.valueOf(em.getEntityManagerFactory().getProperties().get("hibernate.default_batch_fetch_size")));
	}

	@Test
	void indexes() {
		@SuppressWarnings("unchecked")
		final List<String> indexes = em.createNativeQuery("SELECT INDEX_NAME FROM INFORMATION_SCHEMA.SYSTEM_INDEXINFO")
				.getResultList().stream().map(String::valueOf).toList();
		for (final var expected : List.of("IX_EVENT_SUBSCRIPTION", "IX_EVENT_NODE", "IX_SUBSCRIPTION_PROJECT",
				"IX_SUBSCRIPTION_NODE", "IX_PARAMETER_VALUE_SUBSCRIPTION", "IX_PARAMETER_VALUE_NODE", "IX_PARAMETER_OWNER",
				"IX_DELEGATE_NODE_RECEIVER", "IX_DELEGATE_ORG_RECEIVER", "IX_CACHE_PROJECT_GROUP_PROJECT",
				"IX_CACHE_PROJECT_GROUP_GROUP", "IX_USER_LOG_DATE")) {
			Assertions.assertTrue(indexes.contains(expected), expected);
		}
	}
}
