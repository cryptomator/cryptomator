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

// unscoped because each cell needs its own controller
public class VaultListCellController implements FxController {

	private static final Insets COMPACT_INSETS = new Insets(6, 12, 6, 12);
	private static final Insets DEFAULT_INSETS = new Insets(12);

	private final ObjectProperty<Vault> vault = new SimpleObjectProperty<>();
	private final ObservableValue<FontAwesome5Icon> glyph;
	private final ObservableValue<Boolean> compactMode;

	private AutoAnimator spinAnimation;

	/* FXML */
	public FontAwesome5IconView vaultStateView;
	@FXML
	public HBox vaultListCell;

	@Inject
	VaultListCellController(Settings settings) {
		this.glyph = vault.flatMap(Vault::stateProperty).map(this::getGlyphForVaultState);
		this.compactMode = settings.compactMode;
	}

	public void initialize() {
		this.spinAnimation = AutoAnimator.animate(Animations.createDiscrete360Rotation(vaultStateView)) //
				.onCondition(vault.flatMap(Vault::stateProperty).map(VaultState.Value.PROCESSING::equals).orElse(false)) //
				.afterStop(() -> vaultStateView.setRotate(0)) //
				.build();
		this.vaultListCell.paddingProperty().bind(compactMode.map(c -> c ? COMPACT_INSETS : DEFAULT_INSETS));
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
