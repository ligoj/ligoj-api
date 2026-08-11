/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test class of {@link DefaultVerificationMode}
 */
class DefaultVerificationModeTest {

	private boolean checked = false;

	@Test
	void testCall() {
		var context = mock(ApplicationContext.class);
		context.getBean("some");
		verify(context, new DefaultVerificationMode(data -> {
			Assertions.assertEquals(1, data.getAllInvocations().size());
			setChecked(true);
		})).getBean("some");
		Assertions.assertTrue(this.checked);
	}

	@Test
	void testNotCall() {
		var context = mock(ApplicationContext.class);
		var mode = new DefaultVerificationMode(data -> {
			Assertions.assertEquals(0, data.getAllInvocations().size());
			setChecked(true);
		});
		verify(context, mode).getBean("some");
		Assertions.assertNotNull(mode.description("some"));
	}

	private void setChecked(final boolean checked) {
		this.checked = checked;
	}
}
