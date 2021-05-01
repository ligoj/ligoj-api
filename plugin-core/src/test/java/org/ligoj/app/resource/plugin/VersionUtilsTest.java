/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.plugin;

import java.io.IOException;
import java.util.Date;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test class of {@link VersionUtils}
 */
class VersionUtilsTest {

	@Test
	void getLatestReleasedVersion() throws IOException {
		final var releasedVersion = new VersionUtils().getLatestReleasedVersion("https://jira.atlassian.com", "JRA");
		Assertions.assertNotNull(releasedVersion);
		Assertions.assertNotNull(releasedVersion.getName());
		Assertions.assertNotNull(releasedVersion.getReleaseDate());
	}

	@Test
	void isValidVersionFirst() {
		final var jiraVersion = new AtlassianVersion();
		jiraVersion.setName("1.0");
		jiraVersion.setReleased(true);
		jiraVersion.setReleaseDate(new Date());
		Assertions.assertTrue(new VersionUtils().isValidVersion(null, jiraVersion));
	}

	@Test
	void isValidVersionArchived() {
		final var jiraVersion = new AtlassianVersion();
		jiraVersion.setArchived(true);
		jiraVersion.setName("1.0");
		jiraVersion.setReleased(true);
		jiraVersion.setReleaseDate(new Date());
		Assertions.assertFalse(new VersionUtils().isValidVersion(null, jiraVersion));
	}

	@Test
	void isValidVersionUnreleased() {
		final var jiraVersion = new AtlassianVersion();
		jiraVersion.setName("1.0");
		jiraVersion.setReleaseDate(new Date());
		Assertions.assertFalse(new VersionUtils().isValidVersion(null, jiraVersion));
	}

	@Test
	void isValidVersionNoReleaseDate() {
		final var jiraVersion = new AtlassianVersion();
		jiraVersion.setArchived(false);
		jiraVersion.setName("1.0");
		jiraVersion.setReleased(true);
		Assertions.assertFalse(new VersionUtils().isValidVersion(null, jiraVersion));
	}

	@Test
	void isValidVersionNewer() {
		final var previous = new AtlassianVersion();
		previous.setName("1.0");
		previous.setReleased(true);
		previous.setReleaseDate(new Date());

		final var jiraVersion = new AtlassianVersion();
		jiraVersion.setName("1.1");
		jiraVersion.setReleased(true);
		jiraVersion.setReleaseDate(new Date());
		Assertions.assertTrue(new VersionUtils().isValidVersion(previous, jiraVersion));
	}

	@Test
	void isValidVersionInvalidName() {
		final var jiraVersion = new AtlassianVersion();
		jiraVersion.setArchived(false);
		jiraVersion.setName("OLD 1.0");
		jiraVersion.setReleased(true);
		jiraVersion.setReleaseDate(new Date());
		Assertions.assertFalse(new VersionUtils().isValidVersion(null, jiraVersion));
	}

	@Test
	void isValidVersionOlder() {
		final var previous = new AtlassianVersion();
		previous.setName("1.1");
		previous.setReleased(true);
		previous.setReleaseDate(new Date());

		final var jiraVersion = new AtlassianVersion();
		jiraVersion.setName("1.0");
		jiraVersion.setReleased(true);
		jiraVersion.setReleaseDate(new Date());
		Assertions.assertFalse(new VersionUtils().isValidVersion(previous, jiraVersion));
	}

	@Test
	void getLatestReleasedVersionName() throws Exception {
		final var releasedVersion = new VersionUtils().getLatestReleasedVersionName("https://jira.atlassian.com",
				"JRA");
		Assertions.assertNotNull(releasedVersion);
	}

	@Test
	void getLatestReleasedVersionNameFailed() throws Exception {
		Assertions.assertNull(new VersionUtils().getLatestReleasedVersionName("any:some", "0"));
	}

}
