package org.ligoj.app.dao;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.QueryException;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;
import org.ligoj.bootstrap.core.dao.AfterJpaBeforeSpringDataListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.security.util.FieldUtils;
import org.springframework.stereotype.Component;

import lombok.Getter;

/**
 * Register the project native SQL functions for security.
 */
@Component
public class SecuritySpringDataListener implements AfterJpaBeforeSpringDataListener {
	private static final String VISIBLE_GROUP = "$exists $memberR($arg,s_cmg0,$cm,$cg,$q(group),$q(user)) $end OR ";
	private static final String VISIBLE_COMPANY = "$exists $memberR($arg,s_cc0,$cu,$cc,company,id) $end OR ";
	private static final String IS_TEAM_LEADER = "$project.team_leader=$user OR ";
	private static final String VISIBLE_PROJECT = IS_TEAM_LEADER + VISIBLE_GROUP;

	private static final String MEMBER_GROUP = "  $exists (SELECT f_cg6.description AS receiver_dn FROM $cg AS f_cg6 WHERE f_cg6.id=$arg) AS cg_dn WHERE $exists $member(cg_dn,s_cg6,$cm,$cg,$q(group),$q(user)) $end $end";
	private static final String MEMBER_COMPANY = "$exists (SELECT f_cc7.description AS receiver_dn FROM $cc AS f_cc7 WHERE f_cc7.id=$arg) AS cc_dn WHERE $exists $member(cc_dn,s_cc7,$cu,$cc,company,id) $end $end";
	private static final String MEMBER_PKEY = "   $exists (SELECT f_cg8.description AS receiver_dn, f_pr8.team_leader FROM $pj AS f_pr8 LEFT OUTER JOIN $cpg AS f_cpg8 ON (f_cpg8.project=f_pr8.id) LEFT JOIN $cg AS f_cg8 ON (f_cg8.id=f_cpg8.$q(group)) WHERE f_pr8.pkey=$arg) AS cg_dn"
			+ " WHERE cg_dn.team_leader=$user OR $exists $member(cg_dn,s_pr8,$cm,$cg,$q(group),$q(user)) $end $end";
	private static final String MEMBER_PROJECT = "$exists (SELECT f_cg9.description AS receiver_dn FROM $cpg AS f_cpg9 LEFT JOIN $cg AS f_cg9 ON (f_cg9.id=f_cpg9.$q(group)) WHERE f_cpg9.project=$arg) AS cg_dn"
			+ " WHERE $exists $member(cg_dn,s_pr9,$cm,$cg,$q(group),$q(user)) $end $end";

	@Getter
	private final Map<String, SQLFunction> sqlFunctions;
	private final Dialect dialect;

	@SuppressWarnings("unchecked")
	@Autowired
	public SecuritySpringDataListener(final LocalContainerEntityManagerFactoryBean emf) {
		final SessionFactoryImpl sessionFactory = (SessionFactoryImpl) emf.getNativeEntityManagerFactory();
		this.sqlFunctions = (Map<String, SQLFunction>) FieldUtils.getProtectedFieldValue("functionMap",
				sessionFactory.getSqlFunctionRegistry());
		this.dialect = sessionFactory.getJdbcServices().getJdbcEnvironment().getDialect();
	}

	@Override
	public void callback() {

		// Visible project : visible subscribed group of this project
		registerFunction(new DnFunction("visibleproject", 7, 1, null, VISIBLE_PROJECT,
				(sql, args) -> sql.replace("$project", StringUtils.removeEnd((String) args.get(0), ".id"))));

		// Visible group : member of this group or one of its sub-groups or
		// delegate on this group or one of its sub-groups
		registerFunction(new DnFunction("visiblegroup", 5, 0, null, VISIBLE_GROUP, (sql, args) -> sql));

		// Member of a group : member of this group or one of its sub-groups
		registerFunction(new DnFunction("ingroup", 2, 0, MEMBER_GROUP, null, null, (sql, args) -> sql));

		// Member of a company : member of this company or one of its sub-company
		registerFunction(new DnFunction("incompany", 2, 0, MEMBER_COMPANY, null, null, (sql, args) -> sql));

		// Member of a project : team leader or member of any group of this project
		registerFunction(new DnFunction("inprojectkey", 3, 0, MEMBER_PKEY, null, null, (sql, args) -> sql));

		// Member of a project : team leader or member of any group of this project
		registerFunction(new DnFunction("inproject", 3, 0, MEMBER_PROJECT, null, IS_TEAM_LEADER,
				(sql, args) -> sql.replace("$project", StringUtils.removeEnd((String) args.get(0), ".id"))));

		// Visible company : member of this company or one of its sub-companies
		// or delegate on this company or one of its sub-companies
		registerFunction(new DnFunction("visiblecompany", 5, 0, null, VISIBLE_COMPANY, (sql, args) -> sql));

		// Write DN : delegate with "can_write" flag on the related DN of one of
		// its parent
		registerFunction(new DnFunction("writedn", 4, 0, "can_write", null, (sql, args) -> sql));

		// Admin DN : delegate with "can_admin" flag on the related DN of one of
		// its parent
		registerFunction(new DnFunction("admindn", 4, 0, "can_admin", null, (sql, args) -> sql));
	}

