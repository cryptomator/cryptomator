package org.cryptomator.ui.unlock;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.ContentDisplay;
import javafx.stage.Stage;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptolib.api.InvalidPassphraseException;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.Tasks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.ExecutorService;

@UnlockScoped
public class UnlockSuccessController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(UnlockSuccessController.class);

	private final Stage window;
	private final Vault vault;
	private final ExecutorService executor;
	private final ObjectProperty<ContentDisplay> revealButtonState;
	private final BooleanProperty revealButtonDisabled;

	@Inject
	public UnlockSuccessController(@UnlockWindow Stage window, @UnlockWindow Vault vault, ExecutorService executor) {
		this.window = window;
		this.vault = vault;
		this.executor = executor;
		this.revealButtonState = new SimpleObjectProperty<>(ContentDisplay.TEXT_ONLY);
		this.revealButtonDisabled = new SimpleBooleanProperty();
	}


	public void close() {
		LOG.trace("UnlockSuccessController.close()");
		window.close();
	}

	public void revealAndClose() {
		LOG.trace("UnlockSuccessController.revealAndClose()");
		revealButtonState.set(ContentDisplay.LEFT);
		revealButtonDisabled.set(true);
		Tasks.create(() -> {
			vault.reveal();
		}).onSuccess(() -> {
			window.close();
		}).onError(InvalidPassphraseException.class, e -> {
			// TODO
			LOG.warn("Reveal failed.", e);
		}).andFinally(() -> {
			revealButtonState.set(ContentDisplay.TEXT_ONLY);
			revealButtonDisabled.set(false);
		}).runOnce(executor);
	}

	/* Getter/Setter */

	public Vault getVault() {
		return vault;
	}

	public ReadOnlyObjectProperty<ContentDisplay> revealButtonStateProperty() {
		return revealButtonState;
	}

	public ContentDisplay getRevealButtonState() {
		return revealButtonState.get();
	}

	public BooleanProperty revealButtonDisabledProperty() {
		return revealButtonDisabled;
	}

	public boolean isRevealButtonDisabled() {
		return revealButtonDisabled.get();
	}

}
