/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.iam;

import lombok.Getter;
import lombok.Setter;

import java.util.Collection;

/**
 * User memberships changes.
 */
@Getter
@Setter
public class UserUpdateResult {

	/**
	 * Groups added to user.
	 */
	Collection<String> addedGroups;

	/**
	 * Groups removed from user.
	 */
	Collection<String> removedGroups;
}
