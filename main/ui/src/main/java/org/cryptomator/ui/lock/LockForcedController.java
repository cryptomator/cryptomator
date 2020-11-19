package org.cryptomator.ui.lock;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.UserInteractionLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.event.ActionEvent;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

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

	public void cancel() {
		forceLockDecisionLock.interacted(LockModule.ForceLockDecision.CANCEL);
		window.close();
	}

	public void confirmForcedLock(ActionEvent actionEvent) {
		forceLockDecisionLock.interacted(LockModule.ForceLockDecision.FORCE);
		window.close();
	}

	private void windowClosed(WindowEvent windowEvent) {
		// if not already interacted, mark this workflow as cancelled:
		if (forceLockDecisionLock.awaitingInteraction().get()) {
			LOG.debug("Lock canceled in force-lock-phase by user.");
			forceLockDecisionLock.interacted(LockModule.ForceLockDecision.CANCEL);
		}
	}

}
