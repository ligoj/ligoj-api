/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.domain.Persistable;

/**
 * Abstract entity having a string key. This fixed type for id is required for some JPQL queries and might be removed
 * with a full support of the genericity of Hibernate.
 */
@Getter
@Setter
@Entity
@ToString
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "name"), name = "LIGOJ_NODE")
public abstract class AbstractStringKeyEntity implements Persistable<String> {

	/**
	 * Business key.
	 */
	@Id
	@NotNull
	private String id;

	/**
	 * Returns if the {@code Persistable} is new or was persisted already.
	 *
	 * @return if {@literal true} the object is new.
	 */
	public boolean isNew() {
		return getId() == null;
	}

}
