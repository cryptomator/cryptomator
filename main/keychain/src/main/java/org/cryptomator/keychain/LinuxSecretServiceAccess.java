package org.cryptomator.keychain;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.SystemUtils;

import javax.inject.Inject;
import java.util.Optional;

public class LinuxSecretServiceAccess implements KeychainAccessStrategy {
	private final Optional<GnomeKeyringAccess> gnomeLoginKeyring;

	@Inject
	public LinuxSecretServiceAccess() {
		gnomeLoginKeyring = LinuxKeychainTester.getSecretService();
	}

	@Override
	public boolean isSupported() {
		return SystemUtils.IS_OS_LINUX && LinuxKeychainTester.getSecretService().isPresent();
	}

	@Override
	public void storePassphrase(String key, CharSequence passphrase) {
		Preconditions.checkState(gnomeLoginKeyring.isPresent());
		gnomeLoginKeyring.get().storePassword(key, passphrase);
	}

	@Override
	public char[] loadPassphrase(String key) {
		Preconditions.checkState(gnomeLoginKeyring.isPresent());
		return gnomeLoginKeyring.get().loadPassword(key);
	}

	@Override
	public void deletePassphrase(String key) {
		Preconditions.checkState(gnomeLoginKeyring.isPresent());
		gnomeLoginKeyring.get().deletePassword(key);
	}
}
