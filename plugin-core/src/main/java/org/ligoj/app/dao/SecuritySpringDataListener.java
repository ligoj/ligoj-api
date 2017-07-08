package org.ligoj.app.dao;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.QueryException;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;
import org.ligoj.bootstrap.core.dao.AfterJpaBeforeSpringDataListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.security.util.FieldUtils;
import org.springframework.stereotype.Component;

/**
 * Register the project native SQL functions for security.
 */
@Component
public class SecuritySpringDataListener implements AfterJpaBeforeSpringDataListener {

	@Autowired
	private ApplicationContext applicationContext;

	@Override
	public void callback() {
		registerFunction("visibleproject", new VisibleProjectFunction());
		registerFunction("visiblegroup", new VisibleGroupFunction());
		registerFunction("writegroup", new WritableGroupFunction());
		registerFunction("admingroup", new AdminGroupFunction());
	}

	private void registerFunction(final String name, final SQLFunction sqlFunction) {
		final SessionFactoryImpl emf = (SessionFactoryImpl) applicationContext
				.getBean(LocalContainerEntityManagerFactoryBean.class).getNativeEntityManagerFactory();
		@SuppressWarnings("unchecked")
		final Map<String, SQLFunction> sqlFunctions = (Map<String, SQLFunction>) FieldUtils
				.getProtectedFieldValue("functionMap", emf.getSqlFunctionRegistry());
		sqlFunctions.put(name, sqlFunction);
		emf.getJdbcServices().getJdbcEnvironment().getDialect().getFunctions().put(name, sqlFunction);

	}

	public class WritableGroupFunction extends AbstractVisibleFunction {
		@Override
		public String render(final Type type, @SuppressWarnings("rawtypes") final List args,
				SessionFactoryImplementor arg2) throws QueryException {
			if (args.size() != 4) {
				throw new IllegalArgumentException(
						"The function must be passed 5 arguments : group dn alias, and 3 parameters for user parameter");
			}
			String groupDn = (String) args.get(0);
			return parse(delegatedGroup(), groupDn, "can_write");
		}
	}

	public class AdminGroupFunction extends AbstractVisibleFunction {
		@Override
		public String render(final Type type, @SuppressWarnings("rawtypes") final List args,
				SessionFactoryImplementor arg2) throws QueryException {
			if (args.size() != 4) {
				throw new IllegalArgumentException(
						"The function must be passed 5 arguments : group dn alias, and 3 parameters for user parameter");
			}
			String groupDn = (String) args.get(0);
			return parse(delegatedGroup(), groupDn, "can_admin");
		}
	}

	public class VisibleGroupFunction extends AbstractVisibleFunction {
		@Override
		public String render(final Type type, @SuppressWarnings("rawtypes") final List args,
				SessionFactoryImplementor arg2) throws QueryException {
			if (args.size() != 5) {
				throw new IllegalArgumentException(
						"The function must be passed 6 arguments : group dn alias, and 4 parameters for user parameter");
			}
			String groupDn = (String) args.get(0);
			return parse(visibleGroup(), groupDn, null);
		}
	}

	public class VisibleProjectFunction extends AbstractVisibleFunction {
		@Override
		public String render(final Type type, @SuppressWarnings("rawtypes") final List args,
				SessionFactoryImplementor arg2) throws QueryException {
			if (args.size() != 7) {
				throw new IllegalArgumentException(
						"The function must be passed 7 arguments : project group dn alias, and 5 parameters for user parameter");
			}
			final String project = (String) args.get(0);
			final String groupDn = (String) args.get(1);
			return parse("$project.team_leader=$user OR " + visibleGroup(), groupDn, null).replace("$project",
					StringUtils.removeEnd(project, ".id"));
		}
	}

	public abstract class AbstractVisibleFunction implements SQLFunction {

		@Override
		public Type getReturnType(final Type type, final Mapping arg1) throws QueryException {
			return StandardBasicTypes.BOOLEAN;
		}

		@Override
		public boolean hasArguments() {
			return true;
		}

		@Override
		public boolean hasParenthesesIfNoArguments() {
			return true;
		}

		protected String delegatedGroup() {
			return "       $exists ($select_dn(_s_d1,USER)  AND _s_d1.receiver=$user) AS _s_d WHERE $match_dn($dn,_s_d.dn) $end"
					+ " OR $exists ($select_dn(_s_d2,GROUP) AND $exists $member(_s_cmg1, _s_cm1,_s_d2.dn) $end) AS _s_d3 WHERE $match_dn($dn,_s_d3.dn) $end"
					+ " OR $exists ($select_dn(_s_d4,COMPANY)"
					+ " 	AND $exists (SELECT _s_cc1.$desc  AS dn FROM $cu AS _s_cu LEFT JOIN $cc AS _s_cc1 ON (_s_cc1.id=_s_cu.company) WHERE _s_cu.id=$user) AS _s_cc2"
					+ " 	 WHERE $match_dn(_s_cc2.dn,_s_d4.dn)))) AS _s_d5 WHERE $match_dn($dn,_s_d5.dn) $end";
		}

		protected String visibleGroup() {
			return "$exists $member(_s_cmg0, _s_cm0,$dn) $end OR " + delegatedGroup();
		}

		protected String parse(final String query, final String dn, final String access) {
			return query
					.replaceAll("\\$member\\(([^,]+),([^)]+),([^)]+)\\)",
							"(SELECT $1.\\$desc AS dn FROM \\$cm AS $2 LEFT JOIN \\$cg AS $1 ON ($1.id=$2.`group`) WHERE $2.`user`=\\$user) AS $1 WHERE \\$member_dn($1.dn, $3)")
					.replaceAll("\\$member_dn\\(([^,]+),([^)]+)\\)",
							"$1=$2 OR $1 LIKE CONCAT('%,',$2) OR $2 LIKE CONCAT('%,',$1)")
					.replaceAll("\\$match_dn\\(([^,]+),([^)]+)\\)",
							"$1=$2 OR $1 LIKE CONCAT('%,',$2)" + (access == null ? " OR $2 LIKE CONCAT('%,',$1)" : ""))
					.replaceAll("\\$select_dn\\(([^,]+),([^)]+)\\)",
							"SELECT $1.dn FROM \\$do AS $1 WHERE $1.receiver_type='$2'"
									+ (access == null ? "" : (" AND $1." + access + "=true")))
					.replace("$exists", "(EXISTS (SELECT 1 FROM").replace("$end", "))").replace("$desc", "description")
					.replace("$do", "ligoj_delegate_org").replace("$cg", "ligoj_cache_group")
					.replace("$cu", "ligoj_cache_user").replace("$cc", "ligoj_cache_company")
					.replace("$cg", "ligoj_cache_group").replace("$cm", "ligoj_cache_membership").replace("$dn", dn)
					.replace("$user", "?");
		}
	}
}
