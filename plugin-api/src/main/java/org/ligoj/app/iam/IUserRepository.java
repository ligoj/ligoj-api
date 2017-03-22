package org.ligoj.app.iam;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.ligoj.app.api.GroupOrg;
import org.ligoj.app.api.UserOrg;
import org.ligoj.bootstrap.core.resource.BusinessException;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * User repository
 */
public interface IUserRepository {

	/**
	 * Return the {@link UserOrg} corresponding to the given login without using cache.
	 * 
	 * @param login
	 *            the user login.
	 * @return the found user or <code>null</code> when not found. Groups are not fetched for this operation.
	 */
	UserOrg findByIdNoCache(String login);

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
	 * @return all user entries. Key is the user login.
	 */
	Map<String, UserOrg> findAll();

	/**
	 * Return the {@link UserOrg} corresponding to the given login using the user cache.
	 * 
	 * @param login
	 *            the user login.
	 * @return the {@link UserOrg} corresponding to the given login. May be <code>null</code>.
	 */
	default UserOrg findById(final String login) {
		return findAll().get(login);
	}

	/**
	 * Return the {@link UserOrg} corresponding to the given login using the user cache.
	 * 
	 * @param id
	 *            the user login.
	 * @return the {@link UserOrg} corresponding to the given login. Never <code>null</code>.
	 * @throws ValidationJsonException
	 *             If no user is found.
	 */
	default UserOrg findByIdExpected(final String id) {
		return Optional.ofNullable(findById(id)).orElseThrow(() -> new ValidationJsonException("id", "unknown-id", "0", "user", "1", id));
	}

	/**
	 * Return the {@link ICompanyRepository} to use to resolve the company of the managed users.
	 * 
	 * @return the {@link ICompanyRepository} to use to resolve the company of the managed users.
	 */
	ICompanyRepository getCompanyRepository();

	/**
	 * Return the {@link UserOrg} corresponding to the given login using the user cache and the relevant security to
	 * check the current user has the rights to perform this request.
	 * 
	 * @param user
	 *            the user requesting this data.
	 * @param id
	 *            the user to find.
	 * @return the {@link UserOrg} corresponding to the given login. Never <code>null</code>.
	 * @throws ValidationJsonException
	 *             If no user is found.
	 */
	default UserOrg findByIdExpected(final String user, final String id) {
		// Check the user exists
		final UserOrg rawUserLdap = findByIdExpected(id);
		if (getCompanyRepository().findById(user, rawUserLdap.getCompany()) == null) {
			// No available delegation -> no result
			throw new ValidationJsonException("id", BusinessException.KEY_UNKNOW_ID, "0", "user", "1", user);
		}
		return rawUserLdap;
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
	Page<UserOrg> findAll(Collection<GroupOrg> requiredGroups, Set<String> companies, String criteria, Pageable pageable);

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
	 * Reset user password to the given value. The given password is not stored inside the given {@link UserOrg}
	 * instance, but only in the remote storage, and in an hashed form.
	 * 
	 * @param userLdap
	 *            The user to update.
	 * @param password
	 *            The raw new password. Will be hashed.
	 */
	void setPassword(UserOrg userLdap, String password);

	/**
	 * Return a safe {@link UserOrg} instance, even if the user is not in LDAP directory.
	 * 
	 * @param login
	 *            the user login. Must not be <code>null</code>.
	 * @return a not <code>null</code> {@link UserOrg} instance with at least login attribute.
	 */
	default UserOrg toUser(final String login) {
		if (login == null) {
			return null;
		}

		// Non null user name
		UserOrg result = findById(login);
		if (result == null) {
			result = new UserOrg();
			result.setId(login);
		}
		return result;
	}

}
