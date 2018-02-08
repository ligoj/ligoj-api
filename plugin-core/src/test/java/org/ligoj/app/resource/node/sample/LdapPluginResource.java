package org.ligoj.app.resource.node.sample;

import org.springframework.stereotype.Component;

/**
 * LDAP resource.
 */
@Component
public class LdapPluginResource extends AbstractToolPluginResource {

	/**
	 * Plug-in key.
	 */
	public static final String URL = IdentityResource.SERVICE_URL + "/ldap";

	/**
	 * Plug-in key.
	 */
	public static final String KEY = URL.replace('/', ':').substring(1);

	/**
	 * Full URL like "ldap/myhost:389/"
	 */
	public static final String PARAMETER_URL = KEY + ":url";

	/**
	 * DN of administrative user that can fetch the repository
	 */
	public static final String PARAMETER_USER = KEY + ":user-dn";

	/**
	 * Referral option as "follow"
	 */
	public static final String PARAMETER_REFERRAL = KEY + ":referral";

	/**
	 * Password of administrative user
	 */
	public static final String PARAMETER_PASSWORD = KEY + ":password";

	/**
	 * Base DN where people, groups and companies are located
	 */
	public static final String PARAMETER_BASE_BN = KEY + ":base-dn";

	/**
	 * LDAP schema attribute name of login.
	 */
	public static final String PARAMETER_UID_ATTRIBUTE = KEY + ":uid-attribute";

	/**
	 * DN of location of users can login
	 */
	public static final String PARAMETER_PEOPLE_DN = KEY + ":people-dn";

	/**
	 * LDAP schema attribute name of department.
	 */
	public static final String PARAMETER_DEPARTMENT_ATTRIBUTE = KEY + ":department-attribute";
	/**
	 * LDAP schema attribute name of internal id of login. May not be unique.
	 */
	public static final String PARAMETER_LOCAL_ID_ATTRIBUTE = KEY + ":local-id-attribute";

	/**
	 * DN of location where isolated users are moved to.
	 */
	public static final String PARAMETER_QUARANTINE_DN = KEY + ":quarantine-dn";

	/**
	 * LDAP schema attribute holding the locked state of a user.
	 */
	public static final String PARAMETER_LOCKED_ATTRIBUTE = KEY + ":locked-attribute";

	/**
	 * Value used as flag for a locked user inside the locked attribute
	 */
	public static final String PARAMETER_LOCKED_VALUE = KEY + ":locked-value";

	/**
	 * Object Class of people : organizationalPerson, inetOrgPerson
	 */
	public static final String PARAMETER_PEOPLE_CLASS = KEY + ":people-class";

	/**
	 * Pattern capturing the company from the DN of the user. May be a row string for constant.
	 */
	public static final String PARAMETER_COMPANY_PATTERN = KEY + ":company-pattern";

	/**
	 * DN of location of groups
	 */
	public static final String PARAMETER_GROUPS_DN = KEY + ":groups-dn";

	/**
	 * DN of location of companies
	 */
	public static final String PARAMETER_COMPANIES_DN = KEY + ":companies-dn";

	/**
	 * DN of location of people considered as internal. May be the same than people
	 */
	public static final String PARAMETER_PEOPLE_INTERNAL_DN = KEY + ":people-internal-dn";

	@Override
	public String getKey() {
		return KEY;
	}

	@Override
	public void create(final int subscription) {
		// Nothing to do
	}
}
