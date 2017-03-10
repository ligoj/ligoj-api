package org.ligoj.app;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import org.ligoj.bootstrap.model.system.SystemAuthorization;
import org.ligoj.bootstrap.model.system.SystemRole;
import org.ligoj.bootstrap.model.system.SystemRoleAssignment;
import org.ligoj.bootstrap.model.system.SystemUser;
import org.ligoj.bootstrap.model.system.SystemAuthorization.AuthorizationType;
import org.ligoj.app.iam.ICompanyRepository;
import org.ligoj.app.iam.IGroupRepository;
import org.ligoj.app.iam.IUserRepository;
import org.ligoj.app.iam.IamProvider;

/**
 * Base test class for JPA tests.
 */
public abstract class AbstractJpaTest extends org.ligoj.bootstrap.AbstractJpaTest {

	@Autowired
	protected IamProvider iamProvider;

	/**
	 * Prepare the Spring Security in the context, not the REST one.
	 */
	@Override
	@Before
	public void setUp() {
		initSpringSecurityContext(getAuthenticationName());
	}

	/**
	 * User repository provider.
	 * 
	 * @return User repository provider.
	 */
	protected IUserRepository getUser() {
		return iamProvider.getConfiguration().getUserLdapRepository();
	}

	/**
	 * Company repository provider.
	 * 
	 * @return Company repository provider.
	 */
	protected ICompanyRepository getCompany() {
		return iamProvider.getConfiguration().getCompanyLdapRepository();
	}

	/**
	 * Group repository provider.
	 * 
	 * @return Group repository provider.
	 */
	protected IGroupRepository getGroup() {
		return iamProvider.getConfiguration().getGroupLdapRepository();
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
		authorization.setType(AuthorizationType.BUSINESS);
		authorization.setPattern(".*");
		authorization.setRole(role);
		em.persist(authorization);
		final SystemRoleAssignment assignment = new SystemRoleAssignment();
		assignment.setRole(role);
		assignment.setUser(user);
		em.persist(assignment);
	}
}
