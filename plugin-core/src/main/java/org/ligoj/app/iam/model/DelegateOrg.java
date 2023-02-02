/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.iam.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Length;
import org.ligoj.app.validation.DistinguishName;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

/**
 * An organizational right delegation. Grants to a user the right to create, delete and update the
 * entries/membership/tree of corresponding DN with included base DN.<br>
 * "name" attribute corresponds to the normalized form of corresponding CN. For "tree" delegate type, the name is equals
 * to the <code>-</code>, but should be ignored
 */
@Getter
@Setter
@Entity
@Table(name = "LIGOJ_DELEGATE_ORG")
public class DelegateOrg extends AbstractDelegate {

	/**
	 * SID, for Hazelcast
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The DN associated to this entry, including base DN to be able to perform SQL filters.
	 */
	@NotBlank
	@Length(max = 512)
	@DistinguishName
	private String dn;

	/**
	 * The resource DN receiving the delegation.
	 */
	@Length(max = 512)
	@DistinguishName
	@JsonIgnore
	private String receiverDn;

	/**
	 * The delegate type.
	 */
	@NotNull
	@Enumerated(EnumType.STRING)
	private DelegateType type;

}
