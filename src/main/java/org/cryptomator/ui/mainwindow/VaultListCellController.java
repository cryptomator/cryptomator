package org.cryptomator.ui.mainwindow;

import com.tobiasdiez.easybind.EasyBind;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultState;
import org.cryptomator.ui.common.Animations;
import org.cryptomator.ui.common.AutoAnimator;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.controls.FontAwesome5Icon;
import org.cryptomator.ui.controls.FontAwesome5IconView;

import javax.inject.Inject;
import javafx.beans.binding.Binding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

// unscoped because each cell needs its own controller
public class VaultListCellController implements FxController {

	private final ObjectProperty<Vault> vault = new SimpleObjectProperty<>();
	private final Binding<FontAwesome5Icon> glyph;

	private AutoAnimator spinAnimation;

	/* FXML */
	public FontAwesome5IconView vaultStateView;

	@Inject
	VaultListCellController() {
		this.glyph = EasyBind.select(vault) //
				.selectObject(Vault::stateProperty) //
				.map(this::getGlyphForVaultState);
	}

	public void initialize() {
		this.spinAnimation = AutoAnimator.animate(Animations.createDiscrete360Rotation(vaultStateView)) //
				.onCondition(EasyBind.select(vault).selectObject(Vault::stateProperty).map(VaultState.Value.PROCESSING::equals)) //
				.afterStop(() -> vaultStateView.setRotate(0)) //
				.build();
	}

	// TODO deduplicate w/ VaultDetailController
	private FontAwesome5Icon getGlyphForVaultState(VaultState.Value state) {
		if (state != null) {
			return switch (state) {
				case LOCKED -> FontAwesome5Icon.LOCK;
				case PROCESSING -> FontAwesome5Icon.SPINNER;
				case UNLOCKED -> FontAwesome5Icon.LOCK_OPEN;
				case NEEDS_MIGRATION, MISSING, ERROR -> FontAwesome5Icon.EXCLAMATION_TRIANGLE;
			};
		} else {
			return FontAwesome5Icon.EXCLAMATION_TRIANGLE;
		}
	}

	/* Getter/Setter */

	public Binding<FontAwesome5Icon> glyphProperty() {
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

	public void setVault(Vault value) {
		vault.set(value);
	}

}
