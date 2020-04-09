/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.plugin;

import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.ligoj.app.model.Node;
import org.ligoj.app.model.Subscription;
import org.ligoj.bootstrap.core.plugin.PluginsClassLoader;

import lombok.Getter;

/**
 * Class Loader which load jars in {@value #PLUGINS_DIR} directory inside the home directory.
 */
@Getter
public class LigojPluginsClassLoader extends PluginsClassLoader {

	/**
	 * Default constructor.
	 * @throws IOException
	 *             exception when reading plug-ins directory
	 * @throws NoSuchAlgorithmException MD5 digest is unavailable for version ciphering.
	 */
	public LigojPluginsClassLoader() throws IOException, NoSuchAlgorithmException {
		super();
	}

	/**
	 * Return the plug-in class loader from the current class loader.
	 *
	 * @return the closest {@link PluginsClassLoader} instance from the current thread's {@link ClassLoader}. May be
	 *         <code>null</code>.
	 */
	public static LigojPluginsClassLoader getInstance() {
		return (LigojPluginsClassLoader) org.ligoj.bootstrap.core.plugin.PluginsClassLoader.getInstance();
	}

	/**
	 * Get a file reference for a specific subscription. This file will use the subscription as a context to isolate it,
	 * and using the related node and the subscription's identifier. The parent directories are created as needed.
	 *
	 * @param subscription
	 *            The subscription used a context of the file to create.
	 * @param fragments
	 *            The file fragments.
	 * @return The {@link Path} reference.
	 * @throws IOException
	 *             When the parent directories creation failed.
	 * @since 2.2.4
	 */
	public Path toPath(final Subscription subscription, final String... fragments) throws IOException {
		return toPath(toPath(subscription.getNode()).resolve(String.valueOf(subscription.getId())), fragments);
	}

	/**
	 * Get a file reference for a specific node. This file will use the node as a context to isolate it. The parent
	 * directories are created as needed.
	 *
	 * @param node
	 *            The related node.
	 * @return The {@link Path} reference.
	 * @throws IOException
	 *             When the parent directories creation failed.
	 * @since 2.2.4
	 */
	public Path toPath(final Node node) throws IOException {
		final var file = toPath(toFragments(node));
		FileUtils.forceMkdir(file.toFile());
		return file;
	}

	/**
	 * Convert a {@link Node} to a {@link Path} inside the given parent directory.
	 *
	 * @param node
	 *            The related node.
	 * @return The computed sibling path.
	 */
	private String[] toFragments(final Node node) {
		final var fragments = new ArrayList<String>();
		toFragments(node, fragments);
		return fragments.toArray(new String[fragments.size()]);
	}

	/**
	 * Convert a {@link Node} to a {@link Path} inside the given parent directory.
	 *
	 * @param node
	 *            The related node.
	 * @param fragments
	 *            The computed sibling path (updated).
	 */
	private void toFragments(final Node node, List<String> fragments) {
		if (node.isRefining()) {
			toFragments(node.getRefined(), fragments);
		}
		fragments.add(toFragmentId(node).replace(':', '-'));
	}

	/**
	 * Return the last part of the node identifier, excluding the part of the parent. Built like that :
	 * <ul>
	 * <li>node = 'service:id:ldap:ad1', fragment = 'ad1'</li>
	 * <li>node = 'service:id:ldap', fragment = 'ldap'</li>
	 * <li>node = 'service:id', fragment = 'service:id'</li>
	 * </ul>
	 *
	 * @param node
	 *            The node to convert to a simple fragment String.
	 * @return The simple fragment.
	 */
	private String toFragmentId(final Node node) {
		return node.isRefining() ? node.getId().substring(node.getRefined().getId().length() + 1) : node.getId();
	}
}
