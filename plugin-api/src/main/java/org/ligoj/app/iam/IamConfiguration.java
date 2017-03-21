package org.ligoj.app.iam;

import lombok.Getter;
import lombok.Setter;
import net.sf.ehcache.pool.sizeof.annotations.IgnoreSizeOf;

/**
 * Identity and Access Management configuration.
 */
@Getter
@Setter
@IgnoreSizeOf
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
