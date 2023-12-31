/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.dao;

import org.hibernate.QueryException;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.query.ReturnableType;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.SortSpecification;
import org.hibernate.type.StandardBasicTypes;
import org.ligoj.bootstrap.core.dao.AfterJpaBeforeSpringDataListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Register the project native SQL functions for security. This "might" be hard to understand the Organization-RBAC implementation.
 * A human-readable form is available on GitHub wiki page.
 *
 * @see <a href="https://github.com/ligoj/ligoj/wiki/Security">Security</a>
 */
@Component
public class SecuritySpringDataListener implements AfterJpaBeforeSpringDataListener {

	private static final String USER_ARG = "$user";
	private static final String DN_ARG = "$arg";
	private static final String VISIBLE_GROUP = "$exists $memberR($arg,s_cmg0,$cm,$cg,$q(group),$q(user)) $end OR ";
	private static final String VISIBLE_COMPANY = "$exists $memberR($arg,s_cc0,$cu,$cc,company,id) $end OR ";
	private static final String VISIBLE_PROJECT = "$team_leader=$user OR " + VISIBLE_GROUP;
	private static final String DELEGATED = "$exists ($select_do(s_d1,USER)    AND s_d1.receiver=$user)               AS s_d1 WHERE $parent_dn(s_d1.dn,$arg) $end"
			+ " OR $exists ($select_do(s_d2,GROUP)   AND $exists $member(s_d2,s_cg1,$cm,$cg,$q(group),$q(user)) $end) AS s_d3 WHERE $parent_dn(s_d3.dn,$arg) $end"
			+ " OR $exists ($select_do(s_d4,COMPANY) AND $exists $member(s_d4,s_cc1,$cu,$cc,company,id) $end)         AS s_d5 WHERE $parent_dn(s_d5.dn,$arg) $end";

	private static final String IN_GROUP = "   $exists (SELECT cg.description AS dn, cg.id FROM $cm AS cm LEFT JOIN $cg AS cg ON (cg.id=cm.$q(group)) WHERE cm.$q(user)=$user) AS s_cg6 WHERE s_cg6.id=$arg OR $exists $cg WHERE id=$arg AND s_cg6.dn LIKE CONCAT('%,',description) $end $end";
	private static final String IN_COMPANY = " $exists (SELECT cc.description AS dn, cc.id FROM $cu AS cu LEFT JOIN $cc AS cc ON (cc.id=cu.company)   WHERE cu.id=$user)       AS s_cc7 WHERE s_cc7.id=$arg OR $exists $cc WHERE id=$arg AND s_cc7.dn LIKE CONCAT('%,',description) $end $end";
	private static final String IN_GROUP2 = "  $exists $cm AS cm LEFT JOIN $cg AS cg ON (cg.id=cm.$q(group)) WHERE cm.$q(user)=$user AND cg.id=$arg OR $exists $cg AS cg WHERE id=$arg AND cg.description LIKE CONCAT('%,',description) $end $end";
	private static final String IN_COMPANY2 = "$exists $cu AS cu LEFT JOIN $cc AS cc ON (cc.id=cu.company)   WHERE cu.id=$user       AND cc.id=$arg OR $exists $cc AS cc WHERE id=$arg AND cc.description LIKE CONCAT('%,',description) $end $end";
	private static final String IN_PKEY = "   $exists $cm AS cm LEFT JOIN $cg AS cg ON (cg.id=cm.$q(group)) LEFT JOIN $cpg AS cpg ON (cg.id=cpg.$q(group)) LEFT JOIN $pj AS pj ON (pj.id=cpg.project) WHERE cm.$q(user)=$user AND pj.pkey=$pkey $end";
	private static final String IN_PROJECT = "$exists $cm AS cm LEFT JOIN $cg AS cg ON (cg.id=cm.$q(group)) LEFT JOIN $cpg AS cpg ON (cg.id=cpg.$q(group)) WHERE cm.$q(user)=$user AND cpg.project=$project $end";
	private static final String IS_TEAM_LEADER_ID = "$exists $pj WHERE team_leader=$user AND id=$project $end OR ";
	private static final String IS_TEAM_LEADER_PK = "$exists $pj WHERE team_leader=$user AND pkey=$pkey $end OR ";

