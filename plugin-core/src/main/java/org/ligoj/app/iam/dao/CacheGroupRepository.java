package org.ligoj.app.iam.dao;

import java.util.List;

import org.ligoj.app.iam.model.CacheGroup;
import org.ligoj.bootstrap.core.dao.RestRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

/**
 * {@link CacheGroup} repository
 */
public interface CacheGroupRepository extends RestRepository<CacheGroup, String>, CacheContainerRepository<CacheGroup> {

	/**
	 * Filter to determine the group is visible or not : brought by a delegate
	 * or one of the sub groups the current user is member.
	 */
	String VISIBLE_RESOURCE = "visiblegroup(l.description,:user,:user,:user,:user)=true";

	@Override
	@Query("FROM CacheGroup l WHERE (:criteria IS NULL OR (UPPER(id) LIKE UPPER(CONCAT(CONCAT('%',:criteria),'%')))) AND "
			+ VISIBLE_RESOURCE)
	Page<CacheGroup> findAll(String user, String criteria, Pageable page);

	@Override
	@Query("FROM CacheGroup l WHERE " + VISIBLE_RESOURCE)
	List<CacheGroup> findAll(String user);

	@Override
	@Query("FROM CacheGroup l WHERE id=:id AND " + VISIBLE_RESOURCE)
	CacheGroup findById(String user, String id);

}
