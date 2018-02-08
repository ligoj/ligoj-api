package org.ligoj.app.resource.plugin;

import java.io.IOException;
import java.util.Date;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test class of {@link VersionUtils}
 */
public class VersionUtilsTest {

	@Test
	public void getLatestReleasedVersion() throws IOException {
		final AtlassianVersion releasedVersion = new VersionUtils().getLatestReleasedVersion("https://jira.atlassian.com", "JRA");
		Assertions.assertNotNull(releasedVersion);
		Assertions.assertNotNull(releasedVersion.getName());
		Assertions.assertNotNull(releasedVersion.getReleaseDate());
	}

	@Test
	public void isValidVersionFirst() {
		final AtlassianVersion jiraVersion = new AtlassianVersion();
		jiraVersion.setName("1.0");
		jiraVersion.setReleased(true);
		jiraVersion.setReleaseDate(new Date());
		Assertions.assertTrue(new VersionUtils().isValidVersion(null, jiraVersion));
	}

	@Test
	public void isValidVersionArchived() {
		final AtlassianVersion jiraVersion = new AtlassianVersion();
		jiraVersion.setArchived(true);
		jiraVersion.setName("1.0");
		jiraVersion.setReleased(true);
		jiraVersion.setReleaseDate(new Date());
		Assertions.assertFalse(new VersionUtils().isValidVersion(null, jiraVersion));
	}

	@Test
	public void isValidVersionUnreleased() {
		final AtlassianVersion jiraVersion = new AtlassianVersion();
		jiraVersion.setName("1.0");
		jiraVersion.setReleaseDate(new Date());
		Assertions.assertFalse(new VersionUtils().isValidVersion(null, jiraVersion));
	}

	@Test
	public void isValidVersionNoReleaseDate() {
		final AtlassianVersion jiraVersion = new AtlassianVersion();
		jiraVersion.setArchived(false);
		jiraVersion.setName("1.0");
		jiraVersion.setReleased(true);
		Assertions.assertFalse(new VersionUtils().isValidVersion(null, jiraVersion));
	}

	@Test
	public void isValidVersionNewer() {
		final AtlassianVersion previous = new AtlassianVersion();
		previous.setName("1.0");
		previous.setReleased(true);
		previous.setReleaseDate(new Date());

		final AtlassianVersion jiraVersion = new AtlassianVersion();
		jiraVersion.setName("1.1");
		jiraVersion.setReleased(true);
		jiraVersion.setReleaseDate(new Date());
		Assertions.assertTrue(new VersionUtils().isValidVersion(previous, jiraVersion));
	}

	@Test
	public void isValidVersionInvalidName() {
		final AtlassianVersion jiraVersion = new AtlassianVersion();
		jiraVersion.setArchived(false);
		jiraVersion.setName("OLD 1.0");
		jiraVersion.setReleased(true);
		jiraVersion.setReleaseDate(new Date());
		Assertions.assertFalse(new VersionUtils().isValidVersion(null, jiraVersion));
	}

	@Test
	public void isValidVersionOlder() {
		final AtlassianVersion previous = new AtlassianVersion();
		previous.setName("1.1");
		previous.setReleased(true);
		previous.setReleaseDate(new Date());

		final AtlassianVersion jiraVersion = new AtlassianVersion();
		jiraVersion.setName("1.0");
		jiraVersion.setReleased(true);
		jiraVersion.setReleaseDate(new Date());
		Assertions.assertFalse(new VersionUtils().isValidVersion(previous, jiraVersion));
	}

	@Test
	public void getLatestReleasedVersionName() throws Exception {
		final String releasedVersion = new VersionUtils().getLatestReleasedVersionName("https://jira.atlassian.com", "JRA");
		Assertions.assertNotNull(releasedVersion);
	}

	@Test
	public void getLatestReleasedVersionNameFailed() throws Exception {
		Assertions.assertNull(new VersionUtils().getLatestReleasedVersionName("any:some", "0"));
	}

}
