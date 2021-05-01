/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.iam;

import lombok.Getter;
import lombok.Setter;

/**
 * Identity and Access Management configuration.
 */
@Getter
@Setter
public class IamConfiguration {

	/**
	 * User repository.
	 */
	private IUserRepository userRepository;

	/**
	 * Company repository.
	 */
	private ICompanyRepository companyRepository;

	/**
	 * Group repository.
	 */
	private IGroupRepository groupRepository;

	/**
	 * The optional related node identifier.
	 */
	private String node;

}
