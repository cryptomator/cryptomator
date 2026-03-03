package org.cryptomator.ui.mainwindow;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultState;
import org.cryptomator.integrations.revealpath.RevealFailedException;
import org.cryptomator.integrations.revealpath.RevealPathService;
import org.cryptomator.ui.common.Animations;
import org.cryptomator.ui.common.AutoAnimator;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.controls.FontAwesome5Icon;
import org.cryptomator.ui.controls.FontAwesome5IconView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;

@MainWindowScoped
public class VaultDetailController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(VaultDetailController.class);

	private final ReadOnlyObjectProperty<Vault> vault;
	private final ObservableValue<FontAwesome5Icon> glyph;
	private final BooleanBinding anyVaultSelected;
	private final RevealPathService revealPathService;

	private AutoAnimator spinAnimation;

	/* FXML */
	public FontAwesome5IconView vaultStateView;


	@Inject
	VaultDetailController(ObjectProperty<Vault> vault, RevealPathService revealPathService) {
		this.vault = vault;
		this.glyph = vault.flatMap(Vault::stateProperty).map(this::getGlyphForVaultState);
		this.anyVaultSelected = vault.isNotNull();
		this.revealPathService = revealPathService;
	}

	public void initialize() {
		this.spinAnimation = AutoAnimator.animate(Animations.createDiscrete360Rotation(vaultStateView)) //
				.onCondition(vault.flatMap(Vault::stateProperty).map(VaultState.Value.PROCESSING::equals).orElse(false)) //
				.afterStop(() -> vaultStateView.setRotate(0)) //
				.build();
	}

	// TODO deduplicate w/ VaultListCellController
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

	@FXML
	public void revealStorageLocation() {
		try {
			revealPathService.reveal(vault.get().getPath());
		} catch (RevealFailedException e) {
			LOG.warn("Failed to reveal vault storage location", e);
		}
	}

	/* Observable Properties */

	public ReadOnlyObjectProperty<Vault> vaultProperty() {
		return vault;
	}

	public Vault getVault() {
		return vault.get();
	}

	public ObservableValue<FontAwesome5Icon> glyphProperty() {
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
