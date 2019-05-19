/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.project;

import javax.persistence.Column;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.SafeHtml;
import org.ligoj.app.iam.SimpleUserOrg;
import org.ligoj.bootstrap.core.IDescribableBean;
import org.ligoj.bootstrap.core.NamedAuditedBean;
import org.ligoj.bootstrap.core.validation.LowerCase;

import lombok.Getter;
import lombok.Setter;

/**
 * Project description base.
 */
@Getter
@Setter
public class BasicProjectVo extends NamedAuditedBean<SimpleUserOrg, Integer> implements IDescribableBean<Integer> {

	/**
	 * SID, for Hazelcast
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * team leader
	 */
	@NotNull
	private SimpleUserOrg teamLeader;

	/**
	 * Unique technical and yet readable name.
	 */
	@NotBlank
	@Column(updatable = false)
	@LowerCase
	@Size(max = 100)
	@Pattern(regexp = "^[a-z0-9\\-]+$")
	private String pkey;

	@Length(max = 1024)
	@SafeHtml
	private String description;

}
