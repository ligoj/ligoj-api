package org.ligoj.app;

import org.junit.Assert;
import org.junit.Test;
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
			Assert.assertEquals(1, data.getAllInvocations().size());
			setChecked(true);
		})).getBean("some");
		Assert.assertTrue(this.checked);
	}

	@Test
	public void testNotCall() {
		ApplicationContext context = Mockito.mock(ApplicationContext.class);
		DefaultVerificationMode mode = new DefaultVerificationMode(data -> {
			Assert.assertEquals(0, data.getAllInvocations().size());
			setChecked(true);
		});
		Mockito.verify(context, mode).getBean("some");
		Assert.assertNotNull(mode.description("some"));
	}

	private final void setChecked(final boolean checked) {
		this.checked = checked;
	}
}
