/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.node;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ligoj.app.api.ServicePlugin;
import org.ligoj.app.model.Node;
import org.ligoj.app.resource.ServicePluginLocator;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

/**
 * Test class of {@link NodeHelper}, focused on the plugin-derived
 * {@code preferredColor} enrichment applied by {@code applyPlugin}.
 */
class NodeHelperTest {

	private static final String NODE_ID = "service:test:tool";
	private static final String PARENT_ID = "service:test";

	private Node newNode() {
		return newNode(NODE_ID, "Test tool");
	}

	private Node newNode(final String id, final String name) {
		final var entity = new Node();
		entity.setId(id);
		entity.setName(name);
		return entity;
	}

	/**
	 * Build a locator stubbing the given plugin for a single node id.
	 */
	private void stub(final ServicePluginLocator locator, final String id, final ServicePlugin plugin) {
		Mockito.when(locator.getResource(ArgumentMatchers.eq(id), ArgumentMatchers.eq(ServicePlugin.class)))
				.thenReturn(plugin);
	}

	private ServicePlugin pluginWithColor(final String color) {
		final var plugin = Mockito.mock(ServicePlugin.class);
		Mockito.when(plugin.getPreferredColor()).thenReturn(color);
		return plugin;
	}

	private ServicePluginLocator locatorReturning(final ServicePlugin plugin) {
		final var locator = Mockito.mock(ServicePluginLocator.class);
		Mockito.when(locator.getResource(ArgumentMatchers.eq(NODE_ID), ArgumentMatchers.eq(ServicePlugin.class)))
				.thenReturn(plugin);
		return locator;
	}

	@Test
	void toVoFillsPreferredColorFromPlugin() {
		final var plugin = Mockito.mock(ServicePlugin.class);
		Mockito.when(plugin.getPreferredColor()).thenReturn("#0052CC");

		final var vo = NodeHelper.toVo(newNode(), locatorReturning(plugin));

		Assertions.assertEquals("#0052CC", vo.getPreferredColor());
	}

	@Test
	void toVoKeepsPreferredColorNullWhenPluginAbsent() {
		final var vo = NodeHelper.toVo(newNode(), locatorReturning(null));

		Assertions.assertNull(vo.getPreferredColor());
	}

	@Test
	void toVoKeepsPreferredColorNullWhenPluginDeclaresNull() {
		// Anonymous plugin relying on the default getPreferredColor() == null
		final ServicePlugin plugin = () -> NODE_ID;

		final var vo = NodeHelper.toVo(newNode(), locatorReturning(plugin));

		Assertions.assertNull(vo.getPreferredColor());
	}

	@Test
	void toVoLightAndToVoParameterAlsoFillPreferredColor() {
		final var plugin = Mockito.mock(ServicePlugin.class);
		Mockito.when(plugin.getPreferredColor()).thenReturn("#0052CC");
		final var locator = locatorReturning(plugin);

		Assertions.assertEquals("#0052CC", NodeHelper.toVoLight(newNode(), locator).getPreferredColor());

		final var rows = new java.util.ArrayList<Object[]>();
		rows.add(new Object[] { newNode(), null });
		Assertions.assertEquals("#0052CC",
				NodeHelper.toVoParameters(rows, locator).get(NODE_ID).getPreferredColor());
	}

	@Test
	void toVoInheritsPreferredColorFromParent() {
		final var child = newNode();
		child.setRefined(newNode(PARENT_ID, "Test service"));

		final var locator = Mockito.mock(ServicePluginLocator.class);
		// Leaf tool declares no color, parent service does.
		stub(locator, NODE_ID, pluginWithColor(null));
		stub(locator, PARENT_ID, pluginWithColor("#0052CC"));

		final var vo = NodeHelper.toVo(child, locator);

		Assertions.assertEquals("#0052CC", vo.getPreferredColor());
	}

	@Test
	void toVoChildPreferredColorOverridesParent() {
		final var child = newNode();
		child.setRefined(newNode(PARENT_ID, "Test service"));

		final var locator = Mockito.mock(ServicePluginLocator.class);
		// Leaf tool overrides the color declared by its parent service.
		stub(locator, NODE_ID, pluginWithColor("#FF0000"));
		stub(locator, PARENT_ID, pluginWithColor("#0052CC"));

		final var vo = NodeHelper.toVo(child, locator);

		Assertions.assertEquals("#FF0000", vo.getPreferredColor());
	}
}
