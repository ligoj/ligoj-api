/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.subscription;

import org.ligoj.app.api.AbstractNodeVo;
import lombok.Getter;
import lombok.Setter;

/**
 * A subscribed node.
 */
@Getter
@Setter
public class SubscribedNodeVo extends AbstractNodeVo {

	/**
	 * SID, for Hazelcast
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Instance of tool proving the expected service.
	 */
	private String refined;
}
