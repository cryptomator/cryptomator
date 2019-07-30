package org.cryptomator.ui.mainwindow;

import javafx.beans.binding.Binding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.common.vaults.Vault;
import org.fxmisc.easybind.EasyBind;

import javax.inject.Inject;

// unscoped because each cell needs its own controller
public class VaultListCellController implements FxController {

	private final ObjectProperty<Vault> vault = new SimpleObjectProperty<>();
	private final Binding<String> glyph;

	@Inject
	VaultListCellController() {
		this.glyph = EasyBind.select(vault).selectObject(Vault::stateProperty).map(this::getGlyphForVaultState).orElse("WARNING");
	}

	private String getGlyphForVaultState(Vault.State state) {
		switch (state) {
			case LOCKED:
				return "LOCK";
			case PROCESSING:
				return "SPINNER";
			case UNLOCKED:
				return "UNLOCK";
			default:
				return "WARNING";
		}
	}

	/* Getter/Setter */

	public Binding<String> glyphProperty() {
		return glyph;
	}

	public String getGlyph() {
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
