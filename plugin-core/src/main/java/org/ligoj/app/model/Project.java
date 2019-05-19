/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.model;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.SafeHtml;
import org.hibernate.validator.constraints.SafeHtml.Attribute;
import org.hibernate.validator.constraints.SafeHtml.Tag;
import org.ligoj.bootstrap.core.IDescribableBean;
import org.ligoj.bootstrap.core.model.AbstractNamedAuditedEntity;
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
public class Project extends AbstractNamedAuditedEntity<Integer> implements IDescribableBean<Integer> {

	/**
	 * SID, for Hazelcast
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Project PKEY pattern.
	 */
	public static final String PKEY_PATTERN = "([a-z]|[0-9]+-?[a-z])[a-z0-9\\-]*";

	/**
	 * Project PKEY pattern.
	 */
	public static final String PKEY_PATTERN_WRAPPER = "^" + PKEY_PATTERN +"$";

	/**
	 * Unique technical and yet readable name.
	 */
	@NotBlank
	@LowerCase
	@Size(max = 100)
	@Pattern(regexp = PKEY_PATTERN_WRAPPER)
	private String pkey;

	@OneToMany(mappedBy = "project", cascade = CascadeType.REMOVE)
	private List<Subscription> subscriptions;

	/**
	 * Human readable description.
	 */
	@Length(max = 1024)
	@SafeHtml(additionalTagsWithAttributes = @Tag(name = "a", attributesWithProtocols = @Attribute(name = "href", protocols = "#")))
	private String description;

	/**
	 * Team Leader user name
	 */
	private String teamLeader;

	@JsonIgnore
	@OneToMany(mappedBy = "project", cascade = CascadeType.REMOVE)
	private List<CacheProjectGroup> cacheGroups;

}
