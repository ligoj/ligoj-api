/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.iam;

/**
 * A provider able to provide a configuration for Identity and Access Management.
 */
@FunctionalInterface
public interface IamConfigurationProvider {

	/**
	 * Return a {@link IamConfiguration} from the given node.
	 *
	 * @param node The node to use for an IAM configuration.
	 * @return a {@link IamConfiguration} from the given node.
	 */
	IamConfiguration getConfiguration(String node);

}
