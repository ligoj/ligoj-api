package org.ligoj.app.iam;

import java.util.Map;

import org.ligoj.app.api.ContainerLdap;

/**
 * Container LDAP contract.
 */
public interface ContainerLdapRepository<T extends ContainerLdap> {

	/**
	 * Return the container corresponding to the given identifier using the user cache.
	 * 
	 * @param id
	 *            The container's identifier. Case is sensitive. Corresponds to the normalized container's name.
	 * @return The container corresponding to the given identifier. May be <code>null</code>
	 */
	default T findById(final String id) {
		return findAll().get(id);
	}

	/**
	 * Return all normalized containers where key is the identifier. Note the result use cache, so does not reflect the current state of LDAP.
	 * LDAP. Cache manager is involved.
	 * 
	 * @return the whole set of containers. Key is the normalized identifier. Value is the corresponding LDAP container containing real CN, DN and
	 *         normalized UID members.
	 */
	Map<String, T> findAll();

	/**
	 * Create a new container. There is no synchronized block, so error could occur; this is assumed for performance
	 * purpose.
	 * 
	 * @param dn
	 *            The DN of new Group. Must ends with the CN.
	 * @param cn
	 *            The formatted CN.
	 * @return The created container. This corresponds to the internal instance stored in cache.
	 */
	T create(String dn, String cn);

	/**
	 * Delete the given container. Cascaded deletion and cache are managed.
	 * 
	 * @param container
	 *            The container to delete.
	 */
	void delete(T container);

}
