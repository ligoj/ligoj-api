/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.plugin;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

/**
 * Test class of {@link VersionUtils}
 */
class VersionUtilsTest {

	@Test
	void getLatestReleasedVersion() {
		final var releasedVersion = new VersionUtils().getLatestReleasedVersion("https://jira.atlassian.com", "JRA");
		Assertions.assertNotNull(releasedVersion);
		Assertions.assertNotNull(releasedVersion.getName());
		Assertions.assertNotNull(releasedVersion.getReleaseDate());
	}

	private static AtlassianVersion newVersion(final String name, final String releaseDate) {
		final var version = new AtlassianVersion();
		version.setName(name);
		version.setReleased(true);
		version.setReleaseDate(Date.from(LocalDate.parse(releaseDate).atStartOfDay(ZoneOffset.UTC).toInstant()));
		return version;
	}

	/**
	 * Versions are compared by their numbers, not as text: "9.9.2" is older than "11.3.12" and "10.0".
	 */
	@Test
	void getLatestReleasedVersionNumeric() {
		final var versions = List.of(newVersion("9.9.2", "2026-01-10"), newVersion("11.3.12", "2026-10-05"),
				newVersion("10.0", "2026-03-01"), newVersion("9.12.40", "2026-10-06"));
		Assertions.assertEquals("11.3.12", new VersionUtils().getLatestReleasedVersion(versions).getName());
	}

	/**
	 * A version released more than a year before the last release is ignored, whatever its number: Jira published
	 * "1000.x" versions in 2017 with another numbering.
	 */
	@Test
	void getLatestReleasedVersionRecent() {
		final var versions = List.of(newVersion("1000.1112.0", "2017-07-10"), newVersion("11.3.12", "2026-10-05"),
				newVersion("9.12.40", "2026-10-06"), newVersion("10.7", "2025-10-06"));
		Assertions.assertEquals("11.3.12", new VersionUtils().getLatestReleasedVersion(versions).getName());
	}

	@Test
	void getLatestReleasedVersionNone() {
		final var archived = newVersion("1.0", "2026-10-05");
		archived.setArchived(true);
		Assertions.assertNull(new VersionUtils().getLatestReleasedVersion(List.of(archived)));
		Assertions.assertNull(new VersionUtils().getLatestReleasedVersion(List.of()));
	}

	@Test
	void isValidVersion() {
		Assertions.assertTrue(new VersionUtils().isValidVersion(newVersion("1.0", "2026-10-05")));
	}

	@Test
	void isValidVersionArchived() {
		final var jiraVersion = newVersion("1.0", "2026-10-05");
		jiraVersion.setArchived(true);
		Assertions.assertFalse(new VersionUtils().isValidVersion(jiraVersion));
	}

	@Test
	void isValidVersionUnreleased() {
		final var jiraVersion = newVersion("1.0", "2026-10-05");
		jiraVersion.setReleased(false);
		Assertions.assertFalse(new VersionUtils().isValidVersion(jiraVersion));
	}

	@Test
	void isValidVersionNoReleaseDate() {
		final var jiraVersion = newVersion("1.0", "2026-10-05");
		jiraVersion.setReleaseDate(null);
		Assertions.assertFalse(new VersionUtils().isValidVersion(jiraVersion));
	}

	/**
	 * Only dotted numbers name a release: not a pre-release, a product prefix, a date or a single number.
	 */
	@Test
	void isValidVersionInvalidName() {
		for (final var name : List.of("OLD 1.0", "11.4.0-EAP01", "6.0-OD-01", "sqs-2026.5",
				"2017-07-06-22-45-08_2017-07-06_6b55650941", "10")) {
			Assertions.assertFalse(new VersionUtils().isValidVersion(newVersion(name, "2026-10-05")), name);
		}
	}

	@Test
	void getLatestReleasedVersionName() {
		final var releasedVersion = new VersionUtils().getLatestReleasedVersionName("https://jira.atlassian.com",
				"JRA");
		Assertions.assertNotNull(releasedVersion);
	}

	@Test
	void getLatestReleasedVersionNameFailed() {
		Assertions.assertNull(new VersionUtils().getLatestReleasedVersionName("any:some", "0"));
	}

}
