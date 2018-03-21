/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.plugin;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * BitBucket values retrieved from REST API.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class BitBucketTags {
	private List<BitBucketTag> values;
}
