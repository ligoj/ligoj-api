/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app;

import org.ligoj.app.iam.ICompanyRepository;
import org.ligoj.app.iam.IGroupRepository;
import org.ligoj.app.iam.IUserRepository;
import org.ligoj.app.iam.IamProvider;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base test class for JPA tests.
 */
public abstract class AbstractAppTest extends org.ligoj.bootstrap.AbstractAppTest {

	@Autowired
	protected IamProvider iamProvider;

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
	 * Return the subscription identifier of MDA. Assumes there is only one subscription for a service.
	 *
	 * @param project
	 *            The project name of the subscription to return.
	 * @param service
	 *            The subscribed service of the project. May be a service or a tool or an instance. <code>LIKE</code> is
	 *            used.
	 * @return The subscription identifier.
	 */
	protected int getSubscription(final String project, final String service) {
		return em
				.createQuery("SELECT id FROM Subscription WHERE project.name = ?1 AND node.id LIKE CONCAT(?2,'%')",
						Integer.class)
				.setParameter(1, project).setParameter(2, service).setMaxResults(1).getResultList().get(0);
	}
}
