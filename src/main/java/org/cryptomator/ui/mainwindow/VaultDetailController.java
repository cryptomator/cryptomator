package org.cryptomator.ui.mainwindow;

import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.Subscription;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultState;
import org.cryptomator.ui.common.Animations;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.controls.FontAwesome5Icon;
import org.cryptomator.ui.controls.FontAwesome5IconView;
import org.cryptomator.ui.fxapp.FxApplication;

import javax.inject.Inject;
import javafx.beans.binding.Binding;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.fxml.FXML;

@MainWindowScoped
public class VaultDetailController implements FxController {

	private final ReadOnlyObjectProperty<Vault> vault;
	private final FxApplication application;
	private final Binding<FontAwesome5Icon> glyph;
	private final BooleanBinding anyVaultSelected;

	private Subscription rotationSubscriptions;

	/* FXML */
	public FontAwesome5IconView vaultStateView;


	@Inject
	VaultDetailController(ObjectProperty<Vault> vault, FxApplication application) {
		this.vault = vault;
		this.application = application;
		this.glyph = EasyBind.select(vault) //
				.selectObject(Vault::stateProperty) //
				.map(this::getGlyphForVaultState);
		this.anyVaultSelected = vault.isNotNull();
	}

	public void initialize() {
		rotationSubscriptions = Animations.spinOnCondition(vaultStateView,EasyBind.select(vault).selectObject(Vault::stateProperty),VaultState.Value.PROCESSING::equals);
	}

	// TODO deduplicate w/ VaultListCellController
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
