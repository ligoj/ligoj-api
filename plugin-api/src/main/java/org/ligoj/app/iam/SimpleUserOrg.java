package org.ligoj.app.iam;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

/**
 * Organizational User
 */
@Getter
@Setter
public class SimpleUserOrg extends SimpleUser {

	/**
	 * SID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * User mail addresses.
	 */
	private List<String> mails;

	/**
	 * This user is secured : has at least an attached credential and not a clear default password.
	 */
	@JsonIgnore
	private boolean secured;

	/**
	 * When not <code>null</code> this user is locked and corresponds to the date since this account is this state.
	 */
	private Date locked;

	/**
	 * When not <code>null</code> this user is locked and corresponds to the author user login of this lock.
	 */
	private String lockedBy;

	/**
	 * When not <code>null</code> this user is in a quarantine zone and this value corresponds to the old normalized
	 * company name. This
	 * value is used to restore the user inside this company if it exists yet.
	 */
	private String isolated;

	/**
	 * Copy all non secured data.
	 * 
	 * @param to
	 *            the target object.
	 */
	public void copy(final SimpleUserOrg to) {
		super.copy(to);
		to.setMails(getMails());
		to.setLocked(getLocked());
		to.setLockedBy(getLockedBy());
		to.setIsolated(getIsolated());
		to.setSecured(false);
	}

}