	private void registerFunction(final StandardSQLFunction sqlFunction) {
		dialect.getFunctions().put(sqlFunction.getName(), sqlFunction);
		sqlFunctions.put(sqlFunction.getName(), sqlFunction);
	}

	private class DnFunction extends StandardSQLFunction {

		private static final String DELEGATED = "$exists ($select_do(s_d1,USER)    AND s_d1.receiver=$user)               AS s_d1 WHERE $parent_dn(s_d1.dn,$arg) $end"
				+ " OR $exists ($select_do(s_d2,GROUP)   AND $exists $member(s_d2,s_cg1,$cm,$cg,$q(group),$q(user)) $end) AS s_d3 WHERE $parent_dn(s_d3.dn,$arg) $end"
				+ " OR $exists ($select_do(s_d4,COMPANY) AND $exists $member(s_d4,s_cc1,$cu,$cc,company,id) $end)         AS s_d5 WHERE $parent_dn(s_d5.dn,$arg) $end";
		private int nbArgs;
		private int dnIndex;
		private int userIndex;
		private String access;
		private String filter;
		private String query;
		private BiFunction<String, List<?>, String> callback;

		/**
		 * Construct a standard SQL function definition with a static return type.
		 *
		 * @param name
		 *            The name of the function.
		 */
		private DnFunction(final String name, final int nbArgs, final int dnIndex, final String access,
				final String filter, final BiFunction<String, List<?>, String> callback) {
			this(name, nbArgs, dnIndex, DELEGATED, access, filter, callback);
		}

		/**
		 * Construct a standard SQL function definition with a static return type.
		 *
		 * @param name
		 *            The name of the function.
		 */
		private DnFunction(final String name, final int nbArgs, final int dnIndex, final String query,
				final String access, final String filter, final BiFunction<String, List<?>, String> callback) {
			super(name, StandardBasicTypes.BOOLEAN);
			this.nbArgs = nbArgs;
			this.filter = filter;
			this.callback = callback;
			this.access = access;
			this.dnIndex = dnIndex;
			this.userIndex = dnIndex + 1;
			this.query = query;
		}

		@Override
		public String render(final Type type, final List args, SessionFactoryImplementor arg2) throws QueryException {
			if (args.size() != nbArgs) {
				throw new IllegalArgumentException("The function must be passed " + nbArgs + " arguments");
			}
			return "(" + callback.apply(parse(StringUtils.defaultString(filter, "") + StringUtils.defaultString(query, ""), (String) args.get(dnIndex),
					(String) args.get(userIndex)), args) + ")";
		}

		private String member(final String parent, final String child) {
			return "(SELECT s$2.description AS dn FROM $3 AS f$2 LEFT JOIN $4 AS s$2 ON (s$2.id=f$2.$5) WHERE f$2.$6=\\$user) AS $2 WHERE \\$parent_dn("
					+ parent + "," + child + ")";
		}

		private String func(final String name, final int nbParam) {
			return "\\$" + name + "\\(" + StringUtils.repeat("([^,)]+)", Math.min(nbParam, 1))
					+ StringUtils.repeat(",([^,)]+)", Math.max(nbParam - 1, 0)) + "\\)";
		}

		private String parse(final String query, final String arg, final String user) {
			final Dialect dialect = SecuritySpringDataListener.this.dialect;
			final String quote = String.valueOf(dialect.openQuote()) + "$1" + dialect.closeQuote();
			return query.replace("$exists", "(EXISTS (SELECT 1 FROM").replace("$end", "))")
					.replace("$pj", "ligoj_project").replace("$cg", "ligoj_cache_group")
					.replace("$cu", "ligoj_cache_user").replace("$cc", "ligoj_cache_company")
					.replace("$cg", "ligoj_cache_group").replace("$cpg", "ligoj_cache_project_group")
					.replace("$cm", "ligoj_cache_membership").replace("$arg", arg)
					.replaceAll("\\$q\\(([^)]+)\\)", quote)
					.replaceAll(func("member", 6), member("$1.receiver_dn", "$2.dn"))
					.replaceAll(func("memberR", 6), member("$2.dn", "$1"))
					.replaceAll(func("parent_dn", 2), "$2=$1 OR $2 LIKE CONCAT('%,',$1)")
					.replaceAll(func("select_do", 2),
							"SELECT $1.dn, $1.receiver_dn FROM \\$do AS $1 WHERE $1.receiver_type='$2'"
									+ (access == null ? "" : (" AND $1." + access + " IS true")))
					.replace("$do", "ligoj_delegate_org").replace("$user", user).replaceAll("\\$q\\(([^)]+)\\)", quote);
		}

	}
}
