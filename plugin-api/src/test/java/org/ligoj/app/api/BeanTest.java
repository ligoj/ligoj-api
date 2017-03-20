package org.ligoj.app.api;

import java.util.Collections;
import java.util.Date;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Test;
import org.ligoj.bootstrap.core.DateUtils;
import org.springframework.ldap.support.LdapUtils;

/**
 * Simple test of API beans.
 */
public class BeanTest {

	@Test
	public void testActivity() {
		check(new Activity(), Activity::setLastConnection, Activity::getLastConnection, DateUtils.newCalendar().getTime());
	}

	@Test
	public void testCompanyLdap() {
		final CompanyLdap companyLdap = new CompanyLdap("dn", "name");
		companyLdap.getCompanyTree().contains(companyLdap);
		check(companyLdap, CompanyLdap::setLdapName, CompanyLdap::getLdapName, LdapUtils.emptyLdapName());
		check(companyLdap, CompanyLdap::setCompanyTree, CompanyLdap::getCompanyTree, Collections.emptyList());
		Assert.assertEquals("name".hashCode(), companyLdap.hashCode());
		Assert.assertEquals(companyLdap, companyLdap);
		Assert.assertFalse(companyLdap.equals(null));
		Assert.assertFalse(companyLdap.equals(new CompanyLdap("dn", "name2")));
		Assert.assertTrue(companyLdap.equals(new CompanyLdap("dn", "name")));
	}

	@Test
	public void testGroupLdap() {
		final GroupLdap groupLdap = new GroupLdap("dn", "name", Collections.emptySet());
		check(groupLdap, GroupLdap::setMembers, GroupLdap::getMembers, Collections.emptySet());
		check(groupLdap, GroupLdap::setGroups, GroupLdap::getGroups, Collections.emptySet());
		check(groupLdap, GroupLdap::setSubGroups, GroupLdap::getSubGroups, Collections.emptySet());
		Assert.assertEquals("name".hashCode(), groupLdap.hashCode());
		Assert.assertEquals(groupLdap, groupLdap);
		Assert.assertFalse(groupLdap.equals(null));
		Assert.assertFalse(groupLdap.equals(new GroupLdap("dn", "name2", Collections.emptySet())));
		Assert.assertTrue(groupLdap.equals(new GroupLdap("dn", "name", Collections.emptySet())));
	}

	@Test
	public void testContainerLdap() {
		final ContainerLdap groupLdap = new ContainerLdap("dn", "name");
		check(groupLdap, ContainerLdap::setLocked, ContainerLdap::isLocked, true);
		check(groupLdap, ContainerLdap::setDescription, ContainerLdap::getDescription, "some");
		Assert.assertEquals("some", groupLdap.getDescription());
		Assert.assertEquals("name", groupLdap.getName());
		Assert.assertEquals("some", ContainerLdap.getSafeDn(groupLdap));
		Assert.assertNull(ContainerLdap.getSafeDn(null));
	}

	@Test
	public void testSimpleUserLdap() {
		final UserLdap user = new UserLdap();

		// Simple user attributes
		check(user, SimpleUser::setCompany, SimpleUser::getCompany, "company");
		check(user, SimpleUser::setDepartment, SimpleUser::getDepartment, "department");
		check(user, SimpleUser::setFirstName, SimpleUser::getFirstName, "first");
		check(user, SimpleUser::setLastName, SimpleUser::getLastName, "last");
		check(user, SimpleUser::setLocalId, SimpleUser::getLocalId, "local");
		check(user, SimpleUser::setName, SimpleUser::getName, "login");

		// InetOrg Person attributes
		check(user, SimpleUserLdap::setNoPassword, SimpleUserLdap::isNoPassword, true);
		check(user, SimpleUserLdap::setIsolated, SimpleUserLdap::getIsolated, "quarantine");
		check(user, SimpleUserLdap::setLocked, SimpleUserLdap::getLocked, new Date());
		check(user, SimpleUserLdap::setMails, SimpleUserLdap::getMails, Collections.emptyList());
		check(user, SimpleUserLdap::setLockedBy, SimpleUserLdap::getLockedBy, "some");

		// LDAP Person attributes
		check(user, UserLdap::setDn, UserLdap::getDn, "dn");
		check(user, UserLdap::setGroups, UserLdap::getGroups, Collections.emptyList());

		//
		final SimpleUserLdap user2 = new SimpleUserLdap();
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
		Assert.assertFalse(user2.isNoPassword());

		Assert.assertEquals("SimpleUser(id=login)", user.toString());

		Assert.assertEquals(user2.hashCode(), user.hashCode());
		Assert.assertEquals(user, user);
		Assert.assertEquals(user, user2);
		Assert.assertFalse(user.equals(null));
		Assert.assertFalse(user.equals(new SimpleUserLdap()));
		Assert.assertTrue(new SimpleUserLdap().equals(new SimpleUserLdap()));

	}

	private <T, X> void check(X bean, BiConsumer<X, T> setter, Function<X, T> getter, T value) {
		setter.accept(bean, value);
		Assert.assertEquals(value, getter.apply(bean));
	}
}
