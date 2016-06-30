package org.cryptomator.ui.model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.cryptomator.crypto.engine.Cryptor;
import org.cryptomator.filesystem.crypto.Constants;
import org.cryptomator.ui.settings.Localization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
class UpgradeVersion3to4 extends UpgradeStrategy {

	private static final Logger LOG = LoggerFactory.getLogger(UpgradeVersion3to4.class);

	@Inject
	public UpgradeVersion3to4(Provider<Cryptor> cryptorProvider, Localization localization) {
		super(cryptorProvider, localization);
	}

	@Override
	public String getNotification(Vault vault) {
		return localization.getString("upgrade.version3to4.msg");
	}

	@Override
	protected void upgrade(Vault vault, Cryptor cryptor) throws UpgradeFailedException {
		throw new UpgradeFailedException("not yet implemented");
	}

	@Override
	public boolean isApplicable(Vault vault) {
		final Path masterkeyFile = vault.path().getValue().resolve(Constants.MASTERKEY_FILENAME);
		try {
			if (Files.isRegularFile(masterkeyFile)) {
				final String keyContents = new String(Files.readAllBytes(masterkeyFile), StandardCharsets.UTF_8);
				return keyContents.contains("\"version\":3") || keyContents.contains("\"version\": 3");
			} else {
				LOG.warn("Not a file: {}", masterkeyFile);
				return false;
			}
		} catch (IOException e) {
			LOG.warn("Could not determine, whether upgrade is applicable.", e);
			return false;
		}
	}

}
