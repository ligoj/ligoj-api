/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.model;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.ligoj.app.iam.model.CacheGroup;
import org.ligoj.bootstrap.core.model.AbstractPersistable;

import lombok.Getter;
import lombok.Setter;

/**
 * Project {@link CacheGroup} association.
 */
@Getter
@Setter
@Entity
@Table(name = "LIGOJ_CACHE_PROJECT_GROUP")
public class CacheProjectGroup extends AbstractPersistable<Integer> implements Serializable {

	/**
	 * SID, for Hazelcast
	 */
	private static final long serialVersionUID = 1L;

	@ManyToOne
	private Project project;

	@ManyToOne
	private CacheGroup group;

}
