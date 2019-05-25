/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app;

import java.util.Collections;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

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
	void testRegisterSingleton() {
		final Object singleton = new Object();
		Assertions.assertThrows(NoSuchBeanDefinitionException.class, () -> {
			applicationContext.getBean("my_dynamical_bean");
		});
		registerSingleton("my_dynamical_bean", singleton);
		Assertions.assertEquals(singleton, applicationContext.getBean("my_dynamical_bean"));
		destroySingleton("my_dynamical_bean");
		Assertions.assertThrows(NoSuchBeanDefinitionException.class, () -> {
			applicationContext.getBean("my_dynamical_bean");
		});
		
		// Destroy method accepts already destroyed bean
		destroySingleton("my_dynamical_bean");
	}

}
