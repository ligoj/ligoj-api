/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.iam.dao;

import org.ligoj.app.iam.model.CacheMembership;
import org.ligoj.bootstrap.core.dao.RestRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

/**
 * {@link CacheMembership} repository
 */
@SuppressWarnings("ALL")
public interface CacheMembershipRepository extends RestRepository<CacheMembership, Integer> {

	// All delegated


	/**
	 * Return the direct group memberships of the given users.
	 *
	 * @param users The user identifiers.
	 * @return The pairs of user identifier and group identifier.
	 */
	@Query("SELECT m.user.id, m.group.id FROM CacheMembership m WHERE m.user.id IN :users")
	List<Object[]> findAllGroupsByUsers(Collection<String> users);
}
