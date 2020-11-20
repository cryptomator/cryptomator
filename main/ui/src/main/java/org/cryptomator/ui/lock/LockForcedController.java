package org.cryptomator.ui.lock;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.UserInteractionLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

@LockScoped
public class LockForcedController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(LockForcedController.class);

	private final Stage window;
	private final Vault vault;
	private final UserInteractionLock<LockModule.ForceLockDecision> forceLockDecisionLock;

	@Inject
	public LockForcedController(@LockWindow Stage window, @LockWindow Vault vault, UserInteractionLock<LockModule.ForceLockDecision> forceLockDecisionLock) {
		this.window = window;
		this.vault = vault;
		this.forceLockDecisionLock = forceLockDecisionLock;
		this.window.setOnHiding(this::windowClosed);
	}

	@FXML
	public void cancel() {
		forceLockDecisionLock.interacted(LockModule.ForceLockDecision.CANCEL);
		window.close();
	}

	@FXML
	public void confirmForcedLock() {
		forceLockDecisionLock.interacted(LockModule.ForceLockDecision.FORCE);
		window.close();
	}

	private void windowClosed(WindowEvent windowEvent) {
		// if not already interacted, set the decision to CANCEL
		if (forceLockDecisionLock.awaitingInteraction().get()) {
			LOG.debug("Lock canceled in force-lock-phase by user.");
			forceLockDecisionLock.interacted(LockModule.ForceLockDecision.CANCEL);
		}
	}

	// ----- Getter & Setter -----

	public String getVaultName() {
		return vault.getDisplayName();
	}

}
