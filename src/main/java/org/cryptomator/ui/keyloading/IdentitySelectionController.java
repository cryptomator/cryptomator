package org.cryptomator.ui.keyloading;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultIdentity;
import org.cryptomator.common.vaults.VaultIdentityManager;
import org.cryptomator.ui.common.FxController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Controller for identity selection during vault unlock.
 */
@IdentitySelectionScoped
public class IdentitySelectionController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(IdentitySelectionController.class);

	private final Stage window;
	private final Vault vault;
	private final CompletableFuture<VaultIdentity> result;

	@FXML
	private ComboBox<VaultIdentity> identityComboBox;

	@FXML
	private Label titleLabel;

	@Inject
	public IdentitySelectionController(@KeyLoading Stage window, @KeyLoading Vault vault, CompletableFuture<VaultIdentity> result) {
		this.window = window;
		this.vault = vault;
		this.result = result;
		this.window.addEventHandler(WindowEvent.WINDOW_HIDING, this::windowClosed);
	}

	@FXML
	public void initialize() {
		try {
			VaultIdentityManager manager = vault.getIdentityProvider().getManager();
			List<VaultIdentity> identities = manager.getIdentities();
			
			identityComboBox.getItems().addAll(identities);
			
			// Set converter to display identity names
			identityComboBox.setConverter(new javafx.util.StringConverter<VaultIdentity>() {
				@Override
				public String toString(VaultIdentity identity) {
					if (identity == null) {
						return null;
					}
					return identity.getName() + (identity.isPrimary() ? " (Primary)" : "");
				}

				@Override
				public VaultIdentity fromString(String string) {
					return null; // Not needed for display-only
				}
			});
			
			// Select primary identity by default
			manager.getPrimaryIdentity().ifPresent(identityComboBox::setValue);
			
			titleLabel.setText("Select Identity for " + vault.getDisplayName());
		} catch (IOException e) {
			LOG.error("Failed to load vault identities", e);
		}
	}

	@FXML
	public void onContinue() {
		VaultIdentity selected = identityComboBox.getValue();
		if (selected != null) {
			result.complete(selected);
			window.close();
		}
	}

	@FXML
	public void onCancel() {
		result.cancel(true);
		window.close();
	}

	private void windowClosed(WindowEvent windowEvent) {
		if (!result.isDone()) {
			result.cancel(true);
		}
	}
}
