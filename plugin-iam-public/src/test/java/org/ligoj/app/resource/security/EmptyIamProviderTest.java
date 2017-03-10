package org.ligoj.app.resource.security;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Test class of {@link EmptyIamProvider}.
 */
@Component
public class EmptyIamProviderTest {

	@Test
	public void authenticate() throws Exception {
		final Authentication mock = Mockito.mock(Authentication.class);
		Assert.assertSame(mock, new EmptyIamProvider().authenticate(mock));
	}

	@Test
	public void getConfiguration() throws Exception {
		Assert.assertNotNull(new EmptyIamProvider().getConfiguration());
	}
}
