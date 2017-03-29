package org.ligoj.app.api;

import org.junit.Test;
import org.ligoj.app.iam.SimpleUser;
import org.ligoj.bootstrap.model.AbstractBusinessEntityTest;

/**
 * Test business keyed entities basic ORM operations : hash code and equals.
 */
public class TestAbstractBusinessEntityTest extends AbstractBusinessEntityTest {

	/**
	 * Test equals and hash code operation with all possible combinations with only one identifier.
	 */
	@Test
	public void testEqualsAndHash() throws Exception {
		testEqualsAndHash(SimpleUser.class, "id");
	}

}
