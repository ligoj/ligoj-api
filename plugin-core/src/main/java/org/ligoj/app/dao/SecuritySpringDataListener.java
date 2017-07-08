package org.ligoj.app.dao;

import java.util.List;
import java.util.Map;

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
		final SessionFactoryImpl emf = (SessionFactoryImpl) applicationContext
				.getBean(LocalContainerEntityManagerFactoryBean.class).getNativeEntityManagerFactory();
		@SuppressWarnings("unchecked")
		final Map<String, SQLFunction> sqlFunctions = (Map<String, SQLFunction>) FieldUtils
				.getProtectedFieldValue("functionMap", emf.getSqlFunctionRegistry());
		final SQLFunction sqlFunction = new VisibleProjectFunction();
		sqlFunctions.put("visibleproject", sqlFunction);
		emf.getJdbcServices().getJdbcEnvironment().getDialect().getFunctions().put("visibleproject", sqlFunction);

	}

	public class VisibleProjectFunction implements SQLFunction {
		@Override
		public String render(Type arg0, @SuppressWarnings("rawtypes") List args, SessionFactoryImplementor arg2)
				throws QueryException {
			if (args.size() != 7) {
				throw new IllegalArgumentException("The function must be passed 3 arguments : teamLeader alias, project group dn alias, and 5 parameters for user parameter");
			}
			String teamLeader = (String) args.get(0);
			String dn = (String) args.get(1);
			return ("$team=$user"
					+ " OR $exists $member(_s_cmg0, _s_cm0,$dn) $end"
					+ " OR $exists ($select_dn(_s_d1,USER) AND _s_d1.receiver=$user) AS _s_d WHERE $match($dn,_s_d.dn) $end"
					+ " OR $exists ($select_dn(_s_d2,GROUP) AND $exists $member(_s_cmg1, _s_cm1,_s_d2.dn) $end) AS _s_d3 WHERE $match($dn,_s_d3.dn) $end"
					+ " OR $exists ($select_dn(_s_d4,COMPANY)"
					+ " 	AND $exists (SELECT _s_cc1.$desc  AS dn FROM $cu AS _s_cu LEFT JOIN $cc AS _s_cc1 ON (_s_cc1.id=_s_cu.company) WHERE _s_cu.id=$user) AS _s_cc2"
					+ " 	 WHERE $match(_s_cc2.dn,_s_d4.dn)))) AS _s_d5"
					+ "  WHERE $match($dn,_s_d5.dn) $end")
							.replaceAll("\\$member\\(([^,]+),([^)]+),([^)]+)\\)",
									"(SELECT $1.\\$desc AS dn FROM \\$cm AS $2 LEFT JOIN \\$cg AS $1 ON ($1.id=$2.`group`) WHERE $2.`user`=\\$user) AS $1 WHERE \\$match($1.dn, $3)")
							.replaceAll("\\$match\\(([^,]+),([^)]+)\\)",
									"$1=$2 OR $1 LIKE CONCAT('%,',$2) OR $2 LIKE CONCAT('%,',$1)")
							.replaceAll("\\$select_dn\\(([^,]+),([^)]+)\\)", "SELECT $1.dn FROM \\$do AS $1 WHERE $1.receiver_type='$2'")
							.replace("$exists", "(EXISTS (SELECT 1 FROM")
							.replace("$end", "))")
							.replace("$desc", "description")
							.replace("$do", "ligoj_delegate_org")
							.replace("$cg", "ligoj_cache_group")
							.replace("$cu", "ligoj_cache_user")
							.replace("$cc", "ligoj_cache_company")
							.replace("$cg", "ligoj_cache_group")
							.replace("$cm", "ligoj_cache_membership")
							.replace("$dn", dn)
							.replace("$user", "?")
							.replace("$team", teamLeader);
		}

		@Override
		public Type getReturnType(Type arg0, Mapping arg1) throws QueryException {
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
	}
}
