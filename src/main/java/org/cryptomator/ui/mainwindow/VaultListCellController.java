package org.cryptomator.ui.mainwindow;

import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultState;
import org.cryptomator.ui.common.Animations;
import org.cryptomator.ui.common.AutoAnimator;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.controls.FontAwesome5Icon;
import org.cryptomator.ui.controls.FontAwesome5IconView;

import javax.inject.Inject;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.layout.HBox;
import java.util.ResourceBundle;

// unscoped because each cell needs its own controller
public class VaultListCellController implements FxController {

	private static final Insets COMPACT_INSETS = new Insets(6, 12, 6, 12);
	private static final Insets DEFAULT_INSETS = new Insets(12);

	private final ResourceBundle resourceBundle;
	private final ObjectProperty<Vault> vault = new SimpleObjectProperty<>();
	private final ObservableValue<VaultState.Value> vaultState;
	private final ObservableValue<FontAwesome5Icon> glyph;
	private final ObservableValue<Boolean> compactMode;
	private final ObservableValue<String> accessibleText;

	private AutoAnimator spinAnimation;

	/* FXML */
	public FontAwesome5IconView vaultStateView;
	@FXML
	public HBox vaultListCell;

	@Inject
	VaultListCellController(Settings settings, ResourceBundle resourceBundle) {
		this.resourceBundle = resourceBundle;
		this.vaultState = vault.flatMap(Vault::stateProperty);
		this.glyph = vaultState.map(this::getGlyphForVaultState);
		this.accessibleText = vaultState.map(this::getAccessibleTextForVaultState);
		this.compactMode = settings.compactMode;
	}

	public void initialize() {
		this.spinAnimation = AutoAnimator.animate(Animations.createDiscrete360Rotation(vaultStateView)) //
				.onCondition(vaultState.map(VaultState.Value.PROCESSING::equals).orElse(false)) //
				.afterStop(() -> vaultStateView.setRotate(0)) //
				.build();
		this.vaultListCell.paddingProperty().bind(compactMode.map(c -> c ? COMPACT_INSETS : DEFAULT_INSETS));
		this.vaultListCell.accessibleTextProperty().bind(accessibleText);
	}

	// TODO deduplicate w/ VaultDetailController
	private FontAwesome5Icon getGlyphForVaultState(VaultState.Value state) {
		if (state != null) {
			return switch (state) {
				case LOCKED -> FontAwesome5Icon.LOCK;
				case PROCESSING -> FontAwesome5Icon.SPINNER;
				case UNLOCKED -> FontAwesome5Icon.LOCK_OPEN;
				case NEEDS_MIGRATION, MISSING, VAULT_CONFIG_MISSING, ALL_MISSING, ERROR -> FontAwesome5Icon.EXCLAMATION_TRIANGLE;
			};
		} else {
			return FontAwesome5Icon.EXCLAMATION_TRIANGLE;
		}
	}

	private String getAccessibleTextForVaultState(VaultState.Value state) {
		var v = vault.get();
		if (state != null && v != null) {
			var translationKey = switch (state) {
				case LOCKED -> "vault.state.locked";
				case PROCESSING -> "vault.state.processing";
				case UNLOCKED -> "vault.state.unlocked";
				case NEEDS_MIGRATION -> "vault.state.migrationNeeded";
				case MISSING -> "vault.state.missing";
				case VAULT_CONFIG_MISSING, ALL_MISSING, ERROR -> "vault.state.error";
			};

			var localizedState = resourceBundle.getString(translationKey);
			return resourceBundle.getString("main.vaultlist.listEntry").formatted(v.getDisplayName(), localizedState);
		} else {
			return "";
		}
	}

	/* Getter/Setter */

	public ObservableValue<FontAwesome5Icon> glyphProperty() {
		return glyph;
	}

	public FontAwesome5Icon getGlyph() {
		return glyph.getValue();
	}

	public ObjectProperty<Vault> vaultProperty() {
		return vault;
	}

	public Vault getVault() {
		return vault.get();
	}

	public ObservableValue<Boolean> compactModeProperty() {
		return compactMode;
	}

	public boolean getCompactMode() {
		return compactMode.getValue();
	}

	public void setVault(Vault value) {
		vault.set(value);
	}

}
