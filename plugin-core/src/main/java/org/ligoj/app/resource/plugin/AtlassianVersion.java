/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.plugin;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * Atlassian product version retrieved from REST API.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AtlassianVersion {
	private String name;
	private boolean released;
	private boolean archived;
	private Date releaseDate;
}
