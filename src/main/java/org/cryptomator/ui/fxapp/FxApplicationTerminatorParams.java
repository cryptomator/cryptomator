package org.cryptomator.ui.fxapp;


import org.cryptomator.common.ShutdownHook;
import org.cryptomator.common.vaults.Vault;


import javax.inject.Inject;

import javafx.collections.ObservableList;


public class FxApplicationTerminatorParams {
	private ObservableList<Vault> vaults;
	private ShutdownHook shutdownHook;
	private FxApplicationWindows appWindows;

	@Inject
	public FxApplicationTerminatorParams(ObservableList<Vault> vaults, ShutdownHook shutdownHook, FxApplicationWindows appWindows) {
		this.vaults = vaults;
		this.shutdownHook = shutdownHook;
		this.appWindows = appWindows;
	}

	public ObservableList<Vault> getVaults() {
		return vaults;
	}

	public ShutdownHook getShutdownHook() {
		return shutdownHook;
	}

	public FxApplicationWindows getAppWindows() {
		return appWindows;
	}

}

