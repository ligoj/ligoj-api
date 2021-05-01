/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.model;

import org.ligoj.app.iam.model.DelegateType;

import lombok.Getter;

/**
 * Container type.
 */
public enum ContainerType {
	/**
	 * Group container.
	 */
	GROUP(DelegateType.GROUP),

	/**
	 * Company container.
	 */
	COMPANY(DelegateType.COMPANY);

	/**
	 * Corresponding {@link DelegateType}
	 */
	@Getter
	private final DelegateType delegateType;

	ContainerType(final DelegateType delegateType) {
		this.delegateType = delegateType;
	}
}
