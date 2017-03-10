package org.ligoj.app.resource.subscription;

import org.ligoj.bootstrap.core.NamedBean;
import lombok.Getter;
import lombok.Setter;

/**
 * Light project description base.
 */
@Getter
@Setter
public class SubscribingProjectVo extends NamedBean<Integer> {

	/**
	 * Unique technical and yet readable name.
	 */
	private String pkey;

}
