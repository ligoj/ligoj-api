/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app;

import java.util.Collections;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ligoj.app.iam.IamConfiguration;
import org.ligoj.app.iam.IamProvider;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

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
	 * Only there fore coverage, no Spring involved.
	 */
	@Test
	void coverage() {
		iamProvider = Mockito.mock(IamProvider.class);
		var configuration = Mockito.mock(IamConfiguration.class);
		Mockito.when(iamProvider.getConfiguration()).thenReturn(configuration);
		em = Mockito.mock(EntityManager.class);
		getUser();
		getCompany();
		getGroup();
		persistSystemEntities();
	}

	@Test
	void getSubscription() {
		em = Mockito.mock(EntityManager.class);
		@SuppressWarnings("unchecked")
		final TypedQuery<Object> typeQuery = Mockito.mock(TypedQuery.class);
		Mockito.when(typeQuery.setParameter(ArgumentMatchers.anyInt(), ArgumentMatchers.any())).thenReturn(typeQuery);
		Mockito.when(typeQuery.setMaxResults(1)).thenReturn(typeQuery);
		Mockito.when(typeQuery.getResultList()).thenReturn(Collections.singletonList(3));
		Mockito.when(em.createQuery(ArgumentMatchers.anyString(), ArgumentMatchers.any())).thenReturn(typeQuery);
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
