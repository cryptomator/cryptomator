package org.cryptomator.ui.mainwindow;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.model.Vault;

import javax.inject.Inject;

// unscoped because each cell needs its own controller
public class VaultListCellController implements FxController {

	private final ObjectProperty<Vault> vault = new SimpleObjectProperty<>();
	private final StringBinding glyph;

	@Inject
	VaultListCellController() {
		this.glyph = Bindings.createStringBinding(this::getGlyphForVault, vault);
	}

	private String getGlyphForVault() {
		Vault v = vault.get();
		if (v == null) {
			return "WARNING";
		} else {
			switch (v.getState()) {
				case LOCKED:
					return "LOCK";
				case UNLOCKED:
					return "UNLOCK";
				case PROCESSING:
					return "SPINNER";
				default:
					return "WARNING";
			}
		}
	}

	/* Getter/Setter */

	public StringBinding glyphProperty() {
		return glyph;
	}

	public String getGlyph() {
		return glyph.get();
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
