/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.plugin;

import org.apache.commons.lang3.ObjectUtils;
import org.ligoj.bootstrap.core.curl.CurlProcessor;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Version utilities.
 */
@Component
public class VersionUtils {

	/**
	 * Valid version pattern.
	 */
	private static final Pattern VERSION_PATTERN = Pattern.compile("^\\d.*$");

	/**
	 * Return the lasted version for the given JIRA project.
	 *
	 * @param serverUrl The server base URL like "<a href="http://jira.codehaus.org">JIRA</a>"
	 * @param project   The JIRA project identifier.
	 * @return <code>null</code> or latest version
	 */
	public AtlassianVersion getLatestReleasedVersion(final String serverUrl, final String project) {
		// Get the download index
		try (final var processor = new CurlProcessor()) {
			final var versionsAsJson = ObjectUtils
					.getIfNull(processor.get(serverUrl + "/rest/api/2/project/" + project + "/versions"), "[]");
			final var versionsRaw = new ObjectMapper().readValue(versionsAsJson,
					new TypeReference<List<AtlassianVersion>>() {
						// Nothing to override
					});

			// Find the last download link
			AtlassianVersion lastVersion = null;
			for (final var jiraVersion : versionsRaw) {
				if (isValidVersion(lastVersion, jiraVersion)) {
					lastVersion = jiraVersion;
				}
			}

			// Return the last read version
			return lastVersion;
		}
	}

	/**
	 * Check the given version is valid and greater/newer than the last one.
	 *
	 * @param lastVersion The last validated version.
	 * @param jiraVersion The version to validate.
	 * @return <code>true</code> when the version is newer then the previous one.
	 */
	protected boolean isValidVersion(final AtlassianVersion lastVersion, final AtlassianVersion jiraVersion) {
		return jiraVersion.isReleased() && jiraVersion.getReleaseDate() != null && !jiraVersion.isArchived()
				&& isValidVersionName(lastVersion, jiraVersion);
	}

	private boolean isValidVersionName(final AtlassianVersion lastVersion, final AtlassianVersion jiraVersion) {
		return VERSION_PATTERN.matcher(jiraVersion.getName()).matches()
				&& (lastVersion == null || jiraVersion.getName().compareTo(lastVersion.getName()) > 0);
	}

	/**
	 * Return the lasted version name for the given Jira project.
	 *
	 * @param serverUrl The server base URL like "<a href="http://jira.codehaus.org">JIRA</a>"
	 * @param project   The JIRA project identifier.
	 * @return <code>null</code> or latest version name.
	 */
	public String getLatestReleasedVersionName(final String serverUrl, final String project) {
		final var version = getLatestReleasedVersion(serverUrl, project);
		if (version != null) {
			return version.getName();
		}
		return null;
	}
}
