package org.ligoj.app.iam;

import java.util.Map;

import org.ligoj.app.api.ContainerLdap;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;

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
	 * Find a container from its identifier. Security is applied regarding the given user.
	 * 
	 * @param user
	 *            The user requesting this container.
	 * @param id
	 *            The container's identifier. Will be normalized.
	 * @return The container from its identifier. <code>null</code> if the container is not found or cannot be seen by
	 *         the given user.
	 */
	default T findById(String user, String id) {
		return findById(id);
	}

	/**
	 * Find a container from its identifier. Security is applied regarding the given user.
	 * 
	 * @param user
	 *            The user requesting this container.
	 * @param id
	 *            The container's identifier. Will be normalized.
	 * @return The container from its identifier. Never <code>null</code>.
	 * @throws ValidationJsonException
	 *             If the container is not found or cannot be seen by the given user.
	 */
	T findByIdExpected(String user, String id);

	/**
	 * Return all normalized containers where key is the identifier. Note the result use cache, so does not reflect the
	 * current state of internal representation.
	 * Cache manager is involved.
	 * 
	 * @return the whole set of containers. Key is the normalized identifier. Value is the corresponding LDAP container
	 *         containing real CN, DN and
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

	/**
	 * Return the human readable container type name.
	 * @return the human readable container type name.
	 */
	String getTypeName();

}
