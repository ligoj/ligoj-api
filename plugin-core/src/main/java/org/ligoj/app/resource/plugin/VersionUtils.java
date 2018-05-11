/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.plugin;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Version utilities.
 */
@Component
public class VersionUtils {

	/**
	 * Valid version pattern.
	 */
	private static final Pattern VERSION_PATTERN = Pattern.compile("^[0-9]+.*$");

	/**
	 * Return the lasted version for the given JIRA project.
	 *
	 * @param serverUrl
	 *            The server base URL like "http://jira.codehaus.org"
	 * @param project
	 *            The JIRA project identifier.
	 * @return <code>null</code> or latest version
	 * @throws IOException
	 *             When version cannot be read from the remote URL.
	 */
	public AtlassianVersion getLatestReleasedVersion(final String serverUrl, final String project) throws IOException {
		// Get the download index
		try (final CurlProcessor processor = new CurlProcessor()) {
			final String versionsAsJson = ObjectUtils
					.defaultIfNull(processor.get(serverUrl + "/rest/api/2/project/" + project + "/versions"), "[]");
			final List<AtlassianVersion> versionsRaw = new ObjectMapper().readValue(versionsAsJson,
					new TypeReference<List<AtlassianVersion>>() {
						// Nothing to override
					});

			// Find the last download link
			AtlassianVersion lastVersion = null;
			for (final AtlassianVersion jiraVersion : versionsRaw) {
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
	 * @param lastVersion
	 *            The last validated version.
	 * @param jiraVersion
	 *            The version to validate.
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
	 * @param serverUrl
	 *            The server base URL like "http://jira.codehaus.org"
	 * @param project
	 *            The JIRA project identifier.
	 * @return <code>null</code> or latest version name.
	 * @throws IOException
	 *             When version cannot be read from the remote URL.
	 */
	public String getLatestReleasedVersionName(final String serverUrl, final String project) throws IOException {
		final AtlassianVersion version = getLatestReleasedVersion(serverUrl, project);
		if (version != null) {
			return version.getName();
		}
		return null;
	}
}
