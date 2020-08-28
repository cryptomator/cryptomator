package org.cryptomator.ui.vaultoptions;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import org.cryptomator.common.settings.UiTheme;
import org.cryptomator.common.settings.WhenUnlocked;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;
import java.util.ResourceBundle;

@VaultOptionsScoped
public class GeneralVaultOptionsController implements FxController {

	private final Vault vault;
	private final ResourceBundle resourceBundle;

	public TextField vaultName;
	public CheckBox unlockOnStartupCheckbox;
	public ChoiceBox<WhenUnlocked> actionAfterUnlockChoiceBox;

	@Inject
	GeneralVaultOptionsController(@VaultOptionsWindow Vault vault, ResourceBundle resourceBundle) {
		this.vault = vault;
		this.resourceBundle = resourceBundle;
	}

	@FXML
	public void initialize() {
		vaultName.textProperty().bindBidirectional(vault.getVaultSettings().displayName());
		unlockOnStartupCheckbox.selectedProperty().bindBidirectional(vault.getVaultSettings().unlockAfterStartup());
		actionAfterUnlockChoiceBox.getItems().addAll(WhenUnlocked.values());
		actionAfterUnlockChoiceBox.valueProperty().bindBidirectional(vault.getVaultSettings().actionAfterUnlock());
		actionAfterUnlockChoiceBox.setConverter(new WhenUnlockedConverter(resourceBundle));
	}

	private static class WhenUnlockedConverter extends StringConverter<WhenUnlocked> {
		
		private final ResourceBundle resourceBundle;

		public WhenUnlockedConverter(ResourceBundle resourceBundle) {
			this.resourceBundle = resourceBundle;
		}

		@Override
		public String toString(WhenUnlocked obj) {
			return resourceBundle.getString(obj.getDisplayName());
		}

		@Override
		public WhenUnlocked fromString(String string) {
			throw new UnsupportedOperationException();
		}
	}

}
