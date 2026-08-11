/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ligoj.app.iam.IamConfiguration;
import org.ligoj.app.iam.IamProvider;
import org.mockito.ArgumentMatchers;

import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class of {@link AbstractAppTest}
 */
class TestAbstractAppTest extends AbstractAppTest {

	@BeforeEach
	@Override
	public void copyIamProvider() {
		iamProviders = new IamProvider[] { null };
		super.copyIamProvider();
	}

	/**
	 * Only there for coverage, no Spring involved.
	 */
	@Test
	void coverage() {
		iamProvider = mock(IamProvider.class);
		var configuration = mock(IamConfiguration.class);
		when(iamProvider.getConfiguration()).thenReturn(configuration);
		em = mock(EntityManager.class);
		getUser();
		getCompany();
		getGroup();
		persistSystemEntities();
	}

	@Test
	void getSubscription() {
		em = mock(EntityManager.class);
		@SuppressWarnings("unchecked")
		final TypedQuery<Object> typeQuery = mock(TypedQuery.class);
		when(typeQuery.setParameter(ArgumentMatchers.anyInt(), ArgumentMatchers.any())).thenReturn(typeQuery);
		when(typeQuery.setMaxResults(1)).thenReturn(typeQuery);
		when(typeQuery.getResultList()).thenReturn(Collections.singletonList(3));
		when(em.createQuery(ArgumentMatchers.anyString(), ArgumentMatchers.any())).thenReturn(typeQuery);
		Assertions.assertEquals(3, getSubscription("some", "service"));
	}

	@Test
	void testNewUriInfoSearch() {
		Assertions.assertEquals("filter", newUriInfo("filter").getQueryParameters().getFirst("search[value]"));
	}

	@Test
	void testNewUriInfoAsc() {
		var map = newUriInfoAsc("prop").getQueryParameters();
		Assertions.assertEquals("2", map.getFirst("order[0][column]"));
		Assertions.assertEquals("prop", map.getFirst("columns[2][data]"));
		Assertions.assertEquals("asc", map.getFirst("order[0][dir]"));
	}

	@Test
	void testNewUriInfoDesc() {
		var map = newUriInfoDesc("prop").getQueryParameters();
		Assertions.assertEquals("2", map.getFirst("order[0][column]"));
		Assertions.assertEquals("prop", map.getFirst("columns[2][data]"));
		Assertions.assertEquals("desc", map.getFirst("order[0][dir]"));
	}

	@Test
	void testNewUriInfoAscSearch() {
		var map = newUriInfoAscSearch("prop", "filter").getQueryParameters();
		Assertions.assertEquals("filter", map.getFirst("search[value]"));
		Assertions.assertEquals("2", map.getFirst("order[0][column]"));
		Assertions.assertEquals("prop", map.getFirst("columns[2][data]"));
		Assertions.assertEquals("asc", map.getFirst("order[0][dir]"));
	}

}
