/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.plugin;

import java.io.File;
import java.io.IOException;
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
import org.ligoj.bootstrap.core.plugin.PluginsClassLoader;
import org.mockito.Mockito;

/**
 * Test class of {@link LigojPluginsClassLoader}
 */
class LigojPluginsClassLoaderTest {

	protected static final String USER_HOME_DIRECTORY = "target/test-classes/home-test";

	@BeforeEach
	void cleanHome() throws IOException {
		FileUtils.deleteDirectory(new File(new File(USER_HOME_DIRECTORY, PluginsClassLoader.HOME_DIR_FOLDER),
				PluginsClassLoader.EXPORT_DIR));
	}

	@Test
	void getInstance() {
		try (var ignored = new ThreadClassLoaderScope(
				new URLClassLoader(new URL[0], Mockito.mock(LigojPluginsClassLoader.class)))) {
			Assertions.assertNotNull(LigojPluginsClassLoader.getInstance());
		}
	}

	@Test
	void toFile() throws IOException, NoSuchAlgorithmException {
		final var file = new File(USER_HOME_DIRECTORY, ".ligoj/service-id/ldap/server1/42/foo/bar.log");
		final var subscriptionParent = new File(USER_HOME_DIRECTORY, ".ligoj/service-id");
		FileUtils.deleteQuietly(subscriptionParent);
		Assertions.assertFalse(subscriptionParent.exists());
		Assertions.assertFalse(file.exists());
		try {
			System.setProperty("ligoj.home", USER_HOME_DIRECTORY + "/.ligoj");
			try (var classLoader = checkClassLoader()) {
				final var subscription = newSubscription();
				final var cFile = classLoader.toPath(subscription, "foo", "bar.log").toFile();
				Assertions.assertTrue(subscriptionParent.exists());
				Assertions.assertTrue(cFile.getParentFile().exists());
				Assertions.assertTrue(file.getParentFile().exists());
			}
			Assertions.assertFalse(file.exists());
		} finally {
			System.clearProperty("ligoj.home");
		}
	}

	private Subscription newSubscription() {
		var subscription = new Subscription();
		var node = new Node();
		node.setId("service:id:ldap:server1");
		var tool = new Node();
		tool.setId("service:id:ldap");
		var service = new Node();
		service.setId("service:id");
		tool.setRefined(service);
		node.setRefined(tool);
		subscription.setNode(node);
		subscription.setId(42);
		return subscription;
	}

	private LigojPluginsClassLoader checkClassLoader() throws IOException, NoSuchAlgorithmException {
		final var classLoader = new LigojPluginsClassLoader();
		Assertions.assertEquals(3, classLoader.getURLs().length);

		// Check the home is in the class-path
		final var homeUrl = classLoader.getURLs()[0];
		Assertions.assertTrue(homeUrl.getFile().endsWith("/"));

		// Check the plug-in is in the class-path
		final var pluginTestUrl = classLoader.getURLs()[1];
		Assertions.assertTrue(pluginTestUrl.getFile().endsWith("plugin-foo-1.0.1.jar"));

		// Check the JAR is readable
		try (var pluginTestUrlStream = pluginTestUrl.openStream()) {
			Assertions.assertNotNull(pluginTestUrlStream);
		}

		// Check the content of the plug-in is resolvable from the class loader
		IOUtils.toString(classLoader.getResourceAsStream("home-test/.ligoj/plugins/plugin-foo-1.0.1.jar"),
				StandardCharsets.UTF_8);
		Assertions.assertEquals("FOO",
				IOUtils.toString(classLoader.getResourceAsStream("plugin-foo.txt"), StandardCharsets.UTF_8));

		final var export = new File(USER_HOME_DIRECTORY + "/.ligoj/export");
		Assertions.assertTrue(export.exists());
		Assertions.assertTrue(export.isDirectory());
		Assertions.assertTrue(new File(export, "export.txt").exists());
		Assertions.assertTrue(new File(export, "export.txt").isFile());
		Assertions.assertEquals("EXPORT",
				FileUtils.readFileToString(new File(export, "export.txt"), StandardCharsets.UTF_8));
		return classLoader;
	}
}
