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

	private static final String MEMBER_GROUP = "   $exists (SELECT cg.description AS dn, cg.id FROM $cm AS cm LEFT JOIN $cg AS cg ON (cg.id=cm.$q(group)) WHERE cm.$q(user)=$user) AS s_cg6 WHERE s_cg6.id=$arg OR $exists $cg WHERE s_cg6.dn LIKE CONCAT('%,',description) $end $end";
	private static final String MEMBER_GROUP2 = "  $exists $cm AS cm LEFT JOIN $cg AS cg ON (cg.id=cm.$q(group)) WHERE cm.$q(user)=$user AND cg.id=$arg OR $exists $cg AS cg WHERE cg.description LIKE CONCAT('%,',description) $end $end";
	private static final String MEMBER_COMPANY = " $exists (SELECT cc.description AS dn, cc.id FROM $cu AS cu LEFT JOIN $cc AS cc ON (cc.id=cu.company)   WHERE cu.id=$user)       AS s_cc7 WHERE s_cc7.id=$arg OR $exists $cc WHERE s_cc7.dn LIKE CONCAT('%,',description) $end $end";
	private static final String MEMBER_COMPANY2 = "$exists $cu AS cu LEFT JOIN $cc AS cc ON (cc.id=cu.company)   WHERE cu.id=$user       AND cc.id=$arg OR $exists $cc AS cc WHERE cc.description LIKE CONCAT('%,',description) $end $end";
	private static final String MEMBER_PKEY = "    $exists (SELECT pj.pkey FROM $cm AS cm LEFT JOIN $cg AS cg ON (cg.id=cm.$q(group)) LEFT JOIN $cpg AS cpg ON (cg.id=cpg.$q(group)) LEFT JOIN $pj AS pj ON (pj.id=cpg.project) WHERE cm.$q(user)=$user) AS s_pj8 WHERE s_pj8.pkey=$arg $end";
	private static final String MEMBER_PKEY2 = "   $exists $cm AS cm LEFT JOIN $cg AS cg ON (cg.id=cm.$q(group)) LEFT JOIN $cpg AS cpg ON (cg.id=cpg.$q(group)) LEFT JOIN $pj AS pj ON (pj.id=cpg.project) WHERE cm.$q(user)=$user AND pj.pkey=$arg $end";
	private static final String MEMBER_PROJECT = " $exists (SELECT cpg.project AS id FROM $cm AS cm LEFT JOIN $cg AS cg ON (cg.id=cm.$q(group)) LEFT JOIN $cpg AS cpg ON (cg.id=cpg.$q(group)) WHERE cm.$q(user)=$user) AS s_pj9 WHERE s_pj9.id=$arg $end";
	private static final String MEMBER_PROJECT2 = "$exists $cm AS cm LEFT JOIN $cg AS cg ON (cg.id=cm.$q(group)) LEFT JOIN $cpg AS cpg ON (cg.id=cpg.$q(group)) WHERE cm.$q(user)=$user AND cpg.project=$arg $end";
	private static final String IS_TEAM_LEADER_ID = " $exists (SELECT id FROM $pj pj WHERE pj.team_leader=$user) AS pj10 WHERE pj10.id=$arg $end OR ";
	private static final String IS_TEAM_LEADER_ID2 = "$exists $pj pj WHERE pj.team_leader=$user AND pj.id=$arg $end OR ";
	private static final String IS_TEAM_LEADER_PK = " $exists (SELECT pkey FROM $pj pj WHERE pj.team_leader=$user) AS pj11 WHERE pj11.pkey=$arg $end OR ";
	private static final String IS_TEAM_LEADER_PK2 = "$exists $pj pj WHERE pj.team_leader=$user AND pj.pkey=$arg $end OR ";

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

		// Member of a group : member of this group or one of its sub-groups
		registerFunction(new DnFunction("ingroup2", 2, 0, MEMBER_GROUP2, null, null, (sql, args) -> sql));

		// Member of a company : member of this company or one of its sub-company
		registerFunction(new DnFunction("incompany", 2, 0, MEMBER_COMPANY, null, null, (sql, args) -> sql));

		// Member of a company : member of this company or one of its sub-company
		registerFunction(new DnFunction("incompany2", 2, 0, MEMBER_COMPANY2, null, null, (sql, args) -> sql));

		// Member of a project : team leader or member of any group of this project
		registerFunction(new DnFunction("inprojectkey", 3, 0, MEMBER_PKEY, null, IS_TEAM_LEADER_PK, (sql, args) -> sql));

		// Member of a project : team leader or member of any group of this project
		registerFunction(new DnFunction("inprojectkey2", 3, 1, MEMBER_PKEY2, null, IS_TEAM_LEADER_PK2, (sql, args) -> sql));

		// Member of a project : team leader or member of any group of this project
		registerFunction(new DnFunction("inproject", 3, 0, MEMBER_PROJECT, null, IS_TEAM_LEADER_ID, (sql, args) -> sql));

		// Member of a project : team leader or member of any group of this project
		registerFunction(new DnFunction("inproject2", 2, 0, MEMBER_PROJECT2, null, IS_TEAM_LEADER_ID2, (sql, args) -> sql));

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
			return "("
					+ callback.apply(parse(StringUtils.defaultString(filter, "") + StringUtils.defaultString(query, ""),
							(String) args.get(dnIndex), (String) args.get(userIndex)), args)
					+ ")";
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
