/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.iam;

import java.util.Collections;
import java.util.Date;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ligoj.bootstrap.core.DateUtils;
import org.mockito.Mockito;

/**
 * Simple test of API beans.
 */
class BeanTest {

	@Test
	void testActivity() {
		check(new Activity(), Activity::setLastConnection, Activity::getLastConnection,
				DateUtils.newCalendar().getTime());
	}

	@Test
	void testIamConfiguration() {
		check(new IamConfiguration(), IamConfiguration::setCompanyRepository, IamConfiguration::getCompanyRepository,
				Mockito.mock(ICompanyRepository.class));
		check(new IamConfiguration(), IamConfiguration::setGroupRepository, IamConfiguration::getGroupRepository,
				Mockito.mock(IGroupRepository.class));
		check(new IamConfiguration(), IamConfiguration::setUserRepository, IamConfiguration::getUserRepository,
				Mockito.mock(IUserRepository.class));
		check(new IamConfiguration(), IamConfiguration::setNode, IamConfiguration::getNode, "service:plugin:id");
	}

	@Test
	void testCompanyLdap() throws InvalidNameException {
		final var companyLdap = new CompanyOrg("dn", "name");
		companyLdap.getCompanyTree().contains(companyLdap);
		check(companyLdap, CompanyOrg::setLdapName, CompanyOrg::getLdapName, new LdapName(""));
		check(companyLdap, CompanyOrg::setCompanyTree, CompanyOrg::getCompanyTree, Collections.emptyList());
		Assertions.assertEquals("name".hashCode(), companyLdap.hashCode());
		Assertions.assertEquals(companyLdap, companyLdap);
		Assertions.assertFalse(companyLdap.equals(null));
		Assertions.assertFalse(companyLdap.equals(new CompanyOrg("dn", "name2")));
		Assertions.assertTrue(companyLdap.equals(new CompanyOrg("dn", "name")));
	}

	@Test
	void testGroupLdap() {
		final var groupLdap = new GroupOrg("dn", "name", Collections.emptySet());
		check(groupLdap, GroupOrg::setMembers, GroupOrg::getMembers, Collections.emptySet());
		check(groupLdap, GroupOrg::setGroups, GroupOrg::getGroups, Collections.emptySet());
		check(groupLdap, GroupOrg::setSubGroups, GroupOrg::getSubGroups, Collections.emptySet());
		Assertions.assertEquals("name".hashCode(), groupLdap.hashCode());
		Assertions.assertEquals(groupLdap, groupLdap);
		Assertions.assertFalse(groupLdap.equals(null));
		Assertions.assertFalse(groupLdap.equals(new GroupOrg("dn", "name2", Collections.emptySet())));
		Assertions.assertTrue(groupLdap.equals(new GroupOrg("dn", "name", Collections.emptySet())));
	}

	@Test
	void testContainerLdap() {
		final var groupLdap = new ContainerOrg("dn", "name");
		check(groupLdap, ContainerOrg::setLocked, ContainerOrg::isLocked, true);
		check(groupLdap, ContainerOrg::setDescription, ContainerOrg::getDescription, "some");
		Assertions.assertEquals("some", groupLdap.getDescription());
		Assertions.assertEquals("name", groupLdap.getName());
	}

	@Test
	void testSimpleUserOrg() {
		final var user = new UserOrg();

		// Simple user attributes
		check(user, SimpleUser::setCompany, SimpleUser::getCompany, "company");
		check(user, SimpleUser::setDepartment, SimpleUser::getDepartment, "department");
		check(user, SimpleUser::setFirstName, SimpleUser::getFirstName, "first");
		check(user, SimpleUser::setLastName, SimpleUser::getLastName, "last");
		check(user, SimpleUser::setLocalId, SimpleUser::getLocalId, "local");
		check(user, SimpleUser::setName, SimpleUser::getName, "login");

		// InetOrg Person attributes
		check(user, SimpleUserOrg::setSecured, SimpleUserOrg::isSecured, true);
		check(user, SimpleUserOrg::setIsolated, SimpleUserOrg::getIsolated, "quarantine");
		check(user, SimpleUserOrg::setLocked, SimpleUserOrg::getLocked, new Date());
		check(user, SimpleUserOrg::setMails, SimpleUserOrg::getMails, Collections.emptyList());
		check(user, SimpleUserOrg::setLockedBy, SimpleUserOrg::getLockedBy, "some");

		// LDAP Person attributes
		check(user, UserOrg::setDn, UserOrg::getDn, "dn");
		check(user, UserOrg::setGroups, UserOrg::getGroups, Collections.emptyList());

		//
		final var user2 = new SimpleUserOrg();
		user.copy(user2);

		// Simple user attributes
		Assertions.assertEquals("company", user2.getCompany());
		Assertions.assertEquals("department", user2.getDepartment());
		Assertions.assertEquals("first", user2.getFirstName());
		Assertions.assertEquals("last", user2.getLastName());
		Assertions.assertEquals("local", user2.getLocalId());
		Assertions.assertEquals("login", user2.getName());
		Assertions.assertEquals("login", user2.getId());

		// InetOrg Person attributes
		Assertions.assertEquals("quarantine", user2.getIsolated());
		Assertions.assertNotNull(user2.getLocked());
		Assertions.assertEquals("some", user2.getLockedBy());
		Assertions.assertNotNull(user2.getMails());

		// Password status and groups are not replicated
		Assertions.assertFalse(user2.isSecured());

		Assertions.assertEquals("SimpleUser(id=login)", user.toString());

		Assertions.assertEquals(user2.hashCode(), user.hashCode());
		Assertions.assertEquals(user, user);
		Assertions.assertEquals(user, user2);
		Assertions.assertFalse(user.equals(null));
		Assertions.assertFalse(user.equals(new SimpleUserOrg()));
		Assertions.assertTrue(new SimpleUserOrg().equals(new SimpleUserOrg()));

	}

	private <T, X> void check(X bean, BiConsumer<X, T> setter, Function<X, T> getter, T value) {
		setter.accept(bean, value);
		Assertions.assertEquals(value, getter.apply(bean));
	}
}
