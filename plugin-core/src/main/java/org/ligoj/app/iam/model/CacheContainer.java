package org.ligoj.app.iam.model;

import javax.persistence.MappedSuperclass;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Length;
import org.ligoj.bootstrap.core.IDescribableBean;
import org.ligoj.bootstrap.core.model.AbstractNamedBusinessEntity;

import lombok.Getter;
import lombok.Setter;

/**
 * Cache container.<br>
 * "id" corresponds to the normalized name.<br>
 * "name" corresponds to the real name, not normalized.<br>
 * "description" corresponds to the normalized "Distinguished Name".
 */
@MappedSuperclass
@Getter
@Setter
public class CacheContainer extends AbstractNamedBusinessEntity<String> implements IDescribableBean<String> {

	/**
	 * DN.
	 */
	@Length(max = 512)
	@NotNull
	@NotEmpty
	private String description;

}