package org.cryptomator.ui.mainwindow;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.VaultService;
import org.cryptomator.ui.stats.VaultStatisticsComponent;

import javax.inject.Inject;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.fxml.FXML;

@MainWindowScoped
public class VaultDetailUnlockedController implements FxController {

	private final ReadOnlyObjectProperty<Vault> vault;
	private final VaultService vaultService;
	private final LoadingCache<Vault, VaultStatisticsComponent> vaultStatisticsWindows;
	private final VaultStatisticsComponent.Builder vaultStatisticsWindow;

	@Inject
	public VaultDetailUnlockedController(ObjectProperty<Vault> vault, VaultService vaultService, VaultStatisticsComponent.Builder vaultStatisticsWindow) {
		this.vault = vault;
		this.vaultService = vaultService;
		this.vaultStatisticsWindows = CacheBuilder.newBuilder().build(CacheLoader.from(this::provideVaultStatisticsComponent));
		//TODO make the binding a weak Binding via weakValues
		this.vaultStatisticsWindow = vaultStatisticsWindow;
	}

	private VaultStatisticsComponent provideVaultStatisticsComponent(Vault vault) {
		return vaultStatisticsWindow.vault(vault).build();
	}

	@FXML
	public void revealAccessLocation() {
		vaultService.reveal(vault.get());
	}

	@FXML
	public void lock() {
		vaultService.lock(vault.get(), false);
		// TODO count lock attempts, and allow forced lock
	}

	@FXML
	public void showVaultStatistics() {
		vaultStatisticsWindows.getUnchecked(vault.get()).showVaultStatisticsWindow();
	}

	/* Getter/Setter */

	public ReadOnlyObjectProperty<Vault> vaultProperty() {
		return vault;
	}

	public Vault getVault() {
		return vault.get();
	}

}
