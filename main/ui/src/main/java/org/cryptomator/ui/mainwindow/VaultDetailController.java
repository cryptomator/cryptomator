package org.cryptomator.ui.mainwindow;

import javafx.beans.binding.Binding;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.fxml.FXML;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultState;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.controls.FontAwesome5Icon;
import org.cryptomator.ui.fxapp.FxApplication;
import org.fxmisc.easybind.EasyBind;

import javax.inject.Inject;

@MainWindowScoped
public class VaultDetailController implements FxController {

	private final ReadOnlyObjectProperty<Vault> vault;
	private final FxApplication application;
	private final Binding<FontAwesome5Icon> glyph;
	private final BooleanBinding anyVaultSelected;

	@Inject
	VaultDetailController(ObjectProperty<Vault> vault, FxApplication application) {
		this.vault = vault;
		this.application = application;
		this.glyph = EasyBind.select(vault).selectObject(Vault::stateProperty).map(this::getGlyphForVaultState).orElse(FontAwesome5Icon.EXCLAMATION_TRIANGLE);
		this.anyVaultSelected = vault.isNotNull();
	}

	private FontAwesome5Icon getGlyphForVaultState(VaultState state) {
		return switch (state) {
			case LOCKED -> FontAwesome5Icon.LOCK;
			case PROCESSING -> FontAwesome5Icon.SPINNER;
			case UNLOCKED -> FontAwesome5Icon.LOCK_OPEN;
			case NEEDS_MIGRATION, MISSING, ERROR -> FontAwesome5Icon.EXCLAMATION_TRIANGLE;
		};
	}

	@FXML
	public void revealStorageLocation() {
		application.getHostServices().showDocument(vault.get().getPath().toUri().toString());
	}

	/* Observable Properties */

	public ReadOnlyObjectProperty<Vault> vaultProperty() {
		return vault;
	}

	public Vault getVault() {
		return vault.get();
	}

	public Binding<FontAwesome5Icon> glyphProperty() {
		return glyph;
	}

	public FontAwesome5Icon getGlyph() {
		return glyph.getValue();
	}

	public BooleanBinding anyVaultSelectedProperty() {
		return anyVaultSelected;
	}

	public boolean isAnyVaultSelected() {
		return anyVaultSelected.get();
	}
}
