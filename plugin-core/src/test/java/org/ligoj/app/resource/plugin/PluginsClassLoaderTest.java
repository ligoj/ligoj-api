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
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.util.thread.ThreadClassLoaderScope;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ligoj.app.api.PluginException;
import org.ligoj.app.model.Node;
import org.ligoj.app.model.Subscription;
import org.mockito.Mockito;

/**
 * Test class of {@link PluginsClassLoader}
 */
public class PluginsClassLoaderTest {

	protected static final String USER_HOME_DIRECTORY = "target/test-classes/home-test";

	@BeforeEach
	public void cleanHome() throws IOException {
		FileUtils.deleteDirectory(new File(new File(USER_HOME_DIRECTORY, PluginsClassLoader.HOME_DIR_FOLDER),
				PluginsClassLoader.EXPORT_DIR));
	}

	@Test
	public void getInstalledPlugins() throws IOException {
		final String oldHome = System.getProperty("user.home");
		try {
			System.setProperty("user.home", USER_HOME_DIRECTORY);
			try (PluginsClassLoader classLoader = checkClassLoader()) {
				// Nothing to do
				final Map<String, String> plugins = classLoader.getInstalledPlugins();
				Assertions.assertEquals(2,plugins.size());
				Assertions.assertEquals("plugin-foo-Z0000001Z0000000Z0000001Z0000000", plugins.get("plugin-foo"));
				Assertions.assertEquals("plugin-bar-Z0000001Z0000000Z0000000Z0000000", plugins.get("plugin-bar"));
			}
		} finally {
			System.setProperty("user.home", oldHome);
		}
	}

	@Test
	public void safeMode() throws IOException {
		final String old = System.getProperty("ligoj.safe.mode");
		try {
			System.setProperty("ligoj.safe.mode", "true");
			try (PluginsClassLoader classLoader = new PluginsClassLoader()) {
				Assertions.assertFalse(classLoader.isEnabled());

				// Check the home is in the class-path
				final URL homeUrl = classLoader.getURLs()[0];
				Assertions.assertTrue(homeUrl.getFile().endsWith("/"));

				// Check the plug-in is in the class-path
				Assertions.assertEquals(1, classLoader.getURLs().length);
			}
		} finally {
			if (old == null) {
				System.clearProperty("ligoj.plugin.enabled");
			} else {
				System.setProperty("ligoj.safe.mode", old);
			}
		}
	}

	@Test
	public void forcedHome() throws IOException {
		try {
			System.setProperty("ligoj.home", USER_HOME_DIRECTORY + "/.ligoj");
			try (PluginsClassLoader classLoader = checkClassLoader()) {
				// Nothing to do
			}
		} finally {
			System.clearProperty("ligoj.home");
		}
	}

	@Test
	public void getInstanceNull() {
		Assertions.assertNull(PluginsClassLoader.getInstance());
	}

	@Test
	public void toExtendedVersion() {
		Assertions.assertEquals("Z0000000Z0000000Z0000000Z0000000", PluginsClassLoader.toExtendedVersion(null));
		Assertions.assertEquals("Z0000000Z0000000Z0000000Z0000000", PluginsClassLoader.toExtendedVersion(""));
		Assertions.assertEquals("Z0000001Z0000000Z0000000Z0000000", PluginsClassLoader.toExtendedVersion("1.0"));
		Assertions.assertEquals("Z0000001Z0000002Z0000003Z0000004", PluginsClassLoader.toExtendedVersion("1.2.3.4"));
		Assertions.assertEquals("Z0000012Z0000034Z0000056Z0000789",
				PluginsClassLoader.toExtendedVersion("12.34.56.789"));
		Assertions.assertEquals("Z0000012Z000003bZ000005AZ0000000", PluginsClassLoader.toExtendedVersion("12.3b.5A"));
	}

	@Test
	public void getInstance() {
		try (ThreadClassLoaderScope scope = new ThreadClassLoaderScope(
				new URLClassLoader(new URL[0], Mockito.mock(PluginsClassLoader.class)))) {
			Assertions.assertNotNull(PluginsClassLoader.getInstance());
		}
	}

	@Test
	public void forcedHomeTwice() throws Exception {
		try {
			System.setProperty("ligoj.home", USER_HOME_DIRECTORY + "/.ligoj");
			try (PluginsClassLoader classLoader = checkClassLoader()) {
				Assertions.assertNotNull(classLoader.getHomeDirectory());
				Assertions.assertNotNull(classLoader.getPluginDirectory());
			}
		} finally {
			System.clearProperty("ligoj.home");
		}
	}

	@Test
	public void toFile() throws IOException {
		final File file = new File(USER_HOME_DIRECTORY, ".ligoj/service-id/ldap/server1/42/foo/bar.log");
		final File subscriptionParent = new File(USER_HOME_DIRECTORY, ".ligoj/service-id");
		FileUtils.deleteQuietly(subscriptionParent);
		Assertions.assertFalse(subscriptionParent.exists());
		Assertions.assertFalse(file.exists());
		try {
			System.setProperty("ligoj.home", USER_HOME_DIRECTORY + "/.ligoj");
			try (PluginsClassLoader classLoader = checkClassLoader()) {
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

	@Test
	public void copyFailed() {
		final AtomicReference<PluginsClassLoader> refError = new AtomicReference<>();
		Assertions.assertThrows(PluginException.class, () -> {
			try {
				System.setProperty("ligoj.home", USER_HOME_DIRECTORY + "/.ligoj");
				try (PluginsClassLoader classLoader = new PluginsClassLoader() {
					@Override
					protected void copy(final Path from, final Path dest) throws IOException {
						throw new IOException();
					}
				}) {
					classLoader.copyExportedResources("any", null);
				}
			} finally {
				System.clearProperty("ligoj.home");
				if (refError.get() != null) {
					refError.get().close();
				}
			}
		});
	}

	@Test
	public void copyAlreadyExists() throws IOException {
		final AtomicReference<PluginsClassLoader> refError = new AtomicReference<>();
		try {
			System.setProperty("ligoj.home", USER_HOME_DIRECTORY + "/.ligoj");
			try (PluginsClassLoader classLoader = new PluginsClassLoader() {
				@Override
				protected void copy(final Path from, final Path dest) throws IOException {
					if (!from.toString().endsWith("/export")) {
						FileUtils.touch(dest.toFile());
					}
				}
			}) {
				classLoader.copyExportedResources("plugin-foo",
						new File(USER_HOME_DIRECTORY, ".ligoj/plugins/plugin-foo-1.0.1.jar").toPath());
				File exported = new File(USER_HOME_DIRECTORY, ".ligoj/export/export.txt");
				Assertions.assertTrue(exported.exists());
				FileUtils.write(exported, "value", StandardCharsets.UTF_8);

				// Copy again without error or overwrite
				classLoader.copyExportedResources("plugin-foo",
						new File(USER_HOME_DIRECTORY, ".ligoj/plugins/plugin-foo-1.0.1.jar").toPath());

				Assertions.assertTrue(exported.exists());
				Assertions.assertEquals("value", FileUtils.readFileToString(exported, StandardCharsets.UTF_8));

			}
		} finally {
			System.clearProperty("ligoj.home");
			if (refError.get() != null) {
				refError.get().close();
			}
		}
	}

	private PluginsClassLoader checkClassLoader() throws IOException {
		final PluginsClassLoader classLoader = new PluginsClassLoader();
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
