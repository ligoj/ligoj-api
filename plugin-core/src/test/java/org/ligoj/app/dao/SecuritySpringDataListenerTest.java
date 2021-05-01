/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.dao;

import java.util.Arrays;
import java.util.Collections;

import javax.transaction.Transactional;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.QueryException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Test class of {@link SecuritySpringDataListener}
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
class SecuritySpringDataListenerTest {

	private static final String ALIAS = "_arg__";
	private static final String Q_ARG = "?dn__";
	private static final String Q_USER = "?user__";
	@Autowired
	private SecuritySpringDataListener listener;

	@Test
	void visiblegroupArgsError() {
		final var function = listener.getSqlFunctions().get("visiblegroup");
		final var empty = Collections.emptyList();
		Assertions.assertThrows(QueryException.class, () -> function.render(null, empty, null));
	}

	private String assertFunction(final String name, final int nbQueryParam, final String sql, String... args) {
		final var query = listener.getSqlFunctions().get(name).render(null, Arrays.asList(args), null);
		Assertions.assertEquals(nbQueryParam, StringUtils.countMatches(query, '?'));
		Assertions.assertTrue(query.contains(sql), query + "-- not contains --" + sql);
		return query;
	}

	@Test
	void visibleproject() {
		assertFunction("visibleproject", 5, "_p__.team_leader=?user__", "_p__.id", ALIAS, Q_USER, Q_USER, Q_USER,
				Q_USER, Q_USER);
	}

	@Test
	void visiblegroup() {
		assertFunction("visiblegroup", 4, "WHERE _arg__=s_d5.dn", ALIAS, Q_USER, Q_USER, Q_USER, Q_USER);
	}

	@Test
	void visiblecompany() {
		assertFunction("visiblecompany", 4, "WHERE _arg__=s_d3.dn", ALIAS, Q_USER, Q_USER, Q_USER, Q_USER);
	}

	@Test
	void writedn() {
		assertFunction("writedn", 3, "WHERE _arg__=s_d5.dn", ALIAS, Q_USER, Q_USER, Q_USER);
	}

	@Test
	void admindn() {
		assertFunction("admindn", 3, "_arg__=s_d5.dn OR _arg__ LIKE", ALIAS, Q_USER, Q_USER, Q_USER);
	}

	@Test
	void inproject() {
		final var assertFunction = assertFunction("inproject", 2, "team_leader=?user__", Q_USER, ALIAS, Q_USER, ALIAS);
		Assertions.assertTrue(assertFunction.contains("pj10.id=_arg__"));
		Assertions.assertTrue(assertFunction.contains("cm.\"user\"=?user__"));
		Assertions.assertTrue(assertFunction.contains("s_pj9.id=_arg__"));
	}

	@Test
	void inproject2() {
		final var assertFunction = assertFunction("inproject2", 4, "team_leader=?user__", Q_USER, Q_ARG, Q_USER, Q_ARG);
		Assertions.assertTrue(assertFunction.contains("id=?dn__"));
		Assertions.assertTrue(assertFunction.contains("cm.\"user\"=?user__"));
		Assertions.assertTrue(assertFunction.contains("cpg.project=?dn__"));
	}

	@Test
	void inprojectkey() {
		final var assertFunction = assertFunction("inprojectkey", 2, "team_leader=?user__", Q_USER, ALIAS, Q_USER,
				ALIAS);
		Assertions.assertTrue(assertFunction.contains("pj11.pkey=_arg__"));
		Assertions.assertTrue(assertFunction.contains("cm.\"user\"=?user__"));
		Assertions.assertTrue(assertFunction.contains("s_pj8.pkey=_arg__"));
	}

	@Test
	void inprojectkey2() {
		final var assertFunction = assertFunction("inprojectkey2", 4, "team_leader=?user__ AND pkey=?dn__", Q_USER,
				Q_ARG, Q_USER, Q_ARG);
		Assertions.assertTrue(assertFunction.contains("cm.\"user\"=?user__ AND pj.pkey=?dn__"));
	}

	@Test
	void ingroup() {
		final var assertFunction = assertFunction("ingroup", 1, "cm.\"user\"=?user__", Q_USER, ALIAS, ALIAS);
		Assertions.assertTrue(assertFunction.contains("s_cg6.id=_arg__"));
		Assertions.assertTrue(assertFunction.contains("id=_arg__"));
	}

	@Test
	void incompany() {
		final var assertFunction = assertFunction("incompany", 1, "cu.id=?user__", Q_USER, ALIAS, ALIAS);
		Assertions.assertTrue(assertFunction.contains("s_cc7.id=_arg__"));
		Assertions.assertTrue(assertFunction.contains("id=_arg__"));
	}

	@Test
	void ingroup2() {
		final var assertFunction = assertFunction("ingroup2", 3, "cm.\"user\"=?user__ AND cg.id=?dn__", Q_USER, Q_ARG,
				Q_ARG);
		Assertions.assertTrue(assertFunction.contains("id=?dn__"));
	}

	@Test
	void incompany2() {
		final var assertFunction = assertFunction("incompany2", 3, "cu.id=?user__", Q_USER, Q_ARG, Q_ARG);
		Assertions.assertTrue(assertFunction.contains("cc.id=?dn__"));
		Assertions.assertTrue(assertFunction.contains("id=?dn__"));
	}

}
