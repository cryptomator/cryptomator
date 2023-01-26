package org.cryptomator.ui.lock;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

@LockScoped
public class LockForcedController implements FxController {

	private final Stage window;
	private final Vault vault;
	private final AtomicReference<CompletableFuture<Boolean>> forceRetryDecision;

	@Inject
	public LockForcedController(@LockWindow Stage window, @LockWindow Vault vault, AtomicReference<CompletableFuture<Boolean>> forceRetryDecision) {
		this.window = window;
		this.vault = vault;
		this.forceRetryDecision = forceRetryDecision;
		this.window.setOnHiding(this::windowClosed);
	}

	@FXML
	public void cancel() {
		window.close();
	}

	@FXML
	public void retry() {
		forceRetryDecision.get().complete(false);
		window.close();
	}

	@FXML
	public void force() {
		forceRetryDecision.get().complete(true);
		window.close();
	}

	private void windowClosed(WindowEvent windowEvent) {
		forceRetryDecision.get().cancel(true);
	}

	// ----- Getter & Setter -----

	public String getVaultName() {
		return vault.getDisplayName();
	}

	public boolean isForceSupported() {
		return vault.supportsForcedUnmount();
	}

}
