/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.util.thread.ThreadClassLoaderScope;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ligoj.app.model.Node;
import org.ligoj.app.model.Subscription;
import org.mockito.Mockito;

/**
 * Test class of {@link LigojPluginsClassLoader}
 */
class LigojPluginsClassLoaderTest {

	protected static final String USER_HOME_DIRECTORY = "target/test-classes/home-test";

	@BeforeEach
	void cleanHome() throws IOException {
		FileUtils.deleteDirectory(new File(new File(USER_HOME_DIRECTORY, LigojPluginsClassLoader.HOME_DIR_FOLDER),
				LigojPluginsClassLoader.EXPORT_DIR));
	}

	@Test
	void getInstance() {
		try (ThreadClassLoaderScope scope = new ThreadClassLoaderScope(
				new URLClassLoader(new URL[0], Mockito.mock(LigojPluginsClassLoader.class)))) {
			Assertions.assertNotNull(LigojPluginsClassLoader.getInstance());
		}
	}

	@Test
	void toFile() throws IOException, NoSuchAlgorithmException {
		final File file = new File(USER_HOME_DIRECTORY, ".ligoj/service-id/ldap/server1/42/foo/bar.log");
		final File subscriptionParent = new File(USER_HOME_DIRECTORY, ".ligoj/service-id");
		FileUtils.deleteQuietly(subscriptionParent);
		Assertions.assertFalse(subscriptionParent.exists());
		Assertions.assertFalse(file.exists());
		try {
			System.setProperty("ligoj.home", USER_HOME_DIRECTORY + "/.ligoj");
			try (LigojPluginsClassLoader classLoader = checkClassLoader()) {
				final Subscription subscription = newSubscription();
				final File cfile = classLoader.toPath(subscription, "foo", "bar.log").toFile();
				Assertions.assertTrue(subscriptionParent.exists());
				Assertions.assertTrue(cfile.getParentFile().exists());
				Assertions.assertTrue(file.getParentFile().exists());
			}
			Assertions.assertFalse(file.exists());
		} finally {
			System.clearProperty("ligoj.home");
		}
	}

	private Subscription newSubscription() {
		Subscription subscription = new Subscription();
		Node node = new Node();
		node.setId("service:id:ldap:server1");
		Node tool = new Node();
		tool.setId("service:id:ldap");
		Node service = new Node();
		service.setId("service:id");
		tool.setRefined(service);
		node.setRefined(tool);
		subscription.setNode(node);
		subscription.setId(42);
		return subscription;
	}

	private LigojPluginsClassLoader checkClassLoader() throws IOException, NoSuchAlgorithmException {
		final LigojPluginsClassLoader classLoader = new LigojPluginsClassLoader();
		Assertions.assertEquals(3, classLoader.getURLs().length);

		// Check the home is in the class-path
		final URL homeUrl = classLoader.getURLs()[0];
		Assertions.assertTrue(homeUrl.getFile().endsWith("/"));

		// Check the plug-in is in the class-path
		final URL pluginTestUrl = classLoader.getURLs()[1];
		Assertions.assertTrue(pluginTestUrl.getFile().endsWith("plugin-foo-1.0.1.jar"));

		// Check the JAR is readable
		try (InputStream pluginTestUrlStream = pluginTestUrl.openStream()) {
			Assertions.assertNotNull(pluginTestUrlStream);
		}

		// Check the content of the plug-in is resolvable from the class loader
		IOUtils.toString(classLoader.getResourceAsStream("home-test/.ligoj/plugins/plugin-foo-1.0.1.jar"),
				StandardCharsets.UTF_8.name());
		Assertions.assertEquals("FOO",
				IOUtils.toString(classLoader.getResourceAsStream("plugin-foo.txt"), StandardCharsets.UTF_8.name()));

		final File export = new File(USER_HOME_DIRECTORY + "/.ligoj/export");
		Assertions.assertTrue(export.exists());
		Assertions.assertTrue(export.isDirectory());
		Assertions.assertTrue(new File(export, "export.txt").exists());
		Assertions.assertTrue(new File(export, "export.txt").isFile());
		Assertions.assertEquals("EXPORT",
				FileUtils.readFileToString(new File(export, "export.txt"), StandardCharsets.UTF_8.name()));
		return classLoader;
	}
}
