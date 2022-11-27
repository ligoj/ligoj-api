/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.iam.model;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

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

	@ManyToOne
	private CacheUser user;

	@ManyToOne
	private CacheGroup subGroup;

	@ManyToOne
	@NotNull
	private CacheGroup group;

}
