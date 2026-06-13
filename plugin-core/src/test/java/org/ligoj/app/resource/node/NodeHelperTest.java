/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.resource.node;

import java.util.ArrayList;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ligoj.app.model.Node;
import org.ligoj.app.resource.ServicePluginLocator;
import org.mockito.Mockito;

/**
 * Test class of {@link NodeHelper}, focused on the {@code uiColor} attribute
 * carried from the {@link Node} entity to the {@link org.ligoj.app.api.NodeVo}
 * (companion of {@code uiClasses}).
 */
class NodeHelperTest {

	private static final String NODE_ID = "service:test:tool";
	private static final String PARENT_ID = "service:test";

	private Node newNode(final String id, final String name, final String uiColor) {
		final var entity = new Node();
		entity.setId(id);
		entity.setName(name);
		entity.setUiColor(uiColor);
		return entity;
	}

	@Test
	void toVoCopiesUiColorFromEntity() {
		final var vo = NodeHelper.toVo(newNode(NODE_ID, "Test tool", "#0052CC"));
		Assertions.assertEquals("#0052CC", vo.getUiColor());
	}

	@Test
	void toVoKeepsUiColorNullWhenUnset() {
		final var vo = NodeHelper.toVo(newNode(NODE_ID, "Test tool", null));
		Assertions.assertNull(vo.getUiColor());
	}

	@Test
	void toVoLightAndToVoParametersAlsoCopyUiColor() {
		final var locator = Mockito.mock(ServicePluginLocator.class);
		Mockito.when(locator.isEnabled(NODE_ID)).thenReturn(true);

		Assertions.assertEquals("#0052CC",
				NodeHelper.toVoLight(newNode(NODE_ID, "Test tool", "#0052CC"), locator).getUiColor());

		final var rows = new ArrayList<Object[]>();
		rows.add(new Object[] { newNode(NODE_ID, "Test tool", "#0052CC"), null });
		Assertions.assertEquals("#0052CC",
				NodeHelper.toVoParameters(rows, locator).get(NODE_ID).getUiColor());
	}

	@Test
	void toVoCarriesUiColorPerNode() {
		// Each node keeps its own color; the parent's color is exposed on the
		// refined VO — there is no backend parent-chain inheritance (a UI
		// concern via the `refined` reference).
		final var child = newNode(NODE_ID, "Test tool", "#FF0000");
		child.setRefined(newNode(PARENT_ID, "Test service", "#0052CC"));

		final var vo = NodeHelper.toVo(child);

		Assertions.assertEquals("#FF0000", vo.getUiColor());
		Assertions.assertEquals("#0052CC", vo.getRefined().getUiColor());
	}

	@Test
	void toVoLeafWithoutColorDoesNotInheritParent() {
		// Confirms the removed backend inheritance: a leaf without a color stays
		// null even when its parent declares one.
		final var child = newNode(NODE_ID, "Test tool", null);
		child.setRefined(newNode(PARENT_ID, "Test service", "#0052CC"));

		final var vo = NodeHelper.toVo(child);

		Assertions.assertNull(vo.getUiColor());
		Assertions.assertEquals("#0052CC", vo.getRefined().getUiColor());
	}
}
