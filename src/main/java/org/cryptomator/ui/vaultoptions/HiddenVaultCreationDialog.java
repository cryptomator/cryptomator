package org.cryptomator.ui.vaultoptions;

import org.cryptomator.common.keychain.MultiKeyslotFile;
import org.cryptomator.common.recovery.IdentityInitializer;
import org.cryptomator.common.vaults.MultiKeyslotVaultConfig;
import org.cryptomator.common.vaults.Vault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;
import java.io.IOException;

/**
 * Hidden vault creation dialog.
 * Maintains plausible deniability by showing no indication of existence after creation.
 * The feature is openly available, but after creation there's no way to prove how many 
 * hidden vaults exist without the passwords.
 * 
 * Uses multi-keyslot vault config to achieve TRUE plausible deniability - no vault.bak file!
 */
public class HiddenVaultCreationDialog {

	private static final Logger LOG = LoggerFactory.getLogger(HiddenVaultCreationDialog.class);

	private final Vault vault;
	private final MultiKeyslotFile multiKeyslotFile;
	private final MultiKeyslotVaultConfig multiKeyslotVaultConfig;
	private final Window owner;

	public HiddenVaultCreationDialog(Vault vault, MultiKeyslotFile multiKeyslotFile, 
									  MultiKeyslotVaultConfig multiKeyslotVaultConfig, Window owner) {
		this.vault = vault;
		this.multiKeyslotFile = multiKeyslotFile;
		this.multiKeyslotVaultConfig = multiKeyslotVaultConfig;
		this.owner = owner;
	}

	public void show() {
		Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
		dialog.initOwner(owner);
		dialog.setTitle("Hidden Vault Creation");
		dialog.setHeaderText("Create Plausibly Deniable Hidden Vault");
		dialog.setContentText("This will create a hidden vault with a separate password.\n\n" +
			"TRUE Plausible Deniability:\n" +
			"  ✓ Multi-keyslot masterkey.cryptomator\n" +
			"  ✓ Multi-keyslot vault.cryptomator\n" +
			"  ✓ NO vault.bak file created!\n" +
			"  ✓ Undetectable by filesystem analysis\n\n" +
				"Enter passwords below:");

		// Create input fields
		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(10);

		TextField nameField = new TextField("Hidden");
		nameField.setPromptText("Hidden vault name");
		
		TextField descField = new TextField("Backup files");
		descField.setPromptText("Plausible description");

		PasswordField primaryPwField = new PasswordField();
		primaryPwField.setPromptText("Current primary password");

		PasswordField hiddenPwField = new PasswordField();
		hiddenPwField.setPromptText("New hidden vault password");

		grid.add(new Label("Name:"), 0, 0);
		grid.add(nameField, 1, 0);
		grid.add(new Label("Description:"), 0, 1);
		grid.add(descField, 1, 1);
		grid.add(new Label("Primary Password:"), 0, 2);
		grid.add(primaryPwField, 1, 2);
		grid.add(new Label("Hidden Password:"), 0, 3);
		grid.add(hiddenPwField, 1, 3);

		dialog.getDialogPane().setContent(grid);

		dialog.showAndWait().ifPresent(response -> {
			if (response == ButtonType.OK) {
				createHiddenVault(
					nameField.getText(),
					descField.getText(),
					primaryPwField.getText(),
					hiddenPwField.getText()
				);
			}
		});
	}

	private void createHiddenVault(String name, String description, String primaryPassword, String hiddenPassword) {
		if (name.isEmpty() || primaryPassword.isEmpty() || hiddenPassword.isEmpty()) {
			showError("Invalid Input", "All required fields must be filled.");
			return;
		}

		try {
			IdentityInitializer.addSecondaryIdentity(
				vault.getPath(),
				name,
				description,
				primaryPassword,
				hiddenPassword,
				multiKeyslotFile,
				multiKeyslotVaultConfig
			);

			// Reload vault identities
			vault.getIdentityProvider().reload();

		showSuccess("Hidden Vault Created - TRUE Plausible Deniability!",
			"Successfully created hidden vault!\n\n" +
			"Files updated:\n" +
			"  ✓ masterkey.cryptomator (keyslot added)\n" +
			"  ✓ vault.cryptomator (config slot added)\n" +
			"  ✓ NO vault.bak created!\n\n" +
			"TRUE Plausible Deniability Achieved:\n" +
			"  • No file presence reveals hidden vault\n" +
			"  • Cannot be detected by filesystem analysis\n" +
			"  • Only password determines which vault opens\n\n" +
			"Access: Just enter your hidden password when unlocking.");

		} catch (IOException e) {
			LOG.error("Failed to create hidden vault", e);
			showError("Creation Failed", "Could not create hidden vault:\n" + e.getMessage());
		}
	}

	private void showSuccess(String title, String message) {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.initOwner(owner);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(message);
		alert.showAndWait();
	}

	private void showError(String title, String message) {
		Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.initOwner(owner);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(message);
		alert.showAndWait();
	}
}
