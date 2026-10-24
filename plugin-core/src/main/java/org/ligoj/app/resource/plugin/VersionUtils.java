/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.plugin;

import org.apache.commons.lang3.ObjectUtils;
import org.ligoj.bootstrap.core.curl.CurlProcessor;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;
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
	private static final Pattern VERSION_PATTERN = Pattern.compile("^\\d+(\\.\\d+)+$");

	/**
	 * Period before the last release where the other releases are compared.
	 */
	private static final Duration RECENT = Duration.ofDays(365);

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

			return getLatestReleasedVersion(versionsRaw);
		}
	}

	/**
	 * Return the valid version having the highest number, among the ones released within a year of the last release:
	 * an older one may follow another numbering, as the "1000.x" versions Jira published in 2017.
	 *
	 * @param versions The versions of a project.
	 * @return <code>null</code> or latest version. Versions are compared by their numbers: "10.0" is newer than "9.9".
	 */
	protected AtlassianVersion getLatestReleasedVersion(final List<AtlassianVersion> versions) {
		final var valid = versions.stream().filter(this::isValidVersion).toList();
		return valid.stream().map(AtlassianVersion::getReleaseDate).max(Comparator.naturalOrder())
				.map(last -> last.toInstant().minus(RECENT))
				.flatMap(from -> valid.stream().filter(v -> !v.getReleaseDate().toInstant().isBefore(from))
						.max(Comparator.comparing(v -> toNumbers(v.getName()), Arrays::compare)))
				.orElse(null);
	}

	/**
	 * Return the numbers of a dotted version.
	 */
	private static long[] toNumbers(final String version) {
		return Arrays.stream(version.split("\\.")).mapToLong(Long::parseLong).toArray();
	}

	/**
	 * Check the given version is a release.
	 *
	 * @param jiraVersion The version to validate.
	 * @return <code>true</code> when the version is released, not archived, has a release date and is named with
	 *         dotted numbers only: not a pre-release such as "11.4.0-EAP01".
	 */
	protected boolean isValidVersion(final AtlassianVersion jiraVersion) {
		return jiraVersion.isReleased() && jiraVersion.getReleaseDate() != null && !jiraVersion.isArchived()
				&& VERSION_PATTERN.matcher(jiraVersion.getName()).matches();
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
