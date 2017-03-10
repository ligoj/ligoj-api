package org.ligoj.app.resource.plugin;

import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test class of {@link VersionUtils}
 */
public class VersionUtilsTest {

	@Test
	public void getLatestReleasedVersion() throws Exception {
		final AtlassianVersion releasedVersion = new VersionUtils().getLatestReleasedVersion("https://jira.atlassian.com", "JRA");
		Assert.assertNotNull(releasedVersion);
		Assert.assertNotNull(releasedVersion.getName());
		Assert.assertNotNull(releasedVersion.getReleaseDate());
	}

	@Test
	public void isValidVersionFirst() throws Exception {
		final AtlassianVersion jiraVersion = new AtlassianVersion();
		jiraVersion.setName("1.0");
		jiraVersion.setReleased(true);
		jiraVersion.setReleaseDate(new Date());
		Assert.assertTrue(new VersionUtils().isValidVersion(null, jiraVersion));
	}

	@Test
	public void isValidVersionArchived() throws Exception {
		final AtlassianVersion jiraVersion = new AtlassianVersion();
		jiraVersion.setArchived(true);
		jiraVersion.setName("1.0");
		jiraVersion.setReleased(true);
		jiraVersion.setReleaseDate(new Date());
		Assert.assertFalse(new VersionUtils().isValidVersion(null, jiraVersion));
	}

	@Test
	public void isValidVersionUnreleased() throws Exception {
		final AtlassianVersion jiraVersion = new AtlassianVersion();
		jiraVersion.setName("1.0");
		jiraVersion.setReleaseDate(new Date());
		Assert.assertFalse(new VersionUtils().isValidVersion(null, jiraVersion));
	}

	@Test
	public void isValidVersionNoReleaseDate() throws Exception {
		final AtlassianVersion jiraVersion = new AtlassianVersion();
		jiraVersion.setArchived(false);
		jiraVersion.setName("1.0");
		jiraVersion.setReleased(true);
		Assert.assertFalse(new VersionUtils().isValidVersion(null, jiraVersion));
	}

	@Test
	public void isValidVersionNewer() throws Exception {
		final AtlassianVersion previous = new AtlassianVersion();
		previous.setName("1.0");
		previous.setReleased(true);
		previous.setReleaseDate(new Date());

		final AtlassianVersion jiraVersion = new AtlassianVersion();
		jiraVersion.setName("1.1");
		jiraVersion.setReleased(true);
		jiraVersion.setReleaseDate(new Date());
		Assert.assertTrue(new VersionUtils().isValidVersion(previous, jiraVersion));
	}

	@Test
	public void isValidVersionInvalidName() throws Exception {
		final AtlassianVersion jiraVersion = new AtlassianVersion();
		jiraVersion.setArchived(false);
		jiraVersion.setName("OLD 1.0");
		jiraVersion.setReleased(true);
		jiraVersion.setReleaseDate(new Date());
		Assert.assertFalse(new VersionUtils().isValidVersion(null, jiraVersion));
	}

	@Test
	public void isValidVersionOlder() throws Exception {
		final AtlassianVersion previous = new AtlassianVersion();
		previous.setName("1.1");
		previous.setReleased(true);
		previous.setReleaseDate(new Date());

		final AtlassianVersion jiraVersion = new AtlassianVersion();
		jiraVersion.setName("1.0");
		jiraVersion.setReleased(true);
		jiraVersion.setReleaseDate(new Date());
		Assert.assertFalse(new VersionUtils().isValidVersion(previous, jiraVersion));
	}

	@Test
	public void getLatestReleasedVersionName() throws Exception {
		final String releasedVersion = new VersionUtils().getLatestReleasedVersionName("https://jira.atlassian.com", "JRA");
		Assert.assertNotNull(releasedVersion);
	}

	@Test
	public void getLatestReleasedVersionNameFailed() throws Exception {
		Assert.assertNull(new VersionUtils().getLatestReleasedVersionName("any:some", "0"));
	}

}
