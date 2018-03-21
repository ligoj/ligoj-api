/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app;

import org.ligoj.app.iam.ICompanyRepository;
import org.ligoj.app.iam.IGroupRepository;
import org.ligoj.app.iam.IUserRepository;
import org.ligoj.app.iam.IamProvider;
import org.ligoj.bootstrap.AbstractJpaTest;
import org.ligoj.bootstrap.model.system.SystemAuthorization;
import org.ligoj.bootstrap.model.system.SystemAuthorization.AuthorizationType;
import org.ligoj.bootstrap.model.system.SystemRole;
import org.ligoj.bootstrap.model.system.SystemRoleAssignment;
import org.ligoj.bootstrap.model.system.SystemUser;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.beans.factory.support.DefaultSingletonBeanRegistry;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Base test class for JPA tests.
 */
public abstract class AbstractAppTest extends AbstractJpaTest {

	@Autowired
	protected IamProvider iamProvider;

	@Autowired
	protected CacheManager cacheManager;

	/**
	 * User repository provider.
	 * 
	 * @return User repository provider.
	 */
	protected IUserRepository getUser() {
		return iamProvider.getConfiguration().getUserRepository();
	}

	/**
	 * Company repository provider.
	 * 
	 * @return Company repository provider.
	 */
	protected ICompanyRepository getCompany() {
		return iamProvider.getConfiguration().getCompanyRepository();
	}

	/**
	 * Group repository provider.
	 * 
	 * @return Group repository provider.
	 */
	protected IGroupRepository getGroup() {
		return iamProvider.getConfiguration().getGroupRepository();
	}

	/**
	 * Persist system user, role and assignment for user DEFAULT_USER.
	 */
	protected void persistSystemEntities() {
		final SystemRole role = new SystemRole();
		role.setName("some");
		em.persist(role);
		final SystemUser user = new SystemUser();
		user.setLogin(DEFAULT_USER);
		em.persist(user);
		final SystemAuthorization authorization = new SystemAuthorization();
		authorization.setType(AuthorizationType.API);
		authorization.setPattern(".*");
		authorization.setRole(role);
		em.persist(authorization);
		final SystemRoleAssignment assignment = new SystemRoleAssignment();
		assignment.setRole(role);
		assignment.setUser(user);
		em.persist(assignment);
	}

	/**
	 * Return the subscription identifier of MDA. Assumes there is only one
	 * subscription for a service.
	 * 
	 * @param project
	 *            The project name of the subscription to return.
	 * @param service
	 *            The subscribed service of the project. May be a service or a tool
	 *            or an instance. <code>LIKE</code> is used.
	 * @return The subscription identifier.
	 */
	protected int getSubscription(final String project, final String service) {
		return em.createQuery("SELECT id FROM Subscription WHERE project.name = ?1 AND node.id LIKE CONCAT(?2,'%')", Integer.class)
				.setParameter(1, project).setParameter(2, service).setMaxResults(1).getResultList().get(0);
	}
	
	/**
	 * Clear all caches.
	 */
	protected void clearAllCache() {
		cacheManager.getCacheNames().stream().map(cacheManager::getCache).forEach(Cache::clear);
	}

	/**
	 * Destroy the given bean instance (usually a prototype instance obtained from
	 * this factory) according to its bean definition.
	 * <p>
	 * Any exception that arises during destruction should be caught and logged
	 * instead of propagated to the caller of this method.
	 * 
	 * @param beanName
	 *            the name of the bean definition
	 */
	protected void destroySingleton(final String beanName) {
		try {
			((DefaultSingletonBeanRegistry) ((ConfigurableApplicationContext) applicationContext).getBeanFactory())
					.destroySingleton(beanName);
		} catch (final NoSuchBeanDefinitionException e) {
			// Ignore
		}
	}

	/**
	 * Register a singleton within the current application context. Don't forget to
	 * destroy this singleton with a try-finally at the end of your tests.
	 * 
	 * @param beanName
	 *            the name of the bean definition.
	 * @param singleton
	 *            the bean instance to register
	 * @return The given instance.
	 * @see #destroySingleton(String)
	 */
	protected <T> T registerSingleton(final String beanName, final T singleton) {
		((SingletonBeanRegistry) applicationContext.getAutowireCapableBeanFactory()).registerSingleton(beanName, singleton);
		return singleton;
	}
}
