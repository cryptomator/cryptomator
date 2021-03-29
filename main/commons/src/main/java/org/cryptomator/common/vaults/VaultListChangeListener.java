package org.cryptomator.common.vaults;

import org.cryptomator.common.settings.VaultSettings;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This listener makes sure to reflect any changes to the vault list back to the settings.
 */
class VaultListChangeListener implements ListChangeListener<Vault> {

	private final ObservableList<VaultSettings> vaultSettingsList;

	public VaultListChangeListener(ObservableList<VaultSettings> vaultSettingsList) {
		this.vaultSettingsList = vaultSettingsList;
	}

	@Override
	public void onChanged(Change<? extends Vault> c) {
		while (c.next()) {
			if (c.wasAdded()) {
				List<VaultSettings> addedSettings = c.getAddedSubList().stream().map(Vault::getVaultSettings).toList();
				vaultSettingsList.addAll(c.getFrom(), addedSettings);
			} else if (c.wasRemoved()) {
				List<VaultSettings> removedSettings = c.getRemoved().stream().map(Vault::getVaultSettings).toList();
				vaultSettingsList.removeAll(removedSettings);
			}
		}
	}
}
