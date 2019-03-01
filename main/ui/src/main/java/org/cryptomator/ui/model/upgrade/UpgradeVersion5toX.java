/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.ui.model.upgrade;

import org.cryptomator.common.FxApplicationScoped;
import org.cryptomator.cryptofs.migration.Migrators;
import org.cryptomator.cryptofs.migration.api.NoApplicableMigratorException;
import org.cryptomator.cryptolib.Cryptors;
import org.cryptomator.cryptolib.api.Cryptor;
import org.cryptomator.cryptolib.api.InvalidPassphraseException;
import org.cryptomator.ui.l10n.Localization;
import org.cryptomator.ui.model.Vault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;

@FxApplicationScoped
class UpgradeVersion5toX extends UpgradeStrategy {

	private static final Logger LOG = LoggerFactory.getLogger(UpgradeVersion5toX.class);

	@Inject
	public UpgradeVersion5toX(Localization localization) {
		super(Cryptors.version1(UpgradeStrategy.strongSecureRandom()), localization, 5, Integer.MAX_VALUE);
	}

	@Override
	public String getTitle(Vault vault) {
		return localization.getString("upgrade.version5toX.title");
	}

	@Override
	public String getMessage(Vault vault) {
		return localization.getString("upgrade.version5toX.msg");
	}

	@Override
	public void upgrade(Vault vault, CharSequence passphrase) throws UpgradeFailedException {
		try {
			Migrators.get().migrate(vault.getPath(), "masterkey.cryptomator", passphrase);
		} catch (InvalidPassphraseException e) {
			throw new UpgradeFailedException(localization.getString("unlock.errorMessage.wrongPassword"));
		} catch (NoApplicableMigratorException | IOException e) {
			LOG.warn("Upgrade failed.", e);
			throw new UpgradeFailedException("Upgrade failed. Details in log message.");
		}
	}

	@Override
	protected void upgrade(Vault vault, Cryptor cryptor) throws UpgradeFailedException {
		// called by #upgrade(Vault, CharSequence), which got overwritten.
		throw new AssertionError("Method can not be called.");
	}

	@Override
	public boolean isApplicable(Vault vault) {
		try {
			return Migrators.get().needsMigration(vault.getPath(), "masterkey.cryptomator");
		} catch (IOException e) {
			LOG.warn("Could not determine, whether upgrade is applicable.", e);
			return false;
		}
	}

}
