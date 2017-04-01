package org.ligoj.app.api;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * A plug-in. The plug-in behavior is massively based on naming convention. The key of the plug-in must be unique and
 * following the bellow rules :
 * <ul>
 * <li>Must follows this pattern <code>[a-z\d]+(:[a-z\d]+)*</code></li>
 * <li>Must be unique</li>
 * </ul>
 */
public interface FeaturePlugin extends Comparable<FeaturePlugin> {

	/**
	 * Return the plug-in key.
	 * 
	 * @return the plug-in key.
	 */
	String getKey();

	/**
	 * Return the plug-in version.
	 * 
	 * @return the plug-in version. Should follow the <a href="http://semver.org/">semantic version management</a>
	 */
	default String getVersion() {
		return getClass().getPackage().getImplementationVersion();
	}

	/**
	 * Return the plug-in name. By default the name is computed from the <code>MANIFEST.MF#Implementation-Title</code>
	 * attribute. When not available, the last capitalized part (after the last <code>:</code> separator) of plug-in's
	 * key is used.
	 * 
	 * @return the plug-in name. Never <code>null</code>.
	 */
	default String getName() {
		return StringUtils.defaultIfBlank(getClass().getPackage().getImplementationTitle(),
				StringUtils.capitalize(getKey().substring(getKey().lastIndexOf(':') + 1).replace(':', ' ')));
	}

	/**
	 * Return the plug-in vendor.
	 * 
	 * @return the plug-in vendor. May be <code>null</code>.
	 */
	default String getVendor() {
		return getClass().getPackage().getImplementationVendor();
	}

	/**
	 * Callback used to persist some additional when the plug-in is being installed.
	 */
	default void install() {
		// Nothing to do
	}

	@Override
	default int compareTo(final FeaturePlugin o) {
		// Compare the plug-in by their key
		return getKey().compareTo(o.getKey());
	}

	/**
	 * Return entities class to be persisted during the installation from CSV files located in the "csv" folder of this
	 * plug-in. CsvForJpa component will be used. Order is important. First {@link Class} will be associated to the
	 * right CSV file and persisted in the database, then the next one. When empty, or not containing the "Node.class"
	 * value, a default Node will be inserted by default.
	 * 
	 * @return Entities class to be persisted during the installation from CSV files
	 */
	default List<Class<?>> getInstalledEntities() {
		return Collections.emptyList();
	}

	/**
	 * Callback used to update some data when the plug-in is being updated from the given version.
	 * 
	 * @param odlVersion
	 *            The previously installed version.
	 */
	default void update(final String odlVersion) {
		// Nothing to do
	}
}
