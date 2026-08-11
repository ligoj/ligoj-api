/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class of {@link AbstractAppTest}
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
class TestAbstractAppTest extends AbstractAppTest {

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
	void testRegisterSingleton() {
		final var singleton = new Object();
		Assertions.assertThrows(NoSuchBeanDefinitionException.class, () -> applicationContext.getBean("my_dynamical_bean"));
		registerSingleton("my_dynamical_bean", singleton);
		Assertions.assertEquals(singleton, applicationContext.getBean("my_dynamical_bean"));
		destroySingleton("my_dynamical_bean");
		Assertions.assertThrows(NoSuchBeanDefinitionException.class, () -> applicationContext.getBean("my_dynamical_bean"));

		// Destroy method accepts already destroyed bean
		destroySingleton("my_dynamical_bean");
	}

}
