package org.cryptomator.ui.addvaultwizard;

import com.google.common.base.Preconditions;
import org.cryptomator.cryptolib.common.MasterkeyFileLoaderContext;

import java.nio.file.Path;

class StaticMasterkeyFileLoaderContext implements MasterkeyFileLoaderContext {

	private final Path masterkeyFilePath;
	private final CharSequence passphrase;

	StaticMasterkeyFileLoaderContext(Path masterkeyFilePath, CharSequence passphrase) {
		this.masterkeyFilePath = masterkeyFilePath;
		this.passphrase = passphrase;
	}

	@Override
	public Path getMasterkeyFilePath(String s) {
		return masterkeyFilePath;
	}

	@Override
	public CharSequence getPassphrase(Path path) {
		Preconditions.checkArgument(masterkeyFilePath.equals(path));
		return passphrase;
	}
}
