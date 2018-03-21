/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;

/**
 * Test class of {@link DefaultVerificationMode}
 */
public class DefaultVerificationModeTest {

	private boolean checked = false;

	@Test
	public void testCall() {
		ApplicationContext context = Mockito.mock(ApplicationContext.class);
		context.getBean("some");
		Mockito.verify(context, new DefaultVerificationMode(data -> {
			Assertions.assertEquals(1, data.getAllInvocations().size());
			setChecked(true);
		})).getBean("some");
		Assertions.assertTrue(this.checked);
	}

	@Test
	public void testNotCall() {
		ApplicationContext context = Mockito.mock(ApplicationContext.class);
		DefaultVerificationMode mode = new DefaultVerificationMode(data -> {
			Assertions.assertEquals(0, data.getAllInvocations().size());
			setChecked(true);
		});
		Mockito.verify(context, mode).getBean("some");
		Assertions.assertNotNull(mode.description("some"));
	}

	private final void setChecked(final boolean checked) {
		this.checked = checked;
	}
}
