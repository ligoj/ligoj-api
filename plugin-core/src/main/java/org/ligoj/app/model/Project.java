package org.ligoj.app.model;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.ligoj.bootstrap.core.model.AbstractDescribedAuditedEntity;
import org.ligoj.bootstrap.core.validation.LowerCase;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

/**
 * A managed project.
 */
@Getter
@Setter
@Entity
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = "name"),
		@UniqueConstraint(columnNames = "pkey") }, name = "LIGOJ_PROJECT")
public class Project extends AbstractDescribedAuditedEntity<Integer> {

	/**
	 * Project PKEY pattern.
	 */
	public static final String PKEY_PATTERN = "^([a-z]|[0-9]+-?[a-z])[a-z0-9\\-]*$";

	/**
	 * Unique technical and yet readable name.
	 */
	@NotNull
	@NotBlank
	@LowerCase
	@Size(max = 100)
	@Pattern(regexp = PKEY_PATTERN)
	private String pkey;

	@OneToMany(mappedBy = "project", cascade = CascadeType.REMOVE)
	private List<Subscription> subscriptions;

	/**
	 * Team Leader user name
	 */
	private String teamLeader;

	@JsonIgnore
	@OneToMany(mappedBy = "project", cascade = CascadeType.REMOVE)
	private List<CacheProjectGroup> cacheGroups;

}
