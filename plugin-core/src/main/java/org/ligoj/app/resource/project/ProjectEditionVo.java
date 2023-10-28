/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

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
	@NotBlank
	@LowerCase
	@Size(max = 100)
	@Pattern(regexp = "^([a-z]|\\d+-?[a-z])[a-z\\d\\-]*$")
	private String pkey;

	/**
	 * UID of team leader.
	 */
	@NotBlank
	@LowerCase
	@Size(max = 100)
	@Pattern(regexp = SimpleUser.USER_PATTERN_WRAPPER)
	private String teamLeader;

}
