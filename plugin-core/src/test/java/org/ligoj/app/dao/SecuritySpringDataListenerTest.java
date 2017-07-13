package org.ligoj.app.dao;

import java.util.Arrays;
import java.util.Collections;

import javax.transaction.Transactional;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
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

	private static final String P_ARG = "_arg__";
	private static final String P_USER = "_user__";
	private static final String Q_ARG = "?dn__";
	private static final String Q_USER = "?user__";
	@Autowired
	private SecuritySpringDataListener listener;

	@Test(expected = IllegalArgumentException.class)
	public void visiblegroupArgsError() {
		listener.getSqlFunctions().get("visiblegroup").render(null, Collections.emptyList(), null);
	}

	private void assertFunction(final String name, final int nbQueryParam, final String sql, String... args) {
		final String query = listener.getSqlFunctions().get(name).render(null, Arrays.asList(args), null);
		Assert.assertEquals(nbQueryParam, StringUtils.countMatches(query, "[?]"));
		Assert.assertTrue(query + "-- not contains --" + sql, query.contains(sql));
	}

	@Test
	public void visibleproject() {
		assertFunction("visibleproject", 3, "_p__.team_leader=_user__", "_p__.id", "g", P_USER, Q_USER, Q_USER, Q_USER,
				Q_USER);
	}

	@Test
	public void visiblegroup() {
		assertFunction("visiblegroup", 4, "WHERE _dn__=s_d5.dn", P_ARG, Q_USER, Q_USER, Q_USER, Q_USER);
	}

	@Test
	public void visiblecompany() {
		assertFunction("visiblecompany", 4, "WHERE _dn__=s_d5.dn", P_ARG, Q_USER, Q_USER, Q_USER, Q_USER);
	}

	@Test
	public void writedn() {
		assertFunction("writedn", 3, "WHERE _dn__=s_d5.dn", P_ARG, Q_USER, Q_USER, Q_USER);
	}

	@Test
	public void admindn() {
		assertFunction("admindn", 3, "WHERE _dn__=s_d5.dn", P_ARG, Q_USER, Q_USER, Q_USER);
	}

	@Test
	public void inproject() {
		assertFunction("inproject", 2, "_p__.team_leader=_user__", "_p__.id", Q_USER, Q_USER);
	}

	@Test
	public void inprojectkey() {
		assertFunction("inprojectkey", 2, "WHERE f_pr8.pkey=_pkey__", "_pkey__", Q_USER, Q_USER);
	}

	@Test
	public void ingroup() {
		assertFunction("ingroup", 1, "_p__.team_leader=_user__", P_ARG, Q_USER);
	}

	@Test
	public void incompany() {
		assertFunction("incompany", 1, "WHERE s_cc7.id=_company__", P_ARG, Q_USER);
	}

	@Test
	public void inprojectkey2() {
		assertFunction("inprojectkey2", 2, "WHERE f_pr8.pkey=_pkey__", Q_ARG, Q_ARG, P_USER);
	}

	@Test
	public void inproject2() {
		assertFunction("inproject2", 2, "_p__.team_leader=_user__", Q_USER, Q_ARG, P_USER);
	}

	@Test
	public void ingroup2() {
		assertFunction("ingroup2", 1, "_p__.team_leader=_user__", Q_ARG, P_USER);
	}

}
