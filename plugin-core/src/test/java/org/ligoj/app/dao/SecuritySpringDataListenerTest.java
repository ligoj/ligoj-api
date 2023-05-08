/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.dao;

import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.QueryException;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.StringBuilderSqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;
import org.hibernate.type.internal.NamedBasicTypeImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
	private LocalContainerEntityManagerFactoryBean emf;

	@Test
	void visibleGroupArgsError() {
		final var sessionFactory = (SessionFactoryImpl) emf.getNativeEntityManagerFactory();
		final var function = (StandardSQLFunction) sessionFactory.getQueryEngine().getSqmFunctionRegistry().findFunctionDescriptor("visibleGroup");
		final List<? extends SqlAstNode> empty = Collections.emptyList();
		Assertions.assertThrows(QueryException.class, () -> function.render(null, empty, null));
	}

	private String assertFunction(final String name, final int nbQueryParam, final String sql, String... args) {
		var sb = new StringBuilder();
		var appender = new StringBuilderSqlAppender(sb);
		var translator = Mockito.mock(SqlAstTranslator.class);
		Mockito.doAnswer(invocation -> {
			appender.append(((Literal) invocation.getArgument(0)).getLiteralValue().toString());
			return null;
		}).when(translator).render(Mockito.any(SqlAstNode.class), Mockito.any(SqlAstNodeRenderingMode.class));
		var astParams = Arrays.stream(args).map(a ->
				new QueryLiteral<>(a,
						new NamedBasicTypeImpl<>(new StringJavaType(), new VarcharJdbcType(), a))).collect(Collectors.toList());

		final var sessionFactory = (SessionFactoryImpl) emf.getNativeEntityManagerFactory();
		final var sqlFunction = (StandardSQLFunction) sessionFactory.getQueryEngine().getSqmFunctionRegistry().findFunctionDescriptor(name);
		sqlFunction.render(appender, astParams, translator);
		final var query = appender.toString();
		Assertions.assertEquals(nbQueryParam, StringUtils.countMatches(query, '?'));
		Assertions.assertTrue(query.contains(sql), query + "-- not contains --" + sql);

		sb.setLength(0);
		sqlFunction.render(appender, astParams, null, translator);
		Assertions.assertEquals(query, appender.toString());

		sb.setLength(0);
		sqlFunction.render(appender, astParams, null, null, translator);
		Assertions.assertEquals(query, appender.toString());

		sb.setLength(0);
		sqlFunction.render(appender, astParams, null, null, null, translator);
		Assertions.assertEquals(query, appender.toString());
		return query;
	}

	@Test
	void visibleProject() {
		assertFunction("visibleProject", 5, "_p__.team_leader=?user__", "_p__.team_leader", ALIAS, Q_USER);
	}

	@Test
	void visibleGroup() {
		assertFunction("visibleGroup", 4, "WHERE _arg__=s_d5.dn", ALIAS, Q_USER);
	}

	@Test
	void visibleCompany() {
		assertFunction("visibleCompany", 4, "WHERE _arg__=s_d3.dn", ALIAS, Q_USER);
	}

	@Test
	void writeDN() {
		assertFunction("writeDN", 3, "WHERE _arg__=s_d5.dn", ALIAS, Q_USER);
	}

	@Test
	void adminDN() {
		assertFunction("adminDN", 3, "_arg__=s_d5.dn OR _arg__ LIKE", ALIAS, Q_USER);
	}


	@Test
	void inProject2() {
		final var assertFunction = assertFunction("inProject", 4, "team_leader=?user__", Q_USER, Q_ARG);
		Assertions.assertTrue(assertFunction.contains("id=?dn__"));
		Assertions.assertTrue(assertFunction.contains("cm.\"user\"=?user__"));
		Assertions.assertTrue(assertFunction.contains("cpg.project=?dn__"));
	}

	@Test
	void inProjectKey() {
		final var assertFunction = assertFunction("inProjectKey", 4, "team_leader=?user__ AND pkey=?dn__", Q_USER,
				Q_ARG);
		Assertions.assertTrue(assertFunction.contains("cm.\"user\"=?user__ AND pj.pkey=?dn__"));
	}

	@Test
	void inGroup() {
		final var assertFunction = assertFunction("inGroup", 1, "cm.\"user\"=?user__", Q_USER, ALIAS);
		Assertions.assertTrue(assertFunction.contains("s_cg6.id=_arg__"));
		Assertions.assertTrue(assertFunction.contains("id=_arg__"));
	}

	@Test
	void inCompany() {
		final var assertFunction = assertFunction("inCompany", 1, "cu.id=?user__", Q_USER, ALIAS);
		Assertions.assertTrue(assertFunction.contains("s_cc7.id=_arg__"));
		Assertions.assertTrue(assertFunction.contains("id=_arg__"));
	}

	@Test
	void inGroup2() {
		final var assertFunction = assertFunction("inGroup2", 3, "cm.\"user\"=?user__ AND cg.id=?dn__", Q_USER, Q_ARG);
		Assertions.assertTrue(assertFunction.contains("id=?dn__"));
	}

	@Test
	void inCompany2() {
		final var assertFunction = assertFunction("inCompany2", 3, "cu.id=?user__", Q_USER, Q_ARG);
		Assertions.assertTrue(assertFunction.contains("cc.id=?dn__"));
		Assertions.assertTrue(assertFunction.contains("id=?dn__"));
	}

}
