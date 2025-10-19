package org.cryptomator.ui.vaultoptions;

import org.cryptomator.common.keychain.MultiKeyslotFile;
import org.cryptomator.common.recovery.IdentityInitializer;
import org.cryptomator.common.vaults.MultiKeyslotVaultConfig;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultIdentity;
import org.cryptomator.common.vaults.VaultIdentityManager;
import org.cryptomator.cryptolib.api.Masterkey;
import org.cryptomator.cryptolib.common.MasterkeyFileAccess;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.PasswordDialogHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Optional;

/**
 * Controller for managing vault identities (plausibly deniable encryption).
 */
@VaultOptionsScoped
public class IdentityManagementController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(IdentityManagementController.class);

	private final Vault vault;
	private final Stage window;
	private final MasterkeyFileAccess masterkeyFileAccess;
	private final MultiKeyslotFile multiKeyslotFile;
	private final MultiKeyslotVaultConfig multiKeyslotVaultConfig;
	private final ObservableList<VaultIdentity> identities;

	@FXML
	private TableView<VaultIdentity> identitiesTable;

	@FXML
	private TableColumn<VaultIdentity, String> nameColumn;

	@FXML
	private TableColumn<VaultIdentity, String> typeColumn;

	@FXML
	private TableColumn<VaultIdentity, String> descriptionColumn;

	@FXML
	private Button addButton;

	@FXML
	private Button removeButton;

	@Inject
	public IdentityManagementController(@VaultOptionsWindow Vault vault, 
										 @VaultOptionsWindow Stage window,
										 MasterkeyFileAccess masterkeyFileAccess,
										 MultiKeyslotFile multiKeyslotFile,
										 MultiKeyslotVaultConfig multiKeyslotVaultConfig) {
		this.vault = vault;
		this.window = window;
		this.masterkeyFileAccess = masterkeyFileAccess;
		this.multiKeyslotFile = multiKeyslotFile;
		this.multiKeyslotVaultConfig = multiKeyslotVaultConfig;
		this.identities = FXCollections.observableArrayList();
	}

	@FXML
	public void initialize() {
		// Set up table columns
		nameColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getName()));
		typeColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().isPrimary() ? "Primary" : "Secondary"));
		descriptionColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getDescription()));

		// Bind table to identities list
		identitiesTable.setItems(identities);

		// Load identities
		loadIdentities();

		// Set up button states
		identitiesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
			removeButton.setDisable(newVal == null || newVal.isPrimary());
		});
	}

	private void loadIdentities() {
		try {
			VaultIdentityManager manager = vault.getIdentityProvider().getManager();
			identities.setAll(manager.getIdentities());
		} catch (IOException e) {
			LOG.error("Failed to load vault identities", e);
			showError("Failed to load identities", e.getMessage());
		}
	}

	@FXML
	public void onAddIdentity() {
		// Show dialog to get identity details
		TextInputDialog nameDialog = new TextInputDialog();
		nameDialog.setTitle("Add Identity");
		nameDialog.setHeaderText("Create a new vault identity");
		nameDialog.setContentText("Identity name:");
		
		Optional<String> nameResult = nameDialog.showAndWait();
		if (nameResult.isEmpty()) {
			return;
		}
		String name = nameResult.get();

		TextInputDialog descDialog = new TextInputDialog();
		descDialog.setTitle("Add Identity");
		descDialog.setHeaderText("Create a new vault identity");
		descDialog.setContentText("Description (optional):");
		String description = descDialog.showAndWait().orElse("");

		// Get primary password first (needed to read the masterkey)
		Optional<String> primaryPasswordResult = PasswordDialogHelper.promptPassword(
			window,
			"Add Identity",
			"Enter primary vault password",
			"Primary password:"
		);
		if (primaryPasswordResult.isEmpty()) {
			return;
		}
		String primaryPassword = primaryPasswordResult.get();

		// Show dialog to get new identity password
		Optional<String> newPasswordResult = PasswordDialogHelper.promptPassword(
			window,
			"Add Identity",
			"Set password for new identity",
			"New identity password:"
		);
		if (newPasswordResult.isEmpty()) {
			return;
		}
		String newPassword = newPasswordResult.get();

		// Create new identity
		try {
			// Add identity - will use the same masterkey encrypted with different password
			VaultIdentity identity = IdentityInitializer.addSecondaryIdentity(
				vault.getPath(), name, description, primaryPassword, newPassword, multiKeyslotFile, multiKeyslotVaultConfig
			);

			// Reload identities
			vault.getIdentityProvider().reload();
			loadIdentities();

			showInfo("Identity Added", "Successfully created identity '" + name + "'.\n\n" +
					"This identity uses the same vault but with a different password.");
		} catch (Exception e) {
			LOG.error("Failed to add identity", e);
			showError("Failed to add identity", e.getMessage());
		}
	}

	@FXML
	public void onRemoveIdentity() {
		VaultIdentity selected = identitiesTable.getSelectionModel().getSelectedItem();
		if (selected == null || selected.isPrimary()) {
			return;
		}

		// Ask for password to identify the keyslot to remove
		Optional<String> passwordResult = PasswordDialogHelper.promptPassword(
			window,
			"Remove Identity",
			"Enter password for identity: " + selected.getName(),
			"Password:"
		);
		if (passwordResult.isEmpty()) {
			return;
		}
		
		// Confirm deletion
		Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
		confirm.setTitle("Remove Identity");
		confirm.setHeaderText("Are you sure you want to remove this identity?");
		confirm.setContentText("Identity: " + selected.getName() + "\n\nThis action cannot be undone.");
		
		Optional<ButtonType> result = confirm.showAndWait();
		if (result.isEmpty() || result.get() != ButtonType.OK) {
			return;
		}

		// Remove identity (requires password to identify keyslot)
		try {
			boolean removed = IdentityInitializer.removeIdentity(vault.getPath(), passwordResult.get(), multiKeyslotFile);
			if (removed) {
				vault.getIdentityProvider().reload();
				loadIdentities();
				showInfo("Identity Removed", "Successfully removed identity '" + selected.getName() + "'");
			} else {
				showError("Failed to remove identity", "Password doesn't match any keyslot");
			}
		} catch (IOException e) {
			LOG.error("Failed to remove identity", e);
			showError("Failed to remove identity", e.getMessage());
		}
	}

	@FXML
	public void onClose() {
		window.close();
	}

	private void showInfo(String title, String message) {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(message);
		alert.showAndWait();
	}

	private void showError(String title, String message) {
		Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(message);
		alert.showAndWait();
	}
}