	private final Dialect dialect;

	private final LocalContainerEntityManagerFactoryBean emf;

	/**
	 * Listener with EMF as context.
	 *
	 * @param emf The current EMF.
	 */
	@Autowired
	public SecuritySpringDataListener(final LocalContainerEntityManagerFactoryBean emf) {
		this.emf = emf;
		final var sessionFactory = (SessionFactoryImpl) emf.getNativeEntityManagerFactory();
		this.dialect = sessionFactory.getJdbcServices().getJdbcEnvironment().getDialect();
	}

	@Override
	public void callback() {

		// Visible project : visible subscribed group of this project
		registerFunction(new DnFunction("visibleProject", List.of("$team_leader", DN_ARG, USER_ARG), VISIBLE_PROJECT + DELEGATED, null));

		// Visible group : member of this group or one of its subgroups or
		// delegate on this group or one of its subgroups
		registerFunction(new DnFunction("visibleGroup", List.of(DN_ARG, USER_ARG), VISIBLE_GROUP + DELEGATED, null));

		// Visible company : member of this company or one of its sub-companies
		// or delegate on this company or one of its sub-companies
		registerFunction(new DnFunction("visibleCompany", List.of(DN_ARG, USER_ARG), VISIBLE_COMPANY + DELEGATED, null));

		// Write DN : delegate with "can_write" flag on the related DN of one of its parent
		registerFunction(new DnFunction("writeDN", List.of(DN_ARG, USER_ARG), DELEGATED, "can_write"));

		// Admin DN : delegate with "can_admin" flag on the related DN of one of its parent
		registerFunction(new DnFunction("adminDN", List.of(DN_ARG, USER_ARG), DELEGATED, "can_admin"));

		// Member of a group : member of this group or one of its subgroups
		registerFunction(new DnFunction("inGroup", List.of(USER_ARG, DN_ARG), IN_GROUP, null));

		// Member of a group : member of this group or one of its subgroups
		registerFunction(new DnFunction("inGroup2", List.of(USER_ARG, DN_ARG), IN_GROUP2, null));

		// Member of a company : member of this company or one of its sub-company
		registerFunction(new DnFunction("inCompany", List.of(USER_ARG, DN_ARG), IN_COMPANY, null));

		// Member of a company : member of this company or one of its sub-company
		registerFunction(new DnFunction("inCompany2", List.of(USER_ARG, DN_ARG), IN_COMPANY2, null));

		// Member of a project : team leader or member of any group of this project
		registerFunction(
				new DnFunction("inProjectKey", List.of(USER_ARG, "$pkey"), IS_TEAM_LEADER_PK + IN_PKEY, null));

		// Member of a project : team leader or member of any group of this project identifier
		registerFunction(
				new DnFunction("inProject", List.of(USER_ARG, "$project"), IS_TEAM_LEADER_ID + IN_PROJECT, null));
	}

	private void registerFunction(final DnFunction sqlFunction) {
		final var sessionFactory = (SessionFactoryImpl) emf.getNativeEntityManagerFactory();
		sessionFactory.getQueryEngine().getSqmFunctionRegistry().register(sqlFunction.getName(), sqlFunction);
	}

	private class DnFunction extends StandardSQLFunction {

		private final List<String> args;
		private final String access;
		private final String query;

		/**
		 * Construct a standard SQL function definition with a static return type.
		 *
		 * @param name The name of the function.
		 */
		private DnFunction(final String name, final List<String> args,
				final String query, final String access) {
			super(name.toLowerCase(), StandardBasicTypes.BOOLEAN);
			this.args = args;
			this.access = access;
			this.query = query;
		}

		@Override
		public void render(
				SqlAppender sqlAppender,
				List<? extends SqlAstNode> sqlAstArguments,
				ReturnableType<?> returnType,
				SqlAstTranslator<?> walker) {
			if (sqlAstArguments.size() != args.size()) {
				throw new QueryException("The function requires " + args.size() + " arguments, but received " + sqlAstArguments.size(), query);
			}
			parse("(" + Objects.toString(query, "") + ")", sqlAppender, sqlAstArguments, walker);
		}

