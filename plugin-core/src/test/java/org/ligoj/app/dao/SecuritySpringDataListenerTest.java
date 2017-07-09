package org.ligoj.app.dao;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import javax.transaction.Transactional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Test class of {@link SecuritySpringDataListener}
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
public class SecuritySpringDataListenerTest {

	private static final String P_DN = "_dn__";
	private static final String P_USER = "_user__";
	@Autowired
	private SecuritySpringDataListener listener;

	@Test(expected = IllegalArgumentException.class)
	public void visiblegroupArgsError() {
		listener.getSqlFunctions().get("visiblegroup").render(null, Collections.emptyList(), null);
	}

	private void assertFunction(final String name, final String sql, String... args) {
		assertTrue(listener.getSqlFunctions().get(name).render(null, Arrays.asList(args), null).contains(sql));
	}

	@Test
	public void visibleproject() {
		assertFunction("visibleproject", "_p__.team_leader=_user__", "_p__.id", P_USER, P_USER, P_USER, P_USER, P_USER,
				P_USER);
	}

	@Test
	public void visiblegroup() {
		assertFunction("visiblegroup", "WHERE _dn__=s_d5.dn", P_DN, P_DN, P_USER, P_USER, P_USER);
	}

	@Test
	public void visiblecompany() {
		assertFunction("visiblecompany", "WHERE _dn__=s_d5.dn", P_DN, P_USER, P_USER, P_USER, P_USER);
	}

	@Test
	public void writedn() {
		assertFunction("writedn", "WHERE _dn__=s_d5.dn", P_DN, P_USER, P_USER, P_USER);
	}

	@Test
	public void admindn() {
		assertFunction("admindn", "WHERE _dn__=s_d5.dn", P_DN, P_USER, P_USER, P_USER);
	}

	@Test
	public void inproject() {
		assertFunction("inproject", "_p__.team_leader=_user__", "_p__.id", P_USER, P_USER);
	}

	@Test
	public void inprojectkey() {
		assertFunction("inprojectkey", "WHERE f_pr8.pkey=_pkey__", "_pkey__", P_USER, P_USER);
	}

	@Test
	public void incompany() {
		assertFunction("incompany", "WHERE f_cc7.id=_company__", "_company__", P_USER);
	}

	@Test
	public void ingroup() {
		assertFunction("ingroup", "WHERE f_cg6.id=_group__", "_group__", P_USER);
	}

}
