package org.cryptomator.ui.fxapp;

import org.cryptomator.common.vaults.Vault;

import javax.inject.Inject;
import javafx.collections.ObservableList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

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
		vaults.stream().filter(Vault::isLocked) //
				.filter(v -> v.getVaultSettings().unlockAfterStartup().get()) //
				.<CompletionStage<Void>>reduce(CompletableFuture.completedFuture(null), //
						(unlockFlow, v) -> unlockFlow.handle((voit, ex) -> appWindows.startUnlockWorkflow(v, null)).thenCompose(stage -> stage), //we don't care here about the exception, logged elsewhere
						(unlockChain1, unlockChain2) -> unlockChain1.handle((voit, ex) -> unlockChain2).thenCompose(stage -> stage));
	}

}
