package org.cryptomator.ui.mainwindow;

import javafx.beans.binding.Binding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.cryptomator.common.vaults.VaultState;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.controls.FontAwesome5Icon;
import org.fxmisc.easybind.EasyBind;

import javax.inject.Inject;

// unscoped because each cell needs its own controller
public class VaultListCellController implements FxController {

	private final ObjectProperty<Vault> vault = new SimpleObjectProperty<>();
	private final Binding<FontAwesome5Icon> glyph;

	@Inject
	VaultListCellController() {
		this.glyph = EasyBind.select(vault).selectObject(Vault::stateProperty).map(this::getGlyphForVaultState).orElse(FontAwesome5Icon.EXCLAMATION_TRIANGLE);
	}

	private FontAwesome5Icon getGlyphForVaultState(VaultState state) {
		switch (state) {
			case LOCKED:
				return FontAwesome5Icon.LOCK;
			case PROCESSING:
				return FontAwesome5Icon.SPINNER;
			case UNLOCKED:
				return FontAwesome5Icon.LOCK_OPEN;
			default:
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
