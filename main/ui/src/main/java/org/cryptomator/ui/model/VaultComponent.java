package org.cryptomator.ui.model;

import org.cryptomator.ui.model.VaultModule.PerVault;

import dagger.Subcomponent;

@PerVault
@Subcomponent(modules = {VaultModule.class})
public interface VaultComponent {

	Vault vault();

}
