package org.ligoj.app.model.ldap;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Cache object of a company.<br>
 * "id" corresponds to the normalized name.<br>
 * "name" corresponds to the real name, not normalized.<br>
 * "description" corresponds to the normalized "Distinguished Name".
 */
@Entity
@Table(name = "SAAS_CACHE_COMPANY")
public class CacheCompany extends CacheContainer {

	/**
	 * SID
	 */
	private static final long serialVersionUID = 1L;

}
