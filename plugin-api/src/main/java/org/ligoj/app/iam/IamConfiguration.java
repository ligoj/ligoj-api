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

}
