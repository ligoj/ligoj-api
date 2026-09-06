/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.system;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.ligoj.app.iam.dao.CacheMembershipRepository;
import org.ligoj.app.iam.dao.CacheUserRepository;
import org.ligoj.app.iam.model.CacheUser;
import org.ligoj.bootstrap.core.NamedBean;
import org.ligoj.bootstrap.dao.system.SystemRoleRepository;
import org.ligoj.bootstrap.model.system.SystemRole;
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

	@Autowired
	private CacheMembershipRepository membershipRepository;

	@Autowired
	private SystemRoleRepository roleRepository;

	@Override
	public Page<SystemUser> findAll(final String criteria, final Pageable page) {
		return repository.findAllSystemUsersByDetails(criteria, page);
	}

	@Override
	public void decorate(final Collection<SystemUserVo> users) {
		final var byLogin = users.stream().collect(Collectors.toMap(SystemUserVo::getLogin, Function.identity()));
		if (byLogin.isEmpty()) {
			return;
		}
		repository.findAllById(byLogin.keySet()).forEach(cacheUser -> {
			final var vo = byLogin.get(cacheUser.getId());
			vo.setFirstName(cacheUser.getFirstName());
			vo.setLastName(cacheUser.getLastName());
			vo.setMails(Arrays.stream(StringUtils.split(StringUtils.defaultString(cacheUser.getMails()), ','))
					.map(String::trim).filter(s -> !s.isEmpty()).toList());
		});

		// Federated roles: the system roles named after a group of the user, as granted at login by the identity
		// provider (see UserOrgResource#getGrantedAuthorities: the group identifier, upper-cased or lower-cased)
		final var rolesByName = roleRepository.findAll().stream()
				.collect(Collectors.toMap(SystemRole::getName, Function.identity(), (a, _) -> a));
		final var groupsByUser = new HashMap<String, Set<String>>();
		membershipRepository.findAllGroupsByUsers(byLogin.keySet()).forEach(pair -> groupsByUser
				.computeIfAbsent((String) pair[0], _ -> new TreeSet<>()).add((String) pair[1]));
		byLogin.forEach((login, vo) -> vo
				.setFederatedRoles(toFederatedRoles(groupsByUser.getOrDefault(login, Set.of()), rolesByName)));
	}

	/**
	 * The system roles a member of the given groups obtains at login: a group grants the role named exactly like it,
	 * like its upper-case form or like its lower-case form.
	 *
	 * @param groups      The group identifiers of the user.
	 * @param rolesByName The system roles by name.
	 * @return The federated roles: the identifier is the granting group, the name is the system role name.
	 */
	static List<NamedBean<String>> toFederatedRoles(final Collection<String> groups,
			final Map<String, SystemRole> rolesByName) {
		final var result = new ArrayList<NamedBean<String>>();
		final var seen = new HashSet<String>();
		for (final var group : groups) {
			for (final var candidate : List.of(group, group.toUpperCase(), group.toLowerCase())) {
				if (rolesByName.containsKey(candidate) && seen.add(candidate)) {
					result.add(new NamedBean<>(group, candidate));
				}
			}
		}
		return result;
	}
}
