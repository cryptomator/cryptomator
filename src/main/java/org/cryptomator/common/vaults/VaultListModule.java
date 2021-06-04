package org.cryptomator.common.vaults;

import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

@Module
public class VaultListModule {

	@Provides
	@Singleton
	public ObservableList<Vault> provideVaultList() {
		return FXCollections.observableArrayList(Vault::observables);
	}

}
