/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.iam.model;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;

import org.ligoj.bootstrap.core.model.AbstractPersistable;

import lombok.Getter;
import lombok.Setter;

/**
 * User and group links : user to group, and group to subgroup.
 */
@Getter
@Setter
@Entity
@Table(name = "LIGOJ_CACHE_MEMBERSHIP", uniqueConstraints = @UniqueConstraint(columnNames = { "user", "sub_group",
		"group" }))
public class CacheMembership extends AbstractPersistable<Integer> {

	/**
	 * The member user. <code>null</code> when this membership is a group to subgroup link.
	 */
	@ManyToOne
	private CacheUser user;

	/**
	 * The member subgroup. <code>null</code> when this membership is a user to group link.
	 */
	@ManyToOne
	private CacheGroup subGroup;

	/**
	 * The parent group.
	 */
	@ManyToOne
	@NotNull
	private CacheGroup group;

}
