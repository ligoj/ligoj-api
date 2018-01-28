package org.ligoj.app;

import java.util.Collections;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ligoj.app.iam.IamConfiguration;
import org.ligoj.app.iam.IamProvider;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

/**
 * Test class of {@link AbstractAppTest}
 */
public class TestAbstractAppTest extends AbstractAppTest {

	/**
	 * Only there fore coverage, no Spring involved.
	 */
	@Test
	public void coverage() {
		iamProvider = Mockito.mock(IamProvider.class);
		IamConfiguration configuration = Mockito.mock(IamConfiguration.class);
		Mockito.when(iamProvider.getConfiguration()).thenReturn(configuration);
		em = Mockito.mock(EntityManager.class);
		getUser();
		getCompany();
		getGroup();
		persistSystemEntities();
	}

	@Test
	public void getSubscription() {
		em = Mockito.mock(EntityManager.class);
		final TypedQuery<Object> typeQuery = Mockito.mock(TypedQuery.class);
		Mockito.when(typeQuery.setParameter(ArgumentMatchers.anyInt(), ArgumentMatchers.any())).thenReturn(typeQuery);
		Mockito.when(typeQuery.setMaxResults(1)).thenReturn(typeQuery);
		Mockito.when(typeQuery.getResultList()).thenReturn(Collections.singletonList(3));
		Mockito.when(em.createQuery(ArgumentMatchers.anyString(), ArgumentMatchers.any())).thenReturn(typeQuery);
		Assertions.assertEquals(3, getSubscription("some", "service"));
	}

}
