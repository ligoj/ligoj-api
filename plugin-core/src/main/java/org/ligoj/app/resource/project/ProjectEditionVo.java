/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.project;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.ligoj.app.iam.SimpleUser;
import org.ligoj.app.iam.UserOrg;
import org.ligoj.bootstrap.core.DescribedAuditedBean;
import org.ligoj.bootstrap.core.validation.LowerCase;

import lombok.Getter;
import lombok.Setter;

/**
 * A fully described project.
 */
@Getter
@Setter
public class ProjectEditionVo extends DescribedAuditedBean<UserOrg, Integer> {

	/**
	 * SID, for Hazelcast
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Unique technical and yet readable name.
	 */
	@NotNull
	@NotBlank
	@LowerCase
	@Size(max = 100)
	@Pattern(regexp = "^([a-z]|[0-9]+-?[a-z])[a-z0-9\\-]*$")
	private String pkey;

	/**
	 * UID of team leader.
	 */
	@NotNull
	@NotBlank
	@LowerCase
	@Size(max = 100)
	@Pattern(regexp = SimpleUser.USER_PATTERN_WRAPPER)
	private String teamLeader;

}
