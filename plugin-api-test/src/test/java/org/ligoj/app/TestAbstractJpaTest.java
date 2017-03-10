package org.ligoj.app;

import javax.persistence.EntityManager;

import org.junit.Test;
import org.mockito.Mockito;

import org.ligoj.app.iam.IamConfiguration;
import org.ligoj.app.iam.IamProvider;

/**
 * Test class of {@link AbstractJpaTest}
 */
public class TestAbstractJpaTest extends AbstractJpaTest {

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
}
