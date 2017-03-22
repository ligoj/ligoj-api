package org.ligoj.app.model;

import org.ligoj.app.iam.model.DelegateType;

import lombok.Getter;

/**
 * Container type.
 */
public enum ContainerType {
	GROUP(DelegateType.GROUP), COMPANY(DelegateType.COMPANY);

	/**
	 * Corresponding {@link DelegateType}
	 */
	@Getter
	private final DelegateType delegateType;

	ContainerType(final DelegateType delegateType) {
		this.delegateType = delegateType;
	}
}
