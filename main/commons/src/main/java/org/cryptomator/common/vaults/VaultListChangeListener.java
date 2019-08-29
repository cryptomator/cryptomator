package org.cryptomator.common.vaults;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.cryptomator.common.settings.VaultSettings;

/**
 * This listener makes sure to reflect any changes to the vault list back to the settings.
 */
public class VaultListChangeListener implements ListChangeListener<Vault> {

	private final ObservableList<VaultSettings> vaultSettingsList;

	public VaultListChangeListener(ObservableList<VaultSettings> vaultSettingsList) {
		this.vaultSettingsList = vaultSettingsList;
	}

	@Override
	public void onChanged(Change<? extends Vault> c) {
		while(c.next()) {
			if (c.wasAdded()) {
				for (int i = c.getFrom(); i < c.getTo(); i++) {
					Vault v = c.getList().get(i);
					vaultSettingsList.add(i, v.getVaultSettings());
				}
			} else if (c.wasRemoved()) {
				for (Vault v : c.getRemoved()) {
					vaultSettingsList.remove(v.getVaultSettings());
				}
			}
		}
	}
}
