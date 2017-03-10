package org.ligoj.app.resource.plugin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * BitBucket named value retrieved from REST API.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class BitBucketTag {
	private String name;
}
