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

	private static final String ALIAS = "_arg__";
	private static final String Q_ARG = "?dn__";
	private static final String Q_USER = "?user__";
	@Autowired
	private SecuritySpringDataListener listener;

	@Test(expected = IllegalArgumentException.class)
	public void visiblegroupArgsError() {
		listener.getSqlFunctions().get("visiblegroup").render(null, Collections.emptyList(), null);
	}

	private String assertFunction(final String name, final int nbQueryParam, final String sql, String... args) {
		final String query = listener.getSqlFunctions().get(name).render(null, Arrays.asList(args), null);
		Assert.assertEquals(nbQueryParam, StringUtils.countMatches(query, '?'));
		Assert.assertTrue(query + "-- not contains --" + sql, query.contains(sql));
		return query;
	}

	@Test
	public void visibleproject() {
		assertFunction("visibleproject", 5, "_p__.team_leader=?user__", "_p__.id", ALIAS, Q_USER, Q_USER, Q_USER,
				Q_USER, Q_USER);
	}

	@Test
	public void visiblegroup() {
		assertFunction("visiblegroup", 4, "WHERE _arg__=s_d5.dn", ALIAS, Q_USER, Q_USER, Q_USER, Q_USER);
	}

	@Test
	public void visiblecompany() {
		assertFunction("visiblecompany", 4, "WHERE _arg__=s_d3.dn", ALIAS, Q_USER, Q_USER, Q_USER, Q_USER);
	}

	@Test
	public void writedn() {
		assertFunction("writedn", 3, "WHERE _arg__=s_d5.dn", ALIAS, Q_USER, Q_USER, Q_USER);
	}

	@Test
	public void admindn() {
		assertFunction("admindn", 3, "_arg__=s_d5.dn OR _arg__ LIKE", ALIAS, Q_USER, Q_USER, Q_USER);
	}

	@Test
	public void inproject() {
		final String assertFunction = assertFunction("inproject", 2, "team_leader=?user__", Q_USER, ALIAS, Q_USER,
				ALIAS);
		Assert.assertTrue(assertFunction.contains("pj10.id=_arg__"));
		Assert.assertTrue(assertFunction.contains("cm.\"user\"=?user__"));
		Assert.assertTrue(assertFunction.contains("s_pj9.id=_arg__"));
	}

	@Test
	public void inproject2() {
		final String assertFunction = assertFunction("inproject2", 4, "team_leader=?user__", Q_USER, Q_ARG, Q_USER,
				Q_ARG);
		Assert.assertTrue(assertFunction.contains("id=?dn__"));
		Assert.assertTrue(assertFunction.contains("cm.\"user\"=?user__"));
		Assert.assertTrue(assertFunction.contains("cpg.project=?dn__"));
	}

	@Test
	public void inprojectkey() {
		final String assertFunction = assertFunction("inprojectkey", 2, "team_leader=?user__", Q_USER, ALIAS, Q_USER,
				ALIAS);
		Assert.assertTrue(assertFunction.contains("pj11.pkey=_arg__"));
		Assert.assertTrue(assertFunction.contains("cm.\"user\"=?user__"));
		Assert.assertTrue(assertFunction.contains("s_pj8.pkey=_arg__"));
	}

	@Test
	public void inprojectkey2() {
		final String assertFunction = assertFunction("inprojectkey2", 4, "team_leader=?user__ AND pkey=?dn__", Q_USER,
				Q_ARG, Q_USER, Q_ARG);
		Assert.assertTrue(assertFunction.contains("cm.\"user\"=?user__ AND pj.pkey=?dn__"));
	}

	@Test
	public void ingroup() {
		final String assertFunction = assertFunction("ingroup", 1, "cm.\"user\"=?user__", Q_USER, ALIAS, ALIAS);
		Assert.assertTrue(assertFunction.contains("s_cg6.id=_arg__"));
		Assert.assertTrue(assertFunction.contains("id=_arg__"));
	}

	@Test
	public void incompany() {
		final String assertFunction = assertFunction("incompany", 1, "cu.id=?user__", Q_USER, ALIAS, ALIAS);
		Assert.assertTrue(assertFunction.contains("s_cc7.id=_arg__"));
		Assert.assertTrue(assertFunction.contains("id=_arg__"));
	}

	@Test
	public void ingroup2() {
		final String assertFunction = assertFunction("ingroup2", 3, "cm.\"user\"=?user__ AND cg.id=?dn__", Q_USER,
				Q_ARG, Q_ARG);
		Assert.assertTrue(assertFunction.contains("id=?dn__"));
	}

	@Test
	public void incompany2() {
		final String assertFunction = assertFunction("incompany2", 3, "cu.id=?user__", Q_USER, Q_ARG, Q_ARG);
		Assert.assertTrue(assertFunction.contains("cc.id=?dn__"));
		Assert.assertTrue(assertFunction.contains("id=?dn__"));
	}

}
