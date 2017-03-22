package org.ligoj.app.iam.model;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;
import org.ligoj.bootstrap.core.model.AbstractPersistable;

/**
 * User and group links : user to group, and group to sub-group.
 */
@Entity
@Table(name = "SAAS_CACHE_MEMBERSHIP")
@Getter
@Setter
public class CacheMembership extends AbstractPersistable<Integer> {

	/**
	 * SID
	 */
	private static final long serialVersionUID = 1L;

	@ManyToOne
	private CacheUser user;

	@ManyToOne
	private CacheGroup subGroup;

	@ManyToOne
	@NotNull
	private CacheGroup group;

}
