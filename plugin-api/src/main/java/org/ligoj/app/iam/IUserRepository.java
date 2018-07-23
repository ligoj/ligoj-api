/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.iam;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.ligoj.bootstrap.core.resource.BusinessException;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;

/**
 * User repository
 */
public interface IUserRepository {

	/**
	 * Return the {@link UserOrg} corresponding to the given identifier without using cache.
	 *
	 * @param id
	 *            The user identifier.
	 * @return the found user or <code>null</code> when not found. Groups are not fetched for this operation.
	 */
	UserOrg findByIdNoCache(String id);

	/**
	 * Return the {@link UserOrg} corresponding to the given attribute/value without using cache for the query, but
	 * using it to resolve the user.
	 *
	 * @param attribute
	 *            the attribute name to match.
	 * @param value
	 *            the attribute value to match.
	 * @return the found user or <code>null</code> when not found. Groups are not fetched for this operation.
	 */
	default UserOrg findOneBy(final String attribute, final String value) {
		return findAllBy(attribute, value).stream().findFirst().orElse(null);
	}

	/**
	 * Return all {@link UserOrg} corresponding to the given attribute/value without using cache for the query, but
	 * using it to resolve the user. If the user is not found in the cache, the fresh data is used.
	 *
	 * @param attribute
	 *            the attribute name to match.
	 * @param value
	 *            the attribute value to match.
	 * @return the found users or empty list.
	 */
	List<UserOrg> findAllBy(String attribute, String value);

	/**
	 * Return all user entries. Cache manager is involved.
	 *
	 * @return all user entries. Key is the user identifier.
	 */
	Map<String, UserOrg> findAll();

	/**
	 * Return all user entries. Cache manager is not involved, poor performance is expected.
	 *
	 * @param groups
	 *            The available and resolved groups.
	 * @return all user entries. Key is the user identifier.
	 * @since 3.0.2
	 */
	default Map<String, UserOrg> findAllNoCache(Map<String, GroupOrg> groups) {
		return findAll();
	}

	/**
	 * Return the {@link UserOrg} corresponding to the given identifier using the user cache.
	 *
	 * @param id
	 *            the user identifier.
	 * @return the {@link UserOrg} corresponding to the given identifier. May be <code>null</code>.
	 */
	default UserOrg findById(final String id) {
		return findAll().get(id);
	}

	/**
	 * Return the {@link UserOrg} corresponding to the given identifier using the user cache.
	 *
	 * @param id
	 *            The user identifier.
	 * @return the {@link UserOrg} corresponding to the given identifier. Never <code>null</code>.
	 * @throws ValidationJsonException
	 *             If no user is found.
	 */
	default UserOrg findByIdExpected(final String id) {
		return Optional.ofNullable(findById(id))
				.orElseThrow(() -> new ValidationJsonException("id", "unknown-id", "0", "user", "1", id));
	}

	/**
	 * Return the {@link ICompanyRepository} to use to resolve the company of the managed users.
	 *
	 * @return the {@link ICompanyRepository} to use to resolve the company of the managed users.
	 */
	ICompanyRepository getCompanyRepository();

	/**
	 * Return the {@link UserOrg} corresponding to the given identifier using the user cache and the relevant security
	 * to check the current user has the rights to perform this request.
	 *
	 * @param principal
	 *            The user requesting this data.
	 * @param id
	 *            the user to find.
	 * @return the {@link UserOrg} corresponding to the given identifier. Never <code>null</code>.
	 * @throws ValidationJsonException
	 *             If no user is found.
	 */
	default UserOrg findByIdExpected(final String principal, final String id) {
		// Check the user exists
		final UserOrg rawUser = findByIdExpected(id);
		if (getCompanyRepository().findById(principal, rawUser.getCompany()) == null) {
			// No available delegation -> no result
			throw new ValidationJsonException("id", BusinessException.KEY_UNKNOW_ID, "0", "user", "1", principal);
		}
		return rawUser;
	}

	/**
	 * Return the users members (UIDs) of the given groups and matching to the given pattern.
	 *
	 * @param requiredGroups
	 *            Filtered groups to be member of returned users. The users must be member of one of these groups. When
	 *            <code>null</code>, there is no constraint.
	 * @param companies
	 *            Filtered companies (DNs) to be member of returned users.
	 * @param criteria
	 *            the optional criteria used to check identifier (UID), first name and last name.
	 * @param pageable
	 *            the ordering and page data.
	 * @return the UID of users matching all above criteria.
	 */
	Page<UserOrg> findAll(Collection<GroupOrg> requiredGroups, Set<String> companies, String criteria,
			Pageable pageable);

	/**
	 * Check the user credentials.
	 *
	 * @param name
	 *            the user's name.
	 * @param password
	 *            the user's password.
	 * @return <code>true</code> when credentials are correct.
	 */
	boolean authenticate(String name, String password);

	/**
	 * Return user token based on salted password.
	 *
	 * @param id
	 *            The user identifier.
	 * @return User token based on salted password or <code>null</code>.
	 */
	String getToken(String id);

