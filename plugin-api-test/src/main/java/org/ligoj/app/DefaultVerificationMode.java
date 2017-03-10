package org.ligoj.app;

import java.util.function.Consumer;

import org.mockito.internal.verification.VerificationModeFactory;
import org.mockito.internal.verification.api.VerificationData;
import org.mockito.verification.VerificationMode;

public class DefaultVerificationMode implements VerificationMode {

	private Consumer<VerificationData> dataVerify;

	public DefaultVerificationMode(final Consumer<VerificationData> dataVerify) {
		this.dataVerify = dataVerify;
	}

	/**
	 * Performs the verification
	 */
	@Override
	public void verify(final VerificationData data) {
		dataVerify.accept(data);
	}

	@Override
	public VerificationMode description(String description) {
		return VerificationModeFactory.description(this, description);
	}

}