		@Override
		public void render(
				SqlAppender sqlAppender,
				List<? extends SqlAstNode> sqlAstArguments,
				Predicate filter,
				Boolean respectNulls,
				Boolean fromFirst,
				ReturnableType<?> returnType,
				SqlAstTranslator<?> walker) {
			// Ignore the filter by default. Subclasses will override this
			render(sqlAppender, sqlAstArguments, returnType, walker);
		}

		@Override
		public void render(
				SqlAppender sqlAppender,
				List<? extends SqlAstNode> sqlAstArguments,
				Predicate filter,
				ReturnableType<?> returnType,
				SqlAstTranslator<?> walker) {
			// Ignore the filter by default. Subclasses will override this
			render(sqlAppender, sqlAstArguments, returnType, walker);
		}

		@Override
		public void render(
				SqlAppender sqlAppender,
				List<? extends SqlAstNode> sqlAstArguments,
				Predicate filter,
				List<SortSpecification> withinGroup,
				ReturnableType<?> returnType,
				SqlAstTranslator<?> walker) {
			// Ignore the filter by default. Subclasses will override this
			render(sqlAppender, sqlAstArguments, returnType, walker);
		}

		private String member(final String parent, final String child) {
			return "(SELECT s$2.description AS dn FROM $3 AS f$2 LEFT JOIN $4 AS s$2 ON (s$2.id=f$2.$5) WHERE f$2.$6=\\$user) AS $2 WHERE \\$parent_dn("
					+ parent + "," + child + ")";
		}

		private String func(final String name, final int nbParam) {
			return "\\$" + name + "\\(" + "([^,)]+)".repeat(Math.min(nbParam, 1))
					+ ",([^,)]+)".repeat(Math.max(nbParam - 1, 0)) + "\\)";
		}

		private void parse(final String query, SqlAppender sqlAppender, List<? extends
				SqlAstNode> sqlAstArguments, final SqlAstTranslator<?> translator) {
			final var quote = dialect.openQuote() + "$1" + dialect.closeQuote();
			var remaining = query
					.replace("$exists", "(EXISTS (SELECT 1 FROM").replace("$end", "))")

					// Quote reserved words
					.replaceAll("\\$q\\(([^)]+)\\)", quote)

					.replaceAll(func("member", 6), member("$1.receiver_dn", "$2.dn"))
					.replaceAll(func("memberR", 6), member("$2.dn", "$1"))
					.replaceAll(func("parent_dn", 2), "$2=$1 OR $2 LIKE CONCAT('%,',$1)")
					.replaceAll(func("select_do", 2),
							"SELECT $1.dn, $1.receiver_dn FROM \\$do AS $1 WHERE $1.receiver_type='$2'"
									+ (access == null ? "" : (" AND $1." + access + " IS true")))

					// Table alias
					.replace("$pj", "ligoj_project")
					.replace("$cg", "ligoj_cache_group")
					.replace("$cu", "ligoj_cache_user")
					.replace("$cc", "ligoj_cache_company")
					.replace("$cg", "ligoj_cache_group")
					.replace("$cpg", "ligoj_cache_project_group")
					.replace("$cm", "ligoj_cache_membership")
					.replace("$do", "ligoj_delegate_org")

					// Quote reserved words
					.replaceAll("\\$q\\(([^)]+)\\)", quote);

			// Parameter alias
			do {
				var nextIndexInQuery = Integer.MAX_VALUE;
				var nextArgIndex = -1;
				for (int argIndex = 0; argIndex < args.size(); argIndex++) {
					var arg = args.get(argIndex);
					var indexInQuery = remaining.indexOf(arg);
					if (indexInQuery != -1 && indexInQuery < nextIndexInQuery) {
						nextIndexInQuery = indexInQuery;
						nextArgIndex = argIndex;
					}
				}
				if (nextArgIndex == -1) {
					// all replacements have been done
					break;
				}
				final var nextArg = args.get(nextArgIndex);
				sqlAppender.append(remaining.substring(0, nextIndexInQuery));
				translator.render(sqlAstArguments.get(nextArgIndex), SqlAstNodeRenderingMode.DEFAULT);
				remaining = remaining.substring(nextIndexInQuery + nextArg.length());
			} while (true);
			sqlAppender.append(remaining);
		}

	}
}
