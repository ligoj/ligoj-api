package org.ligoj.app.iam;

import java.util.Collections;
import java.util.Date;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

import org.junit.Assert;
import org.junit.Test;
import org.ligoj.bootstrap.core.DateUtils;
import org.mockito.Mockito;

/**
 * Simple test of API beans.
 */
public class BeanTest {

	@Test
	public void testActivity() {
		check(new Activity(), Activity::setLastConnection, Activity::getLastConnection, DateUtils.newCalendar().getTime());
	}

	@Test
	public void testIamConfiguration() {
		check(new IamConfiguration(), IamConfiguration::setCompanyRepository, IamConfiguration::getCompanyRepository, Mockito.mock(ICompanyRepository.class));
		check(new IamConfiguration(), IamConfiguration::setGroupRepository, IamConfiguration::getGroupRepository, Mockito.mock(IGroupRepository.class));
		check(new IamConfiguration(), IamConfiguration::setUserRepository, IamConfiguration::getUserRepository, Mockito.mock(IUserRepository.class));
	}


	@Test
	public void testCompanyLdap() throws InvalidNameException {
		final CompanyOrg companyLdap = new CompanyOrg("dn", "name");
		companyLdap.getCompanyTree().contains(companyLdap);
		check(companyLdap, CompanyOrg::setLdapName, CompanyOrg::getLdapName, new LdapName(""));
		check(companyLdap, CompanyOrg::setCompanyTree, CompanyOrg::getCompanyTree, Collections.emptyList());
		Assert.assertEquals("name".hashCode(), companyLdap.hashCode());
		Assert.assertEquals(companyLdap, companyLdap);
		Assert.assertFalse(companyLdap.equals(null));
		Assert.assertFalse(companyLdap.equals(new CompanyOrg("dn", "name2")));
		Assert.assertTrue(companyLdap.equals(new CompanyOrg("dn", "name")));
	}

	@Test
	public void testGroupLdap() {
		final GroupOrg groupLdap = new GroupOrg("dn", "name", Collections.emptySet());
		check(groupLdap, GroupOrg::setMembers, GroupOrg::getMembers, Collections.emptySet());
		check(groupLdap, GroupOrg::setGroups, GroupOrg::getGroups, Collections.emptySet());
		check(groupLdap, GroupOrg::setSubGroups, GroupOrg::getSubGroups, Collections.emptySet());
		Assert.assertEquals("name".hashCode(), groupLdap.hashCode());
		Assert.assertEquals(groupLdap, groupLdap);
		Assert.assertFalse(groupLdap.equals(null));
		Assert.assertFalse(groupLdap.equals(new GroupOrg("dn", "name2", Collections.emptySet())));
		Assert.assertTrue(groupLdap.equals(new GroupOrg("dn", "name", Collections.emptySet())));
	}

	@Test
	public void testContainerLdap() {
		final ContainerOrg groupLdap = new ContainerOrg("dn", "name");
		check(groupLdap, ContainerOrg::setLocked, ContainerOrg::isLocked, true);
		check(groupLdap, ContainerOrg::setDescription, ContainerOrg::getDescription, "some");
		Assert.assertEquals("some", groupLdap.getDescription());
		Assert.assertEquals("name", groupLdap.getName());
	}

	@Test
	public void testSimpleUserLdap() {
		final UserOrg user = new UserOrg();

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
		final SimpleUserOrg user2 = new SimpleUserOrg();
		user.copy(user2);

		// Simple user attributes
		Assert.assertEquals("company", user2.getCompany());
		Assert.assertEquals("department", user2.getDepartment());
		Assert.assertEquals("first", user2.getFirstName());
		Assert.assertEquals("last", user2.getLastName());
		Assert.assertEquals("local", user2.getLocalId());
		Assert.assertEquals("login", user2.getName());
		Assert.assertEquals("login", user2.getId());

		// InetOrg Person attributes
		Assert.assertEquals("quarantine", user2.getIsolated());
		Assert.assertNotNull(user2.getLocked());
		Assert.assertEquals("some", user2.getLockedBy());
		Assert.assertNotNull(user2.getMails());

		// Password status and groups are not replicated
		Assert.assertFalse(user2.isSecured());

		Assert.assertEquals("SimpleUser(id=login)", user.toString());

		Assert.assertEquals(user2.hashCode(), user.hashCode());
		Assert.assertEquals(user, user);
		Assert.assertEquals(user, user2);
		Assert.assertFalse(user.equals(null));
		Assert.assertFalse(user.equals(new SimpleUserOrg()));
		Assert.assertTrue(new SimpleUserOrg().equals(new SimpleUserOrg()));

	}

	private <T, X> void check(X bean, BiConsumer<X, T> setter, Function<X, T> getter, T value) {
		setter.accept(bean, value);
		Assert.assertEquals(value, getter.apply(bean));
	}
}
