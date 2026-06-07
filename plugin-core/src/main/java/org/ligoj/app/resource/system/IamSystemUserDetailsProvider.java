/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.system;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.ligoj.app.iam.dao.CacheUserRepository;
import org.ligoj.app.iam.model.CacheUser;
import org.ligoj.bootstrap.model.system.SystemUser;
import org.ligoj.bootstrap.resource.system.user.ISystemUserDetailsProvider;
import org.ligoj.bootstrap.resource.system.user.SystemUserVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * Extends the system user lookup (<code>GET /system/user/roles</code>, bootstrap's <code>UserResource</code>) with
 * the IAM cache attributes ({@link CacheUser}) sharing the same identifier: first name, last name and mails are
 * searchable and returned. Bootstrap cannot see the IAM entities (dependency direction), hence this provider plugged
 * through the {@link ISystemUserDetailsProvider} extension point. The lookup is fully paginated by the database.
 */
@Component
public class IamSystemUserDetailsProvider implements ISystemUserDetailsProvider {

	@Autowired
	private CacheUserRepository repository;

	@Override
	public Page<SystemUser> findAll(final String criteria, final Pageable page) {
		return repository.findAllSystemUsersByDetails(criteria, page);
	}

	@Override
	public void decorate(final Collection<SystemUserVo> users) {
		final var byLogin = users.stream().collect(Collectors.toMap(SystemUserVo::getLogin, Function.identity()));
		if (!byLogin.isEmpty()) {
			repository.findAllById(byLogin.keySet()).forEach(cacheUser -> {
				final var vo = byLogin.get(cacheUser.getId());
				vo.setFirstName(cacheUser.getFirstName());
				vo.setLastName(cacheUser.getLastName());
				vo.setMails(Arrays.stream(StringUtils.split(StringUtils.defaultString(cacheUser.getMails()), ','))
						.map(String::trim).filter(s -> !s.isEmpty()).toList());
			});
		}
	}
}
