package org.cryptomator.ui.keyloading.masterkeyfile;

import org.cryptomator.cryptolib.api.MasterkeyLoadingFailedException;
import org.cryptomator.cryptolib.common.MasterkeyFileLoader;
import org.cryptomator.ui.keyloading.KeyLoading;
import org.cryptomator.ui.keyloading.KeyLoadingStrategy;

import javax.inject.Inject;

@KeyLoading
class MasterkeyFileLoadingStrategy implements KeyLoadingStrategy {

	private final MasterkeyFileLoader masterkeyFileLoader;
	private final MasterkeyFileLoadingContext context;
	private final MasterkeyFileLoadingFinisher finisher;

	@Inject
	public MasterkeyFileLoadingStrategy(MasterkeyFileLoader masterkeyFileLoader, MasterkeyFileLoadingContext context, MasterkeyFileLoadingFinisher finisher) {
		this.masterkeyFileLoader = masterkeyFileLoader;
		this.context = context;
		this.finisher = finisher;
	}

	@Override
	public MasterkeyFileLoader masterkeyLoader() {
		return masterkeyFileLoader;
	}

	@Override
	public boolean recoverFromException(MasterkeyLoadingFailedException exception) {
		return context.recoverFromException(exception);
	}

	@Override
	public void cleanup(boolean unlockedSuccessfully) {
		finisher.cleanup(unlockedSuccessfully);
	}
}
