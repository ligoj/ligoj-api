/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.iam;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.ligoj.bootstrap.core.resource.BusinessException;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Container repository contract.
 *
 * @param <T>
 *            The container type.
 */
public interface IContainerRepository<T extends ContainerOrg> {
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
	 * @param principal
	 *            The user requesting this container.
	 * @param id
	 *            The container's identifier. Will be normalized.
	 * @return The container from its identifier. <code>null</code> if the container is not found or cannot be seen by
	 *         the given principal user.
	 */
	default T findById(String principal, String id) {
		return findById(id);
	}

	/**
	 * Find a container from its identifier. Security is applied regarding the given user.
	 *
	 * @param principal
	 *            The user requesting this container.
	 * @param id
	 *            The container's identifier. Will be normalized.
	 * @return The container from its identifier. Never <code>null</code>.
	 * @throws ValidationJsonException
	 *             If the container is not found or cannot be seen by the given user.
	 */
	default T findByIdExpected(String principal, String id) {
		// Check the container exists and return the in memory object.
		return Optional.ofNullable(findById(principal, id)).orElseThrow(
				() -> new ValidationJsonException(getTypeName(), BusinessException.KEY_UNKNOW_ID, "0", "id", "1", id));
	}

	/**
	 * Return all normalized containers where key is the identifier. Note the result uses cache, so does not reflect the
	 * current state of internal representation. Cache manager is involved.
	 *
	 * @return the whole set of containers. Key is the normalized identifier. Value is the corresponding LDAP container
	 *         containing real CN, DN and normalized UID members.
	 */
	Map<String, T> findAll();

	/**
	 * Return all normalized containers where key is the identifier. Note the result does not use cache, so reflect the
	 * current state of internal representation and implies a poor performance. Cache manager is not involved.
	 *
	 * @return the whole set of containers. Key is the normalized identifier. Value is the corresponding LDAP container
	 *         containing real CN, DN and normalized UID members.
	 *
	 * @since 3.0.2
	 */
	default Map<String, T> findAllNoCache() {
		return findAll();
	}

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
	 *
	 * @return the human readable container type name.
	 */
	String getTypeName();

	/**
	 * Return the groups matching to the given pattern.
	 *
	 * @param groups
	 *            the visible groups.
	 * @param criteria
	 *            the optional criteria used to check name (CN).
	 * @param pageable
	 *            the ordering and page data.
	 * @param customComparators
	 *            The custom comparators used to order the result. The key is the ordered property name.
	 * @return the UID of users matching all above criteria.
	 */
	Page<T> findAll(Set<T> groups, String criteria, Pageable pageable, Map<String, Comparator<T>> customComparators);
}
