package org.ligoj.app.iam;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ligoj.app.api.GroupLdap;
import org.ligoj.app.api.UserLdap;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * User repository
 */
public interface IUserRepository {

	/**
	 * Return the {@link UserLdap} corresponding to the given login without using cache.
	 * 
	 * @param login
	 *            the user login.
	 * @return the found user or <code>null</code> when not found. Groups are not fetched for this operation.
	 */
	UserLdap findByIdNoCache(String login);

	/**
	 * Return the {@link UserLdap} corresponding to the given attribute/value without using cache for the query, but
	 * using it to resolve the user.
	 * 
	 * @param attribute
	 *            the attribute name to match.
	 * @param value
	 *            the attribute value to match.
	 * @return the found user or <code>null</code> when not found. Groups are not fetched for this operation.
	 */
	default UserLdap findOneBy(final String attribute, final String value) {
		return findAllBy(attribute, value).stream().findFirst().orElse(null);
	}

	/**
	 * Return all {@link UserLdap} corresponding to the given attribute/value without using cache for the query, but
	 * using it to resolve the user. If the user is not found in the cache, the fresh data is used.
	 * 
	 * @param attribute
	 *            the attribute name to match.
	 * @param value
	 *            the attribute value to match.
	 * @return the found users or empty list.
	 */
	List<UserLdap> findAllBy(String attribute, String value);

	/**
	 * Return all user entries. Cache manager is involved.
	 * 
	 * @return all user entries. Key is the user login.
	 */
	Map<String, UserLdap> findAll();

	/**
	 * Return the {@link UserLdap} corresponding to the given login using the user cache.
	 * 
	 * @param login
	 *            the user login.
	 * @return the {@link UserLdap} corresponding to the given login. May be <code>null</code>.
	 */
	default UserLdap findById(final String login) {
		return findAll().get(login);
	}

	/**
	 * Return the {@link UserLdap} corresponding to the given login using the user cache.
	 * 
	 * @param id
	 *            the user login.
	 * @return the {@link UserLdap} corresponding to the given login. Never <code>null</code>.
	 * @throws ValidationJsonException
	 *             If no user is found.
	 */
	default UserLdap findByIdExpected(final String id) {
		final UserLdap userLdap = findById(id);
		if (userLdap == null) {
			throw new ValidationJsonException("id", "unknown-id", "0", "user", "1", id);
		}
		return userLdap;
	}

	/**
	 * Return the {@link UserLdap} corresponding to the given login using the user cache and the relevant security to
	 * check the current user has the rights to perform this request.
	 * 
	 * @param user
	 *            the user requesting this data.
	 * @param id
	 *            the user to find.
	 * @return the {@link UserLdap} corresponding to the given login. Never <code>null</code>.
	 * @throws ValidationJsonException
	 *             If no user is found.
	 */
	default UserLdap findByIdExpected(final String user, final String id) {
		// By default, no security applies
		return findByIdExpected(id);
	}

	/**
	 * Return the users members (UIDs) of the given groups and matching to the given pattern.
	 * 
	 * @param requiredGroups
	 *            Filtered groups to be member of returned users. If is <code>null</code>, there is no constraint. The
	 *            users must be member of one of these groups.
	 * @param companies
	 *            Filtered companies (DNs) to be member of returned users.
	 * @param criteria
	 *            the optional criteria used to check login (UID), first name and last name.
	 * @param pageable
	 *            the ordering and page data.
	 * @return the UID of users matching all above criteria.
	 */
	Page<UserLdap> findAll(Collection<GroupLdap> requiredGroups, Set<String> companies, String criteria, Pageable pageable);

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
	 * @param login
	 *            The user login.
	 * @return User token based on salted password.
	 */
	String getToken(String login);

	/**
	 * Reset user password to the given value. The given password is not stored inside the given {@link UserLdap}
	 * instance, but only in the remote storage, and in an hashed form.
	 * 
	 * @param userLdap
	 *            The user to update.
	 * @param password
	 *            The raw new password. Will be hashed.
	 */
	void setPassword(UserLdap userLdap, String password);

	/**
	 * Return a safe {@link UserLdap} instance, even if the user is not in LDAP directory.
	 * 
	 * @param login
	 *            the user login. Must not be <code>null</code>.
	 * @return a not <code>null</code> {@link UserLdap} instance with at least login attribute.
	 */
	default UserLdap toUser(final String login) {
		if (login == null) {
			return null;
		}

		// Non null user name
		UserLdap result = findById(login);
		if (result == null) {
			result = new UserLdap();
			result.setId(login);
		}
		return result;
	}

}