	/**
	 * Reset user password to the given value. The given password is not stored inside the given {@link UserOrg}
	 * instance, but only in the remote storage, and in an hashed form.
	 *
	 * @param user
	 *            The user to update.
	 * @param password
	 *            The raw new password. Will be hashed.
	 */
	void setPassword(UserOrg user, String password);

	/**
	 * Reset user password to the given value. The given password is not stored inside the given {@link UserOrg}
	 * instance, but only in the remote storage, and in an hashed form. In case <b>password</b> is <code>null</code>, a
	 * new temporary password will be generated to guarantee the authentication before performing any change. Some LDAP
	 * modules such as PPOLICY requires a fully authenticated of the target user to apply the password policy.
	 *
	 * @param user
	 *            The user to update.
	 * @param password
	 *            The user current password.
	 * @param newPassword
	 *            The raw new password. Will be hashed.
	 */
	void setPassword(UserOrg user, @Nullable String password, String newPassword);

	/**
	 * Return a safe {@link UserOrg} instance, even if the user is not in LDAP directory.
	 *
	 * @param id
	 *            The user identifier. Must not be <code>null</code>.
	 * @return a not <code>null</code> {@link UserOrg} instance with at least identifier attribute.
	 */
	default UserOrg toUser(final String id) {
		if (id == null) {
			return null;
		}

		// Non null user name
		UserOrg result = findById(id);
		if (result == null) {
			result = new UserOrg();
			result.setId(id);
		}
		return result;
	}

	/**
	 * Base DN for internal people.
	 *
	 * @return Base DN for internal people.
	 */
	String getPeopleInternalBaseDn();

	/**
	 * Execute LDAP modifications for each change between entries. Cache is also updated.
	 *
	 * @param user
	 *            The user to update. The properties will be copied, this instance will not be the one stored
	 *            internally.
	 */
	void updateUser(UserOrg user);

	/**
	 * Move a user from his/her location to the target company. Cache is also updated, and the company of given user is
	 * replaced by the given company.
	 *
	 * @param user
	 *            The LDAP user to disable.
	 * @param company
	 *            The target company.
	 */
	void move(UserOrg user, CompanyOrg company);

	/**
	 * Restore a user from the isolate to the previous company of this user and unlock this user.
	 *
	 * @param user
	 *            The LDAP user to disable.
	 */
	void restore(UserOrg user);

	/**
	 * Unlock an user :
	 * <ul>
	 * <li>Check the user is not isolated</li>
	 * <li>Check the user is locked</li>
	 * <li>Clear the locked flag</li>
	 * </ul>
	 * Note the password stills as is. If this user was previously locked, the password stills cleared.<br>
	 * Depending on the final implementation, other attributes or changes may be added. Such as PPOLICY for LDAP when
	 * supported.
	 *
	 * @param user
	 *            The LDAP user to disable.
	 * @see #lock(String, UserOrg)
	 */
	void unlock(UserOrg user);

	/**
	 * Isolate an user to the quarantine zone :
	 * <ul>
	 * <li>Clear the password to prevent new authentication</li>
	 * <li>Set the disabled flag.</li>
	 * <li>Move the user to the quarantine zone, DN is also update.</li>
	 * <li>Set the previous company.</li>
	 * </ul>
	 *
	 * @param principal
	 *            User requesting the lock.
	 * @param user
	 *            The LDAP user to disable.
	 */
	void isolate(String principal, UserOrg user);

	/**
	 * Lock an user :
	 * <ul>
	 * <li>Clear the password to prevent new authentication</li>
	 * <li>Set the disabled flag.</li>
	 * </ul>
	 * Depending on the final implementation, other attributes or changes may be added. Such as PPOLICY for LDAP when
	 * supported.
	 *
	 * @param principal
	 *            User requesting the lock.
	 * @param user
	 *            The LDAP user to disable.
	 * @see #unlock(UserOrg)
	 */
	void lock(String principal, UserOrg user);

	/**
	 * Delete the given user.
	 *
	 * @param user
	 *            the LDAP user.
	 */
	void delete(UserOrg user);

	/**
	 * Update membership of given user.
	 *
	 * @param groups
	 *            the target groups CN, not normalized.
	 * @param user
	 *            the target user.
	 */
	void updateMembership(Collection<String> groups, UserOrg user);

	/**
	 * Create an entry.
	 *
	 * @param entry
	 *            User to add to LDAP.
	 * @return the formal parameter.
	 */
	UserOrg create(UserOrg entry);

	/**
	 * Rebuild the DN from the given user.
	 *
	 * @param newUser
	 *            The user source where the DN need to be computed.
	 * @return the DN from the given user. May be different from the {@link UserOrg#getDn()}
	 */
	String toDn(UserOrg newUser);

	/**
	 * Check and update the user lock status without using cache. This will check only
	 * <code>pwdAccountLockedTime</code>, PPolicy attribute.
	 *
	 * @param user
	 *            Target user to check.
	 */
	default void checkLockStatus(UserOrg user) {
		// By default not supported
	}
}
