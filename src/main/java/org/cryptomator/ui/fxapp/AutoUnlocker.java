package org.cryptomator.ui.fxapp;

import org.cryptomator.common.vaults.Vault;

import javax.inject.Inject;
import javafx.collections.ObservableList;

@FxApplicationScoped
public class AutoUnlocker {

	private final ObservableList<Vault> vaults;
	private final FxApplicationWindows appWindows;

	@Inject
	public AutoUnlocker(ObservableList<Vault> vaults, FxApplicationWindows appWindows) {
		this.vaults = vaults;
		this.appWindows = appWindows;
	}

	public void unlock() {
		vaults.stream().filter(Vault::isLocked).filter(v -> v.getVaultSettings().unlockAfterStartup().get()).forEach(v -> {
			appWindows.startUnlockWorkflow(v, null);
		});
	}

}
